#!/usr/bin/env python3
"""Fetch a bounded NYC DOT traffic-speed snapshot for the MN01 map (US-34, D-021, D-050).

Usage: python3 tools/fetch-traffic-snapshot.py OUTPUT.json [--window-minutes 120]

Source: NYC Open Data "DOT Traffic Speeds" (Socrata i4gi-tjb9), no API key needed. The script
asks for the newest data_as_of of the feed, downloads the rows of the preceding window (default
120 minutes), takes the newest data_as_of among those rows as the feed time, keeps the newest row
per link_id and only the links with at least one vertex inside the bounding box of
the MN01 road extract (docs/geospatial-data.md section 3.1). Row objects are written unmodified;
the selection is recorded in the file's metadata. Display only: these values never enter routing
or optimization costs.
"""
import argparse
import datetime as dt
import hashlib
import json
import sys
import urllib.parse
import urllib.request

ENDPOINT = "https://data.cityofnewyork.us/resource/i4gi-tjb9.json"
BBOX = {"south": 40.6950, "west": -74.0250, "north": 40.7350, "east": -73.9850}
FIELDS = ["id", "speed", "travel_time", "status", "data_as_of", "link_id", "link_points",
          "encoded_poly_line", "encoded_poly_line_lvls", "owner", "transcom_id", "borough", "link_name"]


def get(params):
    url = ENDPOINT + "?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=120) as r:
        return json.loads(r.read().decode("utf-8")), url


def decode_polyline(text):
    """Google encoded polyline; the feed doubles backslashes, so they are unescaped first."""
    s = text.replace("\\\\", "\\")
    i, lat, lng, out = 0, 0, 0, []
    while i < len(s):
        vals = []
        for _ in range(2):
            shift = result = 0
            while True:
                if i >= len(s):
                    raise ValueError("truncated polyline")
                b = ord(s[i]) - 63
                i += 1
                if b < 0 or b > 63:
                    raise ValueError("invalid polyline character")
                result |= (b & 0x1F) << shift
                shift += 5
                if b < 0x20:
                    break
            vals.append(~(result >> 1) if result & 1 else result >> 1)
        lat += vals[0]
        lng += vals[1]
        out.append((lat / 1e5, lng / 1e5))
    return out


def link_points(text):
    pts = []
    for token in (text or "").split():
        parts = token.split(",")
        if len(parts) != 2:
            continue
        try:
            pts.append((float(parts[0]), float(parts[1])))
        except ValueError:
            continue
    return pts


def touches_bbox(row):
    candidates = []
    try:
        candidates.extend(decode_polyline(row.get("encoded_poly_line") or ""))
    except ValueError:
        pass
    candidates.extend(link_points(row.get("link_points")))
    return any(BBOX["south"] <= la <= BBOX["north"] and BBOX["west"] <= lo <= BBOX["east"]
               for la, lo in candidates)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("output")
    ap.add_argument("--window-minutes", type=int, default=120)
    a = ap.parse_args()

    reported = get({"$select": "max(data_as_of) as latest"})[0][0]["latest"]
    since = (dt.datetime.fromisoformat(reported) - dt.timedelta(minutes=a.window_minutes))
    params = {"$where": "data_as_of >= '%s'" % since.strftime("%Y-%m-%dT%H:%M:%S"),
              "$order": "data_as_of DESC", "$limit": "50000"}
    rows, url = get(params)
    if not rows:
        raise SystemExit("no traffic rows in the window; nothing written")
    latest = max(r.get("data_as_of", "") for r in rows)
    retrieved = dt.datetime.now(dt.timezone.utc).replace(microsecond=0)

    newest = {}
    for r in rows:
        key = r.get("link_id")
        if key and (key not in newest or r.get("data_as_of", "") > newest[key].get("data_as_of", "")):
            newest[key] = r
    kept = [{k: r[k] for k in FIELDS if k in r} for r in newest.values() if touches_bbox(r)]
    kept.sort(key=lambda r: r["link_id"])

    doc = {
        "source": "NYC DOT Traffic Speeds, NYC Open Data (Socrata i4gi-tjb9)",
        "sourceUrl": "https://data.cityofnewyork.us/Transportation/DOT-Traffic-Speeds/i4gi-tjb9",
        "query": url,
        "retrievedAt": retrieved.isoformat().replace("+00:00", "Z"),
        "feedLatestDataAsOf": latest,
        "timeZone": "America/New_York",
        "windowMinutes": a.window_minutes,
        "rowsInWindow": len(rows),
        "linksInWindow": len(newest),
        "boundingBox": BBOX,
        "selection": "newest row per link_id in the window; links with at least one vertex "
                     "(decoded encoded_poly_line or link_points) inside the bounding box; "
                     "row fields unmodified",
        "records": kept,
    }
    data = (json.dumps(doc, indent=1, ensure_ascii=False) + "\n").encode("utf-8")
    with open(a.output, "wb") as f:
        f.write(data)
    print("records %d of %d links (%d rows); feed latest %s; retrieved %s; sha256 %s"
          % (len(kept), len(newest), len(rows), latest, doc["retrievedAt"],
             hashlib.sha256(data).hexdigest()))


if __name__ == "__main__":
    sys.exit(main())
