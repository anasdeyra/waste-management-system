# WasteMS — Intelligent Waste Management System

Map-centric management system for municipal waste collection, organised around five core areas
— **collection points, vehicles, agents, tours and waste** — plus **real street routing**, an
**intelligent tour-optimization module**, and a **reproducible impact evaluation** against a
documented baseline. The worked example uses official DSNY locations in **New York City,
Lower Manhattan (MN01)**, with interactive maps for planning, execution, and comparison.

This repository is being developed as the deliverable of the Advanced Software Engineering
course project. Its intended final contents are the problem analysis, literature review, UML
models, prototype, Scrum artifacts, and software metrics; the status table below distinguishes
completed artifacts from planned work.

> **Current state: Sprint S8 delivered.** Session authentication with CSRF protection, the
> ADMIN / PLANNER / AGENT roles, user administration, waste categories, collection points with
> demand entries on a Leaflet/OpenStreetMap map (S2), and (S3) vehicles and operational agents
> with availability, the ADMIN-only agent to AGENT-account link, and the idempotent import of
> the included MN01 snapshots (449 baskets, of which 238 `DSNY ` types are
> optimization-eligible, plus the MN01G depot) with snapshot identifiers are implemented
> (backend API and React UI). **Tour drafts (S4):** planning date + depot, map/list stop
> selection in an explicit input order, vehicle + driver + up to two collectors, live capacity
> and structural validation. The map shows the input order as straight dashed lines, not a road
> route. **Directed routing and three-method evaluation (S5):** embedded GraphHopper 11.1 on the
> versioned OSM snapshot `osm-mn01-e1f2fabaa937`; `POST /api/tours/{id}/evaluations` runs input
> order, nearest neighbor and NN + 2-opt on the frozen draft and retains the run (orders, road
> legs, distance, duration, execution time, checks, snapshot IDs). **Review and planning
> gate (S6):** the tour planner page compares the three road routes on a map and in an
> indicator table. The planner accepts the NN + 2-opt proposal or keeps the input order, which
> is refused when stale, and confirms a fully revalidated draft as `PLANNED`, which locks its
> structure.
> **Execution and demonstration (S7):** an assigned agent sees only their own planned,
> in-progress and completed tours, starts a planned tour after a revalidation (availability,
> active points, current route), records each stop as collected (quantities by category) or not
> collected (reason), and completes the tour once every stop is resolved. A `dev` start loads a
> reproducible MN01 demonstration (see §4.6). The real MN01 source extracts are under
> `backend/src/main/resources/data/`.
>
> **Corrections, history and audit (S8):** planners cancel a draft, planned or in-progress tour with
> a reason, correct a recorded stop outcome (the previous values are kept), and search the history
> of completed and cancelled tours. Administrators search an append-only audit log of sign-ins
> and data changes (`/admin/audit`).

The real-city sources, included snapshots, map interactions, routing rules, and attribution
are documented in [`docs/geospatial-data.md`](docs/geospatial-data.md). Traffic is a timestamped
map overlay; it does not drive dynamic rerouting or the controlled optimization experiment.

---

## 1. Technology stack

| Layer | Technology | Version |
|---|---|---|
| Backend | Java (Eclipse Temurin LTS) | 21 |
| Backend framework | Spring Boot (Web, Data JPA, Security, Validation) | 3.5.16 |
| Build tool | Apache Maven (with wrapper) | 3.9.16 |
| Database | H2, file mode | 2.x |
| Frontend | React + TypeScript | 19 / 5.x |
| Build tool | Vite | 8.x |
| Styling | Tailwind CSS | 4.x |
| Components | shadcn/ui (Radix UI) | latest |
| Data fetching | TanStack Query + React Router | 5.x / 7.x |
| Interactive maps | Leaflet + React Leaflet, standard OSM tiles (D-039) | Leaflet 1.9 / React Leaflet 5 |
| Road routing | GraphHopper (embedded) + versioned OpenStreetMap extract `osm-mn01-e1f2fabaa937` | 11.1 |
| Tests | JUnit 5, AssertJ, Spring Security Test | — |
| Metrics | PMD, CKJM, JDepend, `tools/count-loc.py` | — |

The stack and geospatial direction are recorded in
[`docs/decisions.md`](docs/decisions.md) (D-004, D-013, D-017–D-022).

## 2. Repository layout

