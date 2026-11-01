# Evaluation CSV exchange and the seeded experiment

Applies to the evaluation explorer, the scenario import (US-37), the results export (US-38) and
the seeded experiment generator (D-016, D-022, D-050). The routes, columns and rules below are
the ones implemented and tested in S9.

## 1. Scope

- The exchange covers **evaluation data only** (D-028): retained three-method comparisons
  (`INPUT_ORDER`, `NEAREST_NEIGHBOR`, `NN_PLUS_2_OPT`) of tours, experiment scenarios and
  imported scenarios. Operational tours, users, vehicles, agents, waste categories and execution
  records are never exported, and nothing operational can be imported.
- Who: PLANNER and ADMIN (ADMIN inherits PLANNER) may open the explorer, import scenarios and
  export results. Running an experiment batch through `POST /api/admin/experiments` is
  ADMIN only. AGENT gets 403 and anonymous callers 401 everywhere.
- Every run keeps the frozen scenario (stops, depot, capacity, service time, directed matrix), the
  source snapshot identifiers, the road graph version and the routing engine, so any reported
  number can be recomputed.

## 2. Seeded experiment generator `mn01-v1`

The generator samples **real DSNY baskets** of the committed MN01 snapshot
(`backend/src/main/resources/data/nyc-mn01-baskets.csv`, snapshot id
`8znf-7b2c:449:a0737237e370`) and attaches **synthetic demand**. The depot is the real MN01G depot
(snapshot `xw3j-2yxf:1:207851c2e229`).

- **Pool.** Rows whose trimmed `baskettype` starts with `DSNY ` and that the importer accepts
  (non-blank id and type, first occurrence of the id, parsable coordinates in the service area):
  238 baskets, sorted by `basketid` with `String.compareTo`.
- **Locations.** `java.util.Random(seed)`; a Fisher-Yates shuffle of the sorted pool
  (`for i = n-1 .. 1: j = nextInt(i+1); swap(i, j)`); the scenario takes the first `size` entries.
  That order is the input order used by the `INPUT_ORDER` baseline.
- **Demand** (whole kilograms, category `Mixed litter`, synthetic), drawn point by point in input
  order from `java.util.Random(31 * seed + profileCode)`:

  | Profile | Code | Draw per point |
  |---|---|---|
  | `UNIFORM_LOW` | 1 | `10 + nextInt(21)` (10-30 kg) |
  | `UNIFORM_HIGH` | 2 | `30 + nextInt(31)` (30-60 kg) |
  | `HOTSPOT` | 3 | `nextInt(5) == 0 ? 60 + nextInt(61) : 5 + nextInt(16)` (one point in five 60-120 kg, otherwise 5-20 kg) |

- **Fixed inputs.** Vehicle capacity `wastems.experiment.vehicle-capacity-kg` = **10 000 kg** (an
  assumed rear-loader payload); service time 600 s per stop. The largest canonical scenario
  (100 points, seeds 1-5, any profile) needs 4 639 kg, so **every canonical scenario fits** and
  the experiment measures sequencing, not capacity. Capacity infeasibility is exercised by tests
  and by imported scenarios.
- **Scenario key** `mn01-<PROFILE>-s<seed>-n<size>`, for example `mn01-UNIFORM_LOW-s1-n10`.
- **Canonical matrix** (the fixed experiment): profiles `UNIFORM_LOW, UNIFORM_HIGH, HOTSPOT` x
  seeds `1,2,3,4,5` x sizes `10,25,50,100` = **60 scenarios**. A batch is *canonical* exactly when
  it uses all three profiles, seeds exactly 1-5 and sizes exactly 10, 25, 50, 100 (any number of
  repetitions). Execution order: for each repetition, each profile, each seed ascending, each
  size ascending.
- **Determinism and nesting.** The same profile, seed and size always give the identical frozen
  scenario and, on any database, identical matrices, visit orders, distances, travel times,
  durations and feasibility (only execution time, ids and timestamps differ). Sizes are nested
  (the first 10 points of a seed are the first 10 of its 25-point scenario) and the three
  profiles of one seed and size share the same points; only demand differs. Golden values for
  seed 1, size 10: baskets `10130030 10130234 10130154 10130100 10130238 10130246 10130245
  10130099 10130034 10130094`; `UNIFORM_LOW` demand `21 23 12 17 26 15 29 11 19 29`.
- **Request limits** (`POST /api/admin/experiments`, field-level 400s): 1-3 profiles without
  duplicates; 1-10 distinct seeds (1 to 1 000 000); 1-10 distinct sizes (2 to 100); repetitions 1-3;
  label at most 120 characters; at most 180 runs per batch. Missing lists default to the
  canonical values.

