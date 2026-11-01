#!/usr/bin/env bash
# Fetches a fresh OSM road extract for the MN01 area (D-037).
# Usage: tools/fetch-road-snapshot.sh <output.osm>
# Alternative Overpass endpoints (set OVERPASS_URL):
#   https://overpass.kumi.systems/api/interpreter
#   https://maps.mail.ru/osm/tools/overpass/api/interpreter
# A refresh produces new bytes and therefore a NEW routing version ID. The committed file and
# wastems.routing.expected-sha256 must be replaced together; old evaluation runs keep their old
# version ID.
set -euo pipefail

if [ $# -ne 1 ]; then echo "usage: $0 <output.osm>" >&2; exit 2; fi
OUT=$1
if [ -e "$OUT" ]; then echo "refusing to overwrite existing file: $OUT" >&2; exit 1; fi
QUERY="$(dirname "$0")/../backend/src/main/resources/routing/mn01-roads.overpassql"

curl -fsS --max-time 240 -A 'project-ase road snapshot' --data-urlencode "data@$QUERY" \
  "${OVERPASS_URL:-https://overpass-api.de/api/interpreter}" -o "$OUT"

SHA=$(sha256sum "$OUT" | cut -d' ' -f1)
echo "bytes:   $(stat -c %s "$OUT")"
echo "sha256:  $SHA"
echo "osm_base: $(grep -o 'osm_base="[^"]*"' "$OUT" | head -1)"
echo "version: osm-mn01-${SHA:0:12}"