```
project-ase/
├── AGENTS.md                  # workspace conventions and status
├── project-assignment.md      # the assignment (canonical)
├── README.md                  # this file
├── backend/                   # Spring Boot REST API (Java 21, Maven)
│   ├── mvnw                   # Maven wrapper
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/ase/wastems/
│       │   ├── domain/ repository/ service/ web/ security/ config/
│       │   ├── optimization/  # framework-free algorithm core (input order, NN, directed 2-opt)
│       │   ├── routing/       # embedded GraphHopper on the OSM snapshot
│       │   ├── traffic/       # display-only NYC DOT traffic overlay (isolated, D-021)
│       │   └── WasteMsApplication.java
│       ├── main/resources/    # application.yml + real NYC snapshots in data/ + OSM road snapshot in routing/ + traffic snapshot in traffic/
│       └── test/              # JUnit 5 tests
├── frontend/                  # React single-page application (Vite, Tailwind, shadcn/ui)
│   ├── package.json
│   ├── vite.config.ts         # @ alias, Tailwind plugin, /api proxy
│   ├── components.json        # shadcn/ui configuration
│   └── src/
│       ├── components/ui/     # shadcn/ui components (vendored)
│       ├── components/shared/ # app shell, route guards
│       ├── components/maps/   # Leaflet base map and layers
│       ├── components/collection-points/  # list, details, form, demand editor
│       ├── components/tours/  # tour header, stop list, candidates, assignment, capacity
│       ├── pages/ hooks/ types/  # pages (incl. tours-page, tour-planner-page), hooks, typed API contract
│       ├── lib/               # cn(), API client, auth helpers
│       ├── App.tsx            # routes
│       └── main.tsx           # providers: Query, Router
├── docs/
│   ├── decisions.md           # decision log
│   ├── geospatial-data.md     # NYC sources, snapshots, maps, routing, attribution
│   ├── evaluation-csv.md      # evaluation CSV import/export, seeded experiment, CLI
│   ├── 01-analysis.md         # Part 1 — problem analysis
│   ├── 02-literature-review.md
│   ├── 03-uml/                # Part 3 — UML models (Mermaid)
│   ├── 05-scrum.md            # Part 5 — Scrum artifacts
│   └── 06-metrics.md          # Part 6 — software metrology
├── tools/                     # toolchain installer, metric scripts, fetch-road-snapshot.sh,
│                              # fetch-traffic-snapshot.py, run-experiment.sh
└── presentation/              # final deck
```

## 3. Prerequisites

| Tool | Required version | Notes |
|---|---|---|
| Node.js | 20.19+ / 22.12+ | provided by `fnm` or `nvm` on the development machine |
| npm | 10+ | ships with Node.js |
| JDK 21 | 21 | installed locally by `tools/setup-toolchain.sh` |
| Maven | 3.9+ | installed locally by `tools/setup-toolchain.sh` |

The development machine has no system package manager and no root access, so the Java
toolchain is installed as user-local tarballs. Install it once with:

```bash
bash tools/setup-toolchain.sh
. "$HOME/.local/opt/ase-env.sh"   # sets JAVA_HOME, MAVEN_HOME and PATH
```

The script is idempotent: it skips anything that is already present.

## 4. Running the application

### 4.1 Backend

```bash
cd backend
./mvnw spring-boot:run          # or: mvn spring-boot:run
```

The API listens on <http://localhost:8080>. It starts with the `dev` Spring profile by default
(`spring.profiles.default: dev`), which creates three accounts on first start:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `planner` | `planner123` | PLANNER |
| `agent` | `agent123` | AGENT |
| `collector` | `collector123` | AGENT (demo) |
| `agent2` | `agent2123` | AGENT (demo) |

`collector` and `agent2` are created by the demonstration load (§4.6); `agent` is linked to the
demo agent *Alex Rivera (demo)*.

These are **development credentials only: change or remove them for any shared deployment.**
The `dev` profile also seeds a small map sample, idempotently: two demo waste categories
(`Mixed litter`, `Recyclables`), the real MN01G depot, and five real DSNY baskets from the
included snapshots (four active, one inactive) with **synthetic** demand.

At `dev` start-up, after the accounts and the map sample, the MN01 import runs. It upserts on
source dataset + id, so the five sample baskets are not duplicated and local edits are kept
(only the source ownership label is refreshed). It gives each new point one synthetic
`Mixed litter` demand of 10 to 30 kg, and it records the snapshot identifiers (dataset, row
count and checksum). An ADMIN can re-run it from the Data import page or with
`POST /api/admin/import`. Only `DSNY ` basket types (238 of 449) are optimization-eligible.