## 3. Scenario import CSV

`POST /api/evaluations/imports?fileName=&label=` with the raw CSV as the request body and
`Content-Type: text/csv` (no multipart). PLANNER and ADMIN. The whole file is validated first: any
error answers **400** with `rowErrors` and **nothing** is created. After validation the MN01G depot
must exist (409) and routing must be available (503). Then every scenario is evaluated in memory
by the three methods and the batch, its runs and one audit event are saved in **one transaction**.
The answer is 201, `Location: /api/evaluations/batches/{id}` and the batch detail.

**Format.** UTF-8 (a leading BOM is ignored), comma separator, RFC 4180 quoting, header row
required, `\n` or `\r\n`. Row numbers count the header as row 1. **Limits:** body 1 to 1 048 576
bytes (else 400 on field `file`), at most 5 000 data rows, at most 10 scenarios, 1 to 100 points
per scenario.

| Column | Required | Rule |
|---|---|---|
| `scenario_key` | yes | 1-80 characters of `[A-Za-z0-9._-]`; groups the rows of one scenario |
| `input_order` | yes | integer >= 1; per scenario exactly 1..n without gaps; it is the baseline order |
| `point_id` | one identity form | UUID of an existing collection point |
| `source_dataset`, `source_identifier` | one identity form | source key of an existing collection point (for example `8znf-7b2c`, `10130030`) |
| `longitude`, `latitude` | yes | decimals inside WGS84 ranges and the service area; must equal the referenced point's stored coordinates within 1e-6 degrees |
| `category` | yes | name of an existing **active** waste category (case-insensitive) |
| `demand_kg` | yes | decimal > 0, at most 100 000, at most 2 decimals |
| `vehicle_capacity_kg` | yes | decimal > 0, at most 100 000, at most 2 decimals; the same on every row of a scenario |
| `demand_profile` | no | at most 40 characters; the same on every row of a scenario |
| `seed` | no | integer 0 to 9 223 372 036 854 775 807; the same on every row of a scenario |

Column names are exact and lower case, in any order; unknown or duplicate columns are errors. The
header must contain every required column and either `point_id` or both `source_dataset` and
`source_identifier` (it may contain all three).

**Row rules.** Exactly one identity form per row (both given is an error, none is an error). The
point must exist and must not carry a non-DSNY ownership label (`point <ref> is not a DSNY basket
(<label>)`, D-019). One row per point and category: rows of a scenario with the same
`input_order` must reference the same point, may not repeat a category, and the same point may not
appear under two input orders. Cross-row and scenario-level checks consider only rows without a
field error, so one bad field produces one error, not a cascade.

**Errors.** Collected over the whole file, sorted by row, at most 100 reported:

```json
{"status":400,"error":"Bad Request","message":"The CSV has 2 errors; nothing was imported.",
 "rowErrors":[{"row":2,"column":"category","message":"category Plastic not found"},
              {"row":4,"column":"demand_kg","message":"must be > 0"}]}
```

`column` is omitted for record-level errors; scenario-level errors use the scenario's first row.
With more than 100 errors the message reads `The CSV has <N> errors (first 100 shown); nothing was
imported.`

**Evaluation of an imported scenario.** Stops in `input_order`, each with the point's id, source
dataset and identifier and name, the CSV coordinates, demand = the sum of the rows' `demand_kg`
and `demand_breakdown` = `<category>=<kg>` per category (two decimals, sorted by category name,
joined by `; `). Capacity, profile and seed come from the CSV, the depot is MN01G, and the
source snapshot ids are those of every referenced dataset (`<dataset>:unknown` without a snapshot
row, `USER_ENTERED` for a point without a source dataset). Locations keep their source ids; the
demand is the CSV's (synthetic) value. All three methods use the same frozen scenario.

**Valid example** (the coordinates are the snapshot coordinates of these baskets):

```
scenario_key,input_order,source_dataset,source_identifier,longitude,latitude,category,demand_kg,vehicle_capacity_kg,demand_profile,seed
lower-broadway-3,1,8znf-7b2c,10130067,-74.00212207,40.71528035,Mixed litter,18,10000,,
lower-broadway-3,2,8znf-7b2c,10130002,-74.00821676,40.71706895,Mixed litter,12.5,10000,,
lower-broadway-3,3,8znf-7b2c,10110232,-74.0115043,40.71560317,Mixed litter,22,10000,,
```

## 4. Results export CSV

