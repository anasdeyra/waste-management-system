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
| Routable road network and basemap | [OpenStreetMap](https://www.openstreetmap.org/) | Road-following paths, one-way/access restrictions, route geometry, and map background | OSM data and tile infrastructure are separate: data use requires ODbL attribution, and public tile usage must follow the provider's policy. |

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
- The optimizer consumes a fixed directed distance/time matrix. GraphHopper and map rendering
  remain outside the algorithm's core sequencing logic.
- NYC DOT traffic is an **overlay**, not a live rerouting input. Its timestamp and limited
  coverage are visible, and experimental comparisons do not change traffic input between
  methods. A refresh retrieves a bounded current per-link snapshot rather than the full
  historical table, retains each record's `data_as_of`, and rejects invalid/stale observations.
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