To start without seeding, use any non-`dev` profile, e.g.
`./mvnw spring-boot:run -Dspring-boot.run.profiles=prod`. To reset the database, stop the
application and delete `backend/data/`.

 The first evaluation (or the `dev` warm-up at start) imports the road graph into
`backend/routing-cache/` (about 2 s, about 8 MB, git-ignored; delete it to rebuild);
`--wastems.routing.cache-dir=<dir>` moves it.

 The H2 database file is created on first start in
`backend/data/`; the H2 console is available at <http://localhost:8080/h2-console> (JDBC URL
`jdbc:h2:file:./data/wastems`, user `sa`, empty password).

### 4.2 Frontend

```bash
cd frontend
npm install
npm run dev
```

The application is served on <http://localhost:5173>. The Vite development server proxies
`/api` to `http://localhost:8080`, so no CORS configuration is required.

To run a second frontend against a backend on another port (for example a scratch backend on
8081), override the proxy target and port:

```bash
API_PROXY_TARGET=http://localhost:8081 npx vite --port 5174 --strictPort
```

### 4.3 Production build

```bash
cd backend  && ./mvnw clean package        # -> backend/target/wastems-backend-0.1.0-SNAPSHOT.jar
cd frontend && npm run build               # -> frontend/dist
```

### 4.4 API authentication (curl)

The API uses an HttpSession cookie (`JSESSIONID`) plus a CSRF token: `GET /api/auth/csrf` sets
the cookie `XSRF-TOKEN`; send its value back as the header `X-XSRF-TOKEN` on every
POST/PUT/PATCH request. The CSRF cookie is cleared on logout, so fetch a token again afterwards.

```bash
B=http://localhost:8080; J=/tmp/jar
curl -s -c $J -b $J $B/api/auth/csrf                      # sets XSRF-TOKEN
X=$(awk '$6=="XSRF-TOKEN"{print $7}' $J)
curl -s -c $J -b $J -H "X-XSRF-TOKEN: $X" -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"admin123"}' $B/api/auth/login   # sets JSESSIONID
curl -s -b $J $B/api/auth/me                              # {"id":...,"username":"admin","role":"ADMIN"}
curl -s -b $J $B/api/collection-points?status=ACTIVE      # collection points with demand entries
curl -s -b $J $B/api/vehicles?status=AVAILABLE            # available vehicles (PLANNER/ADMIN)
curl -s -b $J $B/api/admin/import/report                  # last MN01 import report (ADMIN)
# link an operational agent to an AGENT account (ADMIN); $AGENT_ID and $USER_ID are UUIDs
curl -s -b $J -X PUT -H "X-XSRF-TOKEN: $X" -H 'Content-Type: application/json' \
     -d "{\"userId\":\"$USER_ID\"}" $B/api/agents/$AGENT_ID/user-account
```

Tour drafts (PLANNER/ADMIN). `$DEPOT_ID`, `$POINT_ID`, `$VEHICLE_ID` and `$AGENT_ID` are UUIDs;
`$TOUR_ID` and `$STOP_ID` come from the responses (every tour response carries `stops[].id`,
the capacity report, `revision` and `validationIssues`):

```bash
H=(-H "X-XSRF-TOKEN: $X" -H 'Content-Type: application/json')
# create a draft (planning date must be today or later); the Location header names the tour
curl -s -i -b $J "${H[@]}" -d "{\"planningDate\":\"$(date -d tomorrow +%F)\",\"depotId\":\"$DEPOT_ID\"}" $B/api/tours
# add a stop (appended at the end; demand is snapshotted), then reorder with the full id list
curl -s -b $J "${H[@]}" -d "{\"collectionPointId\":\"$POINT_ID\"}" $B/api/tours/$TOUR_ID/stops
curl -s -b $J -X PUT "${H[@]}" -d "{\"stopIds\":[\"$STOP_ID\"]}" $B/api/tours/$TOUR_ID/stops/order
# replace vehicle, driver and collectors (at most two) together
curl -s -b $J -X PUT "${H[@]}" \
     -d "{\"vehicleId\":\"$VEHICLE_ID\",\"driverId\":\"$AGENT_ID\",\"collectorIds\":[]}" $B/api/tours/$TOUR_ID/assignment
```

Three-method evaluation of a ready draft (PLANNER/ADMIN):