`GET /api/evaluations/export?kind=&batchId=&runId=` (all optional, combined with AND; an unknown id
gives a header-only file) returns `text/csv;charset=UTF-8` as an attachment named
`evaluation-results-<suffix>.csv` (`run-<id>`, `batch-<id>`, `<kind>` or `all`). UTF-8 without BOM,
`\n` line ends, RFC 4180 quoting. **One row per run and planning method** in the order
`INPUT_ORDER`, `NEAREST_NEIGHBOR`, `NN_PLUS_2_OPT`; runs by creation time then id.

| Column | Unit / format | Meaning |
|---|---|---|
| `run_id` | UUID | evaluation run |
| `scenario_kind` | `TOUR_DRAFT`, `EXPERIMENT`, `CSV_IMPORT` | origin of the scenario |
| `batch_id` | UUID | experiment or import batch (empty for tour runs) |
| `scenario_key` | text | `mn01-...` key or the CSV `scenario_key` |
| `demand_profile` | text | experiment profile or the CSV value |
| `seed` | integer | generator seed or the CSV value |
| `repetition` | integer | repetition of an experiment batch |
| `point_count` | integer | number of stops |
| `tour_id`, `tour_revision` | UUID, integer | tour evaluations only |
| `run_status` | `COMPLETED`, `ROUTING_FAILED` | state of the run |
| `failure_code` | `POINT_NOT_ROUTABLE`, `NO_ROAD_PATH` | routing failure |
| `created_at` | ISO-8601 UTC | when the run was computed |
| `created_by` | username | who started it (`system` for the CLI) |
| `method` | enum | planning method of this row |
| `feasible` | `true`/`false` | coverage and capacity both satisfied |
| `coverage_ok`, `capacity_ok` | `true`/`false` | the two feasibility checks |
| `infeasibility_reason` | text | reason, or the routing failure message |
| `distance_m` | metres, `%.3f` | route length on the directed road network |
| `travel_time_s` | seconds, `%.3f` | driving time |
| `service_time_per_stop_s` | seconds, `%.3f` | service time per stop (600) |
| `service_time_total_s` | seconds, `%.3f` | per-stop time x `point_count` |
| `duration_s` | seconds, `%.3f` | `travel_time_s + service_time_total_s` |
| `execution_time_ms` | milliseconds, `%.6f` | wall time of the method (not deterministic) |
| `input_order_distance_m`, `input_order_duration_s` | `%.3f` | the baseline of the same run |
| `distance_change_pct`, `duration_change_pct` | percent, `%.4f` | change against the input order |
| `total_demand_kg`, `vehicle_capacity_kg` | kilograms | scenario totals |
| `visit_order` | input indexes, space separated | stop sequence (empty when infeasible) |
| `visit_order_source_ids` | source ids, space separated | the visited stops' `source_identifier` (`point:<uuid>` when a stop has none) |
| `algorithm_configuration` | text | parameters of the method |
| `source_snapshot_ids` | `;`-joined | snapshot ids of the source data |
| `road_graph_version`, `routing_engine` | text | road graph and engine |

- **Percent change** `(value - input_order_value) / input_order_value x 100` (negative = reduction),
  computed in one place (`EvaluationComparison`). It is empty unless the run is `COMPLETED`, both
  the method and `INPUT_ORDER` are feasible and the input-order value is above 0. For
  `INPUT_ORDER` itself it is `0.0000` when feasible.
- **Recalculation.** For every feasible row, `(distance_m - input_order_distance_m) /
  input_order_distance_m x 100` equals `distance_change_pct` within 0.001, likewise for duration,
  and `duration_s = travel_time_s + service_time_total_s` within rounding.
- **Failure rows.** A `ROUTING_FAILED` run gives three rows (one per method) with
  `run_status=ROUTING_FAILED`, the `failure_code`, `feasible=false`, `infeasibility_reason` = the
  failure message and every indicator column empty. An infeasible method of a completed run has
  `feasible=false`, its reason, the two checks and the execution time, and empty distance,
  duration and change columns.

## 5. Scenario export (round trip)

`GET /api/evaluations/batches/{id}/scenarios.csv` returns the batch's scenarios **in the import
format** (section 3) as `evaluation-scenarios-<batchId>.csv`, 404 for an unknown batch. Header:
`scenario_key,input_order,point_id,source_dataset,source_identifier,longitude,latitude,category,demand_kg,vehicle_capacity_kg,demand_profile,seed`.
Only runs of repetition 1 are written; a stop gives one row per category of its demand breakdown;
`point_id` is filled only when a stop has no source key, otherwise the source columns are filled;
coordinates are written with `Double.toString` and kilograms with two decimals. Importing the file
unchanged re-evaluates the same scenarios and gives the same matrices, visit orders, distances and
durations.
