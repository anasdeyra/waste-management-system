# New York City geospatial data and map scope

## 1. Selected case study

The prototype uses **New York City**, with **Lower Manhattan (DSNY district MN01)** as its
initial pilot area. This area was selected because the necessary waste-location, depot,
street, and traffic datasets are available from official city sources and can be joined by
real geographic coordinates.

A source check on **2026-10-31** found:

- **449** basket records in MN01, all with latitude and longitude;
- **238** of those records with a `DSNY ...` basket type;
- the official **MN01G** garage record at **353 Spring St**, with a GeoJSON point; and
- current NYC DOT traffic records with road-link geometry, speed, travel time, status, and
  observation timestamps.

These counts describe the checked source snapshot, not a permanent citywide total. The
project does not claim that its generated tours are official DSNY routes.

## 2. Authoritative sources

| Data | Official source and identifier | Project use | Important limitation |
|---|---|---|---|
| Collection-point locations | [DSNY Litter Basket Inventory](https://data.cityofnewyork.us/Environment/DSNY-Litter-Basket-Inventory/8znf-7b2c), Socrata ID `8znf-7b2c` | Real basket IDs, ownership/physical type, street names, WGS84 coordinates, borough, and district | Locations are approximate and surveyed periodically. The dataset also contains BID-owned, private, adopted, and other baskets. |
| Depot | [DSNY Garages](https://data.cityofnewyork.us/Environment/DSNY-Garages-Map-/2kdb-mfqb), source dataset `xw3j-2yxf` | Real MN01G depot coordinates and address | A garage can serve multiple districts; `name=MN01G` selects the depot, not the garage record's `districtcode`. |
| Traffic overlay | [DOT Traffic Speeds](https://data.cityofnewyork.us/Transportation/DOT-Traffic-Speeds/i4gi-tjb9), Socrata ID `i4gi-tjb9`; [DOT feed description](https://www.nyc.gov/html/dot/html/about/datafeeds.shtml#realtime) | Timestamped traffic-link polylines and speed/travel-time attributes on the map | Sensors cover mostly major arterials and highways, not every local street. Invalid/stale records must not be presented as current conditions. |
| Street reference | [NYC Street Centerline](https://data.cityofnewyork.us/City-Government/Centerline/inkn-q76z), Socrata ID `inkn-q76z` | Street names, geometry, traffic direction, and validation/reference display | It is a reference dataset, not by itself the prototype's complete routing engine. |
| Routable road network and basemap | [OpenStreetMap](https://www.openstreetmap.org/) | Versioned OSM XML road extract for GraphHopper routing (§3.1); the basemap uses OSM vector tiles from OpenFreeMap with our own minimal style (D-054) | OSM data and tile infrastructure are separate: data use requires ODbL attribution, and public tile usage must follow the provider's policy. |

## 3. Included snapshots

Two unmodified API extracts are stored with the backend resources so the demonstration data
can be reproduced without depending on a live request:

- `backend/src/main/resources/data/nyc-mn01-baskets.csv` — all 449 MN01 basket rows;
  SHA-256 `a0737237e370d73152ee0eaec064385232c4c3027d7939d5a9033abf1b5a3013`;
- `backend/src/main/resources/data/nyc-mn01-depot.csv` — the single MN01G garage row;
  SHA-256 `207851c2e229df0827a5132d2f3c6154934540daa1134b3a430e5b0a71c90832`.

Only the 238 rows whose `baskettype` starts with `DSNY ` are eligible for the initial
optimization scenarios. Other ownership types may be displayed with their source label, but
must not silently become DSNY service obligations. Waste demand, vehicle details, agent
assignments, and collection results are **synthetic demonstration values**, clearly separated
from the real location data.

The extracts can be refreshed with:

```bash
curl -fSsL -G 'https://data.cityofnewyork.us/resource/8znf-7b2c.csv' \
  --data-urlencode '$select=basketid,baskettype,streetname1,streetname2,latitude,longitude,borough,district' \
  --data-urlencode '$where=district="MN01"' \
  --data-urlencode '$limit=5000' \
  -o backend/src/main/resources/data/nyc-mn01-baskets.csv

curl -fSsL -G 'https://data.cityofnewyork.us/resource/xw3j-2yxf.csv' \
  --data-urlencode 'name=MN01G' \
  --data-urlencode '$limit=1' \
  -o backend/src/main/resources/data/nyc-mn01-depot.csv
```

Refreshing changes the input snapshot. Evaluation records must therefore retain source query,
retrieval date, record count, and a checksum or equivalent snapshot identifier.

### 3.1 Road-network snapshot (OSM, D-037)

- File: `backend/src/main/resources/routing/mn01-roads.osm` (OSM XML 0.6, 4 148 737 bytes).
- Retrieved **2026-11-01** from `https://overpass-api.de/api/interpreter`; Overpass `osm_base`
  **2026-11-01T13:53:59Z**.
- Content: 12 641 nodes, 3 201 drivable `highway` ways, 272 `type=restriction` relations.
- Bounding box (S, W, N, E): `40.6950, -74.0250, 40.7350, -73.9850`, i.e. the bounding box of the
  449 MN01 baskets and the MN01G depot plus about 1 km.
- SHA-256: `e1f2fabaa937117f3a4c25582fb1ac4026fbb6fe8f3b9882f705e88a341d6a01`.
- **Routing version ID: `osm-mn01-e1f2fabaa937`** (`osm-mn01-` plus the first 12 hex characters).
- Query: `backend/src/main/resources/routing/mn01-roads.overpassql`

```
[out:xml][timeout:180];
(
  way["highway"~"^(motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential|living_street|service|road)$"](40.6950,-74.0250,40.7350,-73.9850);
  relation["type"="restriction"](40.6950,-74.0250,40.7350,-73.9850);
);
(._;>;);
out body;
```

- Refresh: `tools/fetch-road-snapshot.sh /tmp/new.osm`. A refresh is a **new version**: replace the
  committed file and `wastems.routing.expected-sha256` together; retained evaluation runs keep
  their version ID.
- License: (c) OpenStreetMap contributors, ODbL 1.0. The extract is a database excerpt
  redistributed under the ODbL with this attribution.
- The GraphHopper graph cache is derived data in `backend/routing-cache/` (git-ignored, rebuilt
  automatically in about 2 s).

### 3.2 Traffic snapshot (NYC DOT, D-050)

The traffic overlay reads one committed, timestamped snapshot instead of calling the DOT at run
time (reproducible, works offline, tests need no network; the D-021 boundary is structural).

- **File:** `backend/src/main/resources/traffic/mn01-traffic-links.json` (9 705 bytes), SHA-256
  `2a1b9f1faa7fdffe63b547971e68d0eadbdad65b7c68f92b52be532549a19467`.
- **Snapshot id:** `nyc-dot-i4gi-tjb9:12:2a1b9f1faa7f` (`nyc-dot-i4gi-tjb9:<records>:<first 12 hex of the
  SHA-256>`).
- **Source:** DOT Traffic Speeds, NYC Open Data (Socrata `i4gi-tjb9`), no API key needed. The query is
  stored in the file (`query`): rows with `data_as_of` in a 120-minute window, newest first.
- **Retrieved:** 2026-11-01T21:53:38Z. **Feed time:** `2026-11-01T15:08:05` America/New_York
  = 2026-11-01T20:08:05Z (EST, after the DST change of that morning).
- **Window and selection:** 120 minutes, 4 301 rows, 125 links, **12 kept**: the newest row per
  `link_id`, keeping links with at least one vertex inside the bounding box of the §3.1 road extract
  (south 40.695, west -74.025, north 40.735, east -73.985); row fields unmodified.
- **Classification of this snapshot:** 8 observed, 0 stale, 4 without a valid reading, 0 invalid.
- **Refresh:** `python3 tools/fetch-traffic-snapshot.py backend/src/main/resources/traffic/mn01-traffic-links.json`
  (stdlib only). A refresh is a **new snapshot version**: replace the file, update this section (SHA-256,
  counts, times) and the pinned counts in the tests, then run the tests. The running application never
  fetches the feed.
- **Limitations seen when the snapshot was prepared:** NYC Open Data publishes the feed about 1.5-3 hours
  late; replicas can disagree on the newest `data_as_of`; the `status` field is documented by the DOT
  as an artifact and is ignored; very low speeds can come from missing travel-time data (DOT
  metadata); `encoded_poly_line` carries doubled backslashes and `link_points` is cut at 255
  characters (both handled by the rules in §4); the DOT's own feed host was not reachable from the
  development machine.

## 4. Geospatial and routing rules

- Coordinates are stored and exchanged as **WGS84 longitude and latitude**. GeoJSON arrays use
  `[longitude, latitude]`; explicit API fields use `longitude` and `latitude` to avoid order
  ambiguity.
- A collection point retains its source dataset and source identifier in addition to its local
  database identifier.
- Tours follow a versioned road-network graph. Straight lines between stops are not accepted as
  route geometry or road distance.
- Real street directions can make `distance(A,B)` different from `distance(B,A)`. The optimizer
  must use directed leg costs, including when a 2-opt move reverses a stop segment.
- The optimizer consumes a fixed directed distance/time matrix built by GraphHopper 11.1
  (embedded, profile `car` with the bundled `car.json` custom model, contraction hierarchies,
  OSM turn restrictions) on the snapshot of §3.1. Every point must snap to a drivable road
  within 100 m; otherwise the evaluation fails explicitly. GraphHopper and map rendering remain
  outside the algorithm's core sequencing logic.
- The `car` profile approximates a collection truck: truck-specific restrictions (height,
  weight, turning radius) and kerbside constraints are not modelled.
- NYC DOT traffic is a **display-only overlay**, not a rerouting input (implemented in S9, D-050).
  It is served from the committed snapshot of §3.2. Each link is classified: `OBSERVED` (a valid
  reading no more than 15 minutes older than the feed time), `STALE` (older than that), `NO_DATA`
  (a sensor link without a valid speed or travel time) or `INVALID` (blank id, unparsable time,
  unusable geometry or an implausible speed above 100 mph; counted, never drawn). Geometry comes from
  the decoded `encoded_poly_line` (precision 1e5, after reducing doubled backslashes), else from
  `link_points` (the last token is dropped when the field reaches 255 characters). Observed speeds
  fall into the bands under 10 mph, 10-25 mph and 25 mph and over. Times are converted from
  `America/New_York`. The package `com.ase.wastems.traffic` is isolated (no reference to
  optimization, routing, services, domain or persistence, enforced by a test) and never changes route
  costs or results. A missing snapshot is shown as unavailable. Timestamps and limited coverage are
  visible, and experimental comparisons never change traffic input between methods.
- Vehicle GPS tracking, automatic incident avoidance, and traffic-aware dynamic rerouting are
  outside the current scope.

## 5. Required map interactions

The React interface will use Leaflet/React Leaflet with an attributed OSM-compatible basemap.
Maps are required for the following workflows, with equivalent lists/forms where needed for
accessibility:

1. display and filter collection points and the depot;
2. inspect a point's source, type, demand, and status in a popup or linked side panel;
3. create or correct a point by clicking the map and confirming a form;
4. select stops for a tour from the map and see their visit order;
5. display road-following tour geometry and completed/pending stops;
6. compare baseline and optimized tours using distinct route layers and indicators; and
7. toggle the traffic layer and inspect its observation time and coverage.

Implemented in S6 on the tour planner page. A comparison map draws one toggleable
road-following layer per method from the retained run's leg GeoJSON, numbers the stops in a
selected method's order, and sits beside an indicator table (distance, duration, changes from
input order, execution time, checks) with accessible visit-order lists. The planner map draws the
attached road route of the tour (item 5, planned side; execution states added in S7). Traffic is
not shown and is not an optimizer input (D-021).

Implemented in S7: the agent's tour page draws the depot, the stops numbered in visit order and
coloured by execution state (pending, collected, not collected) and the attached road route; the
same states colour the planner map of a non-draft tour. The ordered stop list carries the same
information without the map (NFR-03). There is no live vehicle position (D-020).

Implemented in S9: item 7 - a `NYC DOT traffic (display only)` switch on the collection-point and
tour planner maps draws only the sensor links of the committed snapshot, coloured by speed band
(stale and no-reading links in grey, dashed), with the observation time, the snapshot age, counts,
the coverage note, attribution and the DOT disclaimer. Unsensed streets get no colour. The layer never
changes routes or optimization (D-021).

The map is not merely a static illustration: selection, editing, planning, execution, and
comparison workflows must remain synchronized with the underlying REST resources.

## 6. Attribution and disclaimer

NYC data use remains subject to the [NYC Open Data Terms of
Use](https://www.nyc.gov/home/terms-of-use.page). OSM-based maps must visibly attribute
**© OpenStreetMap contributors** and link to the [OSM copyright page](https://www.openstreetmap.org/copyright).

NYC DOT requests the following disclaimer for applications using its data:

> This application may contain and/or utilize information which was originally compiled by the
> New York City Department of Transportation (NYC DOT) for governmental purposes; the
> information may subsequently been modified by entity/entities other than NYC DOT. NYC DOT and
> the City of New York make no representation as to the accuracy or usefulness of the
> information provided by this application or the information's suitability for any purpose and
> disclaim any liability for omissions or errors that may be contained therein. The public is
> advised to observe posted signage for compliance with applicable laws and regulations.

The disclaimer and the attribution `Traffic speeds: NYC DOT, NYC Open Data (i4gi-tjb9)` are shown in
the traffic panel under the maps whenever the layer is switched on.