```bash
curl -s -i -b $J -X POST "${H[@]}" $B/api/tours/$TOUR_ID/evaluations   # 201, Location: /api/evaluations/<runId>
curl -s -b $J $B/api/evaluations/$RUN_ID                                 # three method results with orders, legs, indicators
curl -s -b $J $B/api/evaluations/$RUN_ID/matrix                          # retained directed distance/time matrix
curl -s -b $J $B/api/tours/$TOUR_ID/evaluations                          # runs of the tour, newest first
# accept the NN + 2-opt proposal of a current run, or keep the input order (both 200, 409 when stale)
curl -s -b $J "${H[@]}" -d "{\"evaluationRunId\":\"$RUN_ID\"}" $B/api/tours/$TOUR_ID/proposal-acceptances
curl -s -b $J "${H[@]}" -d "{\"evaluationRunId\":\"$RUN_ID\"}" $B/api/tours/$TOUR_ID/proposal-rejections
# confirm as PLANNED (422 lists the blocking issues; structural edits of a planned tour answer 409)
curl -s -b $J -X POST "${H[@]}" $B/api/tours/$TOUR_ID/confirmation
```

Execution by the assigned crew (AGENT):

```bash
# as an AGENT (log in as agent/agent123 first): assigned tours, detail, start, outcome, completion
curl -s -b $J $B/api/tours/mine
curl -s -b $J $B/api/tours/mine/$TOUR_ID
curl -s -b $J -X POST "${H[@]}" $B/api/tours/$TOUR_ID/start                 # 422 lists start issues
curl -s -b $J "${H[@]}" -d "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\"$CATEGORY_ID\",\"actualQuantityKg\":12.5}]}" \
     $B/api/tours/$TOUR_ID/stops/$STOP_ID/outcome
curl -s -b $J "${H[@]}" -d '{"status":"NOT_COLLECTED","incidentReason":"Blocked by a parked truck"}' \
     $B/api/tours/$TOUR_ID/stops/$STOP_ID/outcome
curl -s -b $J -X POST "${H[@]}" $B/api/tours/$TOUR_ID/completion            # 422 STOP_PENDING while a stop is pending
```

Cancellation, corrections, history and the audit log (S8):

```bash
# cancel a tour with a reason (PLANNER/ADMIN; 409 when COMPLETED or CANCELLED)
curl -s -b $J "${H[@]}" -d '{"reason":"Street fair on Broadway"}' $B/api/tours/$TOUR_ID/cancellation
# correct a recorded stop outcome (PLANNER/ADMIN; the previous values are kept)
curl -s -b $J "${H[@]}" -d "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\"$CATEGORY_ID\",\"actualQuantityKg\":18}],\"reason\":\"Weighbridge ticket\"}" \
     $B/api/tours/$TOUR_ID/stops/$STOP_ID/corrections
# history of completed and cancelled tours (filters: from, to, status, vehicleId, agentId, pointId)
curl -s -b $J "$B/api/tours/history?status=COMPLETED"
# audit log (ADMIN; filters: from, to (UTC, ISO-8601 with Z), actor, action, entityType, entityId, outcome, page, size)
curl -s -b $J "$B/api/admin/audit-events?action=SIGN_IN&outcome=FAILURE"
```

Dashboard, evaluation explorer, CSV exchange, experiment batches and traffic overlay (S9):

```bash
# operational dashboard (PLANNER/ADMIN; optional planning-date period)
curl -s -b $J "$B/api/dashboard/operations?from=2026-11-01&to=2026-11-07"
# run a small seeded experiment batch (ADMIN; omit the body for the fixed 60 scenarios, about 1-2 min)
curl -s -b $J -H "X-XSRF-TOKEN: $X" -H 'Content-Type: application/json' \
  -d '{"profiles":["UNIFORM_LOW"],"seeds":[1],"sizes":[10,25]}' $B/api/admin/experiments
# explorer: batches, one batch with aggregates, filtered runs
curl -s -b $J $B/api/evaluations/batches
curl -s -b $J "$B/api/evaluations?kind=EXPERIMENT&pointCount=10&page=0&size=50"
# export results (one row per run and method) and a batch's scenarios (import format)
curl -s -b $J -o results.csv "$B/api/evaluations/export?batchId=$BATCH"
curl -s -b $J -o scenarios.csv "$B/api/evaluations/batches/$BATCH/scenarios.csv"
# import scenarios (400 lists row errors and imports nothing)
curl -s -b $J -H "X-XSRF-TOKEN: $X" -H 'Content-Type: text/csv' --data-binary @scenarios.csv \
  "$B/api/evaluations/imports?fileName=scenarios.csv"
# NYC DOT traffic overlay (display only)
curl -s -b $J $B/api/traffic/overlay
```

A draft that is not ready answers 422 (e.g. `OVER_CAPACITY`, `NO_VEHICLE`); an unroutable point or
missing directed leg answers 201 with a retained run of status `ROUTING_FAILED`; a missing or
corrupt road snapshot answers 503. Evaluation never
modifies the draft; only an explicit accept or keep-input-order does.

Over-capacity is reported (`capacity.overCapacity`, issue `OVER_CAPACITY`), not rejected, in a
draft. There is no `DELETE /api/tours/{id}`.

Collection points, depots, vehicles, agents and tours are available to PLANNER and ADMIN only; AGENT
receives 403 **except** `/api/tours/mine…` and the start/outcome/completion endpoints, which are
AGENT only and restricted to the tour's crew (403 otherwise). Cancellation, corrections and history are PLANNER/ADMIN; the audit log is ADMIN only. Linking agents to accounts (`/api/agents/{id}/user-account`) and the data import
(`/api/admin/**`) are ADMIN only.

### 4.5 Map attribution

Base-map tiles come from OpenStreetMap and are shown with the required
"© OpenStreetMap contributors" attribution. Depot and basket locations come from NYC Open Data
(DSNY); see [`docs/geospatial-data.md`](docs/geospatial-data.md). Road routing uses an
OpenStreetMap extract (© OpenStreetMap contributors, ODbL). The optional traffic layer shows
"Traffic speeds: NYC DOT, NYC Open Data (i4gi-tjb9)" and the NYC DOT disclaimer in its panel.

### 4.6 Try the demo

**What is loaded and how.** The `dev` start (the default) loads the demonstration automatically
after the MN01 import. An ADMIN can reload it from *Data import → Demonstration data → Load
demonstration data* or with `curl -s -b $J -X POST -H "X-XSRF-TOKEN: $X" $B/api/admin/demo`
(report: `GET /api/admin/demo/report`). Loading again never duplicates anything and never resets
progress made in the UI. `--wastems.demo.enabled=false` skips it. Delete `backend/data/` to
start from scratch (stop the backend first).

**Real versus synthetic.**

| Real | Synthetic (`origin` `SYNTHETIC`) |
|---|---|
| The 449 MN01 basket locations and the MN01G garage (NYC Open Data, source ids kept) | Every demand quantity, the categories `Recyclables` and `Organics`, vehicles `DEMO-01…03`, the five agents named `… (demo)`, the demo accounts, the four tours and every collection outcome (reasons end with "(synthetic demo outcome)") |

**Accounts.**

| Username / password | Role | Linked agent |
|---|---|---|
| `admin` / `admin123` | ADMIN | |
| `planner` / `planner123` | PLANNER | |
| `agent` / `agent123` | AGENT | Alex Rivera (demo), driver |
| `collector` / `collector123` | AGENT | Blair Chen (demo) |
| `agent2` / `agent2123` | AGENT | Casey Morgan (demo) |

Dana Lopez (demo) has no login. Eli Novak (demo) and `DEMO-03` are unavailable.

**Tours** (dates are the day of the first load):

| Tour | State | Vehicle and crew | Stops | Road route |
|---|---|---|---|---|
| draft | `DRAFT` | DEMO-01, Alex + Blair | 8 baskets | evaluated (input order 11.74 km, NN 10.21 km, NN + 2-opt 7.47 km), not yet decided |
| planned | `PLANNED` | DEMO-01, Alex + Blair | 5 baskets | NN + 2-opt, 2.39 km |
| in-progress | `IN_PROGRESS` | DEMO-02, Alex + Casey + Dana | 5 baskets, stop 1 collected, stop 2 not collected | 2.75 km |
| completed | `COMPLETED` | DEMO-02, Casey + Alex | 4 baskets, 3 collected and 1 not collected | 6.11 km |

**What to click.**

1. Sign in as `planner`, open *Tours*: four tours with the `Synthetic demo` badge, one per state.
2. Open the draft, *Route evaluation*: compare the three routes; *Accept NN + 2-opt proposal*, then
   *Confirm as planned*.
3. Open the in-progress tour: the map colours stops by state and the *Execution* card shows the
   progress.
4. Sign out; sign in as `agent`, *My tours*: three tours (the draft is not visible).
5. Open the planned tour, *Start tour*; for each stop *Record outcome* (Collected with quantities,
   or Not collected with a reason); then *Complete tour*.
6. Sign in as `agent2`, *My tours*: the planned tour is absent; its URL answers "You are not
   assigned to this tour."
7. (Optional) as `planner`, deactivate a basket of a planned tour: the agent's *Start tour* is
   blocked with `INACTIVE_STOP` until it is reactivated.
8. As `planner`, open the completed tour, *Correct* its not-collected stop (for example
   Collected, Mixed litter 18 kg, reason "Weighbridge ticket"): the stop shows *Corrected* and the
   previous values.
9. As `planner`, open the planned tour and *Cancel tour* with a reason; `agent` sees it as
   cancelled in *My tours*.
10. As `planner`, open *History*: filter by status, vehicle, agent or collection point; the
    corrected and the cancelled tours are listed.
11. As `admin`, open *Audit log*: filter by action (`Outcome corrected`, `Sign-in` with outcome
    `Failure`, …), actor or the tour id.

12. As `planner`, open *Dashboard*: resources, tours by status and stop progress; click a count to
    open the filtered list (for example *Planned* opens *Tours* filtered).
13. As `admin`, open *Evaluations*, *Run experiment* with seeds `1` and sizes `10,25` (a few
    seconds): the batch shows per-size and per-profile changes against the input order; open a
    run to compare the three routes on the map.
14. *Export results CSV* and *Download scenarios CSV*; import the scenarios file again with
    *Import scenario CSV*: the new batch has the same distances.
15. On *Collection points* or a tour, switch on *NYC DOT traffic (display only)*: sensor
    links of the stored snapshot with their observation time; routes do not change.

The demo load itself is recorded as one `DEMO_LOADED` event (actor `system` at start-up); the tour
steps it performs appear as their own events.

On a database that already holds other data, the loader reuses existing names and skips what it
cannot do (for example an `agent` account linked to another agent); the warnings are in the report
and the log (`Demo load warning:`).

### 4.7 Evaluation experiment (for Part 6)

```bash
bash tools/run-experiment.sh metrics/experiment
```

Runs the fixed 60 scenarios (3 demand profiles x seeds 1-5 x 10, 25, 50, 100 real MN01 baskets) once,
in about 1-2 minutes, headless on its own database and a random port (never 8080). It writes
`results.csv`, `scenarios.csv`, `batch.json`, `run-info.txt` and `run.log` into the output
directory, which must be new or empty (`metrics/` is git-ignored). Add
`--wastems.experiment.repetitions=3` for execution-time medians. Columns, formats and the generator
are documented in [`docs/evaluation-csv.md`](docs/evaluation-csv.md).

## 5. Tests and metrics

```bash
cd backend && ./mvnw test                  # unit and integration tests
bash tools/metrics.sh                      # LOC, CKJM, PMD, JDepend (Part 6)
```

`tools/metrics.sh` writes the raw reports to `metrics/`. Every number quoted in
`docs/06-metrics.md` must come from that script so the results stay reproducible.

## 6. Documentation

| Part | Document | State |
|---|---|---|
| 1 — Problem analysis | [`docs/01-analysis.md`](docs/01-analysis.md) | complete |
| 2 — Literature review | [`docs/02-literature-review.md`](docs/02-literature-review.md) | complete |
| 3 — UML modelling | [`docs/03-uml/README.md`](docs/03-uml/README.md) | four required diagrams + OCL complete |
| 4 — Prototype | `backend/`, `frontend/` | S1–S8: authentication, roles, users, waste categories, collection points and demand on the map, vehicles, agents and account links, MN01 import, tour drafts, three-method evaluation, review and confirmation, crew execution, MN01 demonstration, cancellation, outcome corrections, operational history, audit log |
| 5 — Scrum | [`docs/05-scrum.md`](docs/05-scrum.md) | Product Backlog complete; S1–S8 executed, S9–S10 planned |
| 6 — Metrology | `docs/06-metrics.md` | tooling ready, measurements to do |
| Evaluation CSV and experiment | [`docs/evaluation-csv.md`](docs/evaluation-csv.md) | S9: import, export, seeded generator `mn01-v1`, CLI |
| Supporting geospatial specification | [`docs/geospatial-data.md`](docs/geospatial-data.md) | NYC snapshots included; point map implemented; routing to do |

## 7. Conventions

- Documentation is Markdown, in English, one file per assignment part.
- Diagrams are Mermaid inside Markdown so they render on GitHub without a toolchain.
- Commits are imperative and scoped: `docs: add use case diagram`, `feat(auth): add login`.
- Secrets are never committed; `.env.example` documents the expected variables.
