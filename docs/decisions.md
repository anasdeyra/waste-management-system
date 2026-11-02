# Decision log

This file records project decisions so that the analysis, UML models, prototype, Scrum
artifacts, metrics, and presentation remain consistent.

## D-001 — Project domain

- **Date:** 2026-10-31
- **Decision:** The project is the **Intelligent Waste Management System**.
- **Rationale:** Selected by the project owner as the subject of the ASE project.

## D-002 — Main development areas

- **Date:** 2026-10-31
- **Decision:** The functional analysis and prototype will be organized around five core
  areas: **collection points, vehicles, agents, tours, and waste**.
- **Rationale:** These are the development areas explicitly selected by the project owner.

## D-003 — Intelligent module and research dimension

- **Date:** 2026-10-31
- **Decision:** The intelligent module will perform **collection-tour optimization**. Its
  impact will be evaluated by comparing optimized tours with a documented baseline under
  identical operational constraints. Candidate impact measures include total distance,
  estimated collection duration, optimization execution time, and compliance with vehicle
  capacity and service constraints.
- **Rationale:** The project owner selected route optimization and defined the research
  dimension as integrating an intelligent module and measuring its impact on the system.
  The algorithm, baseline, routing model, datasets, and evaluation protocol are resolved
  in D-008–D-010 and D-015–D-016, as amended by D-019–D-022.

## D-004 — Technology stack

- **Date:** 2026-10-31
- **Decision:** The prototype will use **Java 21**, **Spring Boot 3**, **Maven** (with
  wrapper) for the build, and **H2 in file mode** as the relational database (D-013). JUnit 5
  is the test framework.
- **Rationale:** Java/Spring Boot gives a clean object-oriented domain model that matches
  the UML deliverables and has mature, free tooling for Part 6: PMD (cyclomatic complexity,
  code smells), CKJM (WMC, DIT, NOC, CBO, RFC, LCOM), and JDepend (coupling). A Maven
  wrapper keeps the build reproducible for a reviewer.
- **Amendment:** The presentation layer originally planned in this decision (Thymeleaf
  server-rendered views) was replaced by the React application described in **D-017**. The
  backend technology itself is unchanged.

## D-005 — Project mode

- **Date:** 2026-10-31
- **Decision:** The project is **individual** (one developer). Scrum artifacts describe a
  single-developer team.
- **Rationale:** Selected by the project owner.

## D-006 — Schedule

- **Date:** 2026-10-31
- **Decision:** There is **no fixed deadline**; work is organized in sprints without a
  fixed calendar end date. The presentation date will be recorded when known.
- **Rationale:** Selected by the project owner.

## D-007 — Prototype scope (extended by D-019–D-022)

- **Date:** 2026-10-31
- **Decision:** The prototype scope is **Extended**:
  - authentication and role-based authorization;
  - waste-category management;
  - CRUD and availability management for collection points, vehicles, and agents;
  - tour creation, consultation, and agent execution updates;
  - intelligent tour optimization and baseline comparison, with retained evaluation
    history;
  - multiple vehicles and multiple tours per planning day;
  - dashboards/charts;
  - evaluation-only CSV import/export (narrowed by D-028);
  - audit log.

  Explicitly excluded: connected fill-level sensors or other IoT hardware, real-time GPS
  tracking, external commercial mapping or routing services, a citizen-facing mobile
  application, and billing or payment management.
- **Rationale:** Selected by the project owner. The exclusions preserve the scope boundary
  already recorded in `docs/01-analysis.md` §8.
- **Resolution:** "Multiple vehicles" means a fleet with multiple single-vehicle tours per
  planning day; the optimizer plans one tour per run (D-012).

## D-008 — Optimization method and baseline (amended by D-022 and D-052)

- **Date:** 2026-10-31
- **Decision:** The evaluation compares three planning methods on identical scenarios:
  1. **Baseline:** collection points in input/selection order.
  2. **Intermediate:** capacity-aware nearest-neighbor construction.
  3. **Optimized:** nearest-neighbor construction followed by 2-opt improvement.

  The optimized method is the intelligent module; the baseline and intermediate methods
  make the source of any improvement visible.
- **Rationale:** All three methods are deterministic, explainable, and reproducible. The
  three-point comparison separates the gain from smarter construction from the gain from
  local search, which strengthens the Part 6 answer.

## D-009 — Routing and scenario model (superseded by D-021)

- **Date:** 2026-10-31
- **Decision:** An optimization scenario uses:
  - **Euclidean distance** on synthetic 2D coordinates (kilometres);
  - **one depot**;
  - **one vehicle per tour**, with a capacity check in kilograms;
  - **estimated duration** = travel distance / average speed + service time per stop.

  Average speed, service time, and capacity values are configuration parameters, not
  hard-coded constants. The default duration parameters are 40 km/h and 10 minutes per
  stop (D-015).
- **Rationale:** Euclidean distance is fully reproducible, needs no external service, and
  keeps the comparison focused on tour order and capacity rather than road-network data.

## D-010 — Evaluation datasets (amended by D-022)

- **Date:** 2026-10-31
- **Decision:** Evaluation uses **seeded synthetic scenarios** at 10, 25, 50, and 100
  collection points, with several demand profiles and random seeds, plus **one
  hand-crafted demo dataset** for the presentation. The matrix is fixed at 3 demand
  profiles × 5 seeds × 4 sizes = 60 scenarios (D-016); generator parameters will be
  recorded in `docs/06-metrics.md`.
- **Rationale:** Seeded generation makes every reported number reproducible and allows
  scaling; the hand-crafted dataset keeps the prototype demonstrable to a reviewer.

## D-011 — Agent assignment cardinality

- **Date:** 2026-10-31
- **Decision:** Each tour has **one driver** and **zero to two collectors** (D-014). Every
  assigned agent must be available; the driver and collector roles are distinct.
- **Rationale:** Selected by the project owner. This is more realistic than a single
  generic agent while remaining tractable for the prototype.

## D-012 — Fleet semantics

- **Date:** 2026-10-31
- **Decision:** The Extended scope supports **multiple single-vehicle tours per planning
  day**. Each tour has exactly one vehicle and one driver; the optimizer plans one tour per
  run. The prototype does **not** solve a simultaneous multi-vehicle CVRP.
- **Rationale:** Keeps the optimizer consistent with D-009 while still supporting a fleet
  and multiple tours per day. Resolves the open point in D-007.

## D-013 — Database engine

- **Date:** 2026-10-31
- **Decision:** The prototype uses **H2 in file mode** as its relational database.
- **Rationale:** Native Spring Boot support, no external service, a local file for
  persistence, and a web console for demonstration and inspection. Satisfies NFR-10.

## D-014 — Crew size

- **Date:** 2026-10-31
- **Decision:** A tour has **one driver and zero to two collectors** (maximum two
  collectors). Every assigned agent must be available.
- **Rationale:** A realistic small collection crew with bounded assignment validation.
  Resolves the open point in D-011.

## D-015 — Duration parameters (amended by D-021)

- **Date:** 2026-10-31
- **Decision:** The duration model defaults to **40 km/h average speed** and **10 minutes
  of service time per stop**. Both values remain configurable per scenario.
- **Rationale:** Typical urban collection assumptions; configurability keeps the
  evaluation transparent and reproducible.

## D-016 — Evaluation matrix (amended by D-022)

- **Date:** 2026-10-31
- **Decision:** The synthetic evaluation matrix is **60 scenarios**: 3 demand profiles ×
  5 seeds × 4 sizes (10, 25, 50, and 100 collection points). A hand-crafted demo dataset
  is evaluated separately. Each run records the seed, profile, size, method, distance,
  duration, execution time, and constraint checks.
- **Rationale:** Enough coverage for a clear comparison while keeping the experiment quick
  and reproducible. Resolves the open point in D-010.

## D-017 — Frontend technology stack

- **Date:** 2026-10-31
- **Decision:** The user interface of the prototype is a **React single-page application**
  built with **Vite**, **TypeScript**, **Tailwind CSS v4** and **shadcn/ui** (Radix-based
  components), located in `frontend/`. **Thymeleaf is no longer used.** The backend remains
  **Java 21 + Spring Boot 3**, but exposes a JSON REST API under `/api` instead of
  server-rendered pages.
- **Rationale:** Selected by the project owner. The component-based UI supports the
  dashboards, tables and charts of the Extended scope (D-007) without hand-written CSS, the
  typed API contract makes the boundary between the two parts explicit, and the two
  applications can be built, tested and measured independently — which also matches the
  modularity required by NFR-08.
- **Impact:** Supersedes the presentation layer of D-004. The backend technology, the
  database engine and the metric tooling are unchanged.

## D-018 — Repository layout and API contract

- **Date:** 2026-10-31
- **Decision:** The repository is split into two buildable applications:
  - `backend/` — Spring Boot REST API, Maven build, package `com.ase.wastems`;
  - `frontend/` — Vite + React application, npm build.

  The API is planned to be organised by resource (`/api/collection-points`, `/api/vehicles`,
  `/api/agents`, `/api/tours`, `/api/waste-categories`, `/api/optimization`,
  `/api/dashboard`, `/api/auth`). During development the Vite server proxies `/api` to
  `http://localhost:8080`, so the browser only ever talks to a single origin.
- **Rationale:** Two independent builds keep the frontend toolchain (Node, Vite) out of the
  Maven build and make the metrics of Part 6 attributable to each part. The proxy removes
  the need for CORS in development while the production build can point
  `VITE_API_BASE_URL` to any deployment.
- **Consequence:** The API contract will be mirrored by the TypeScript types in
  `frontend/src/types/api.ts`; both sides must be changed together.

## D-019 — Real-city case study and data provenance

- **Date:** 2026-10-31
- **Decision:** The case study is **New York City**, initially scoped to **Lower Manhattan
  (DSNY district MN01)**. Collection-point and depot coordinates come from the official DSNY
  Litter Basket Inventory and DSNY Garages datasets. The initial optimization scenarios use
  only records with a `DSNY ...` basket type; other ownership types retain their source label
  and are not treated as DSNY service obligations. The MN01G garage is the initial depot.
  Waste demands, fleet/crew records, and collection outcomes remain explicitly synthetic.
- **Rationale:** A source check on 2026-10-31 verified 449 geolocated MN01 basket records,
  including 238 DSNY-type records, plus the official MN01G garage location. NYC also publishes
  traffic-speed link geometry and a detailed street centerline. This is the strongest verified
  combination of waste, depot, road, and traffic data for the prototype.
- **Consequence:** The reproducible source extracts, queries, limitations, and attribution are
  documented in `docs/geospatial-data.md`; CSV snapshots are stored under
  `backend/src/main/resources/data/`.

## D-020 — Map-centric user experience

- **Date:** 2026-10-31
- **Decision:** Interactive maps are a core part of the prototype, not an optional chart. The
  React frontend will use **Leaflet/React Leaflet** with an attributed OSM-compatible basemap.
  Required workflows include collection-point display/filtering, popup inspection,
  click-to-set coordinates, map-based stop selection, depot and stop-order display,
  road-following tour geometry, baseline/optimized route comparison, execution status, and a
  timestamped traffic layer. Synchronized forms and lists remain available for precise editing
  and accessibility.
- **Rationale:** Selected by the project owner. Leaflet is lightweight, open source, and
  sufficient for the required markers, polylines, popups, and layer controls without a
  commercial mapping SDK.
- **Boundary:** Real-time vehicle GPS tracking is still excluded. No mapping or routing feature
  is marked implemented until it exists in the frontend/backend and has been run.

## D-021 — Real road routing and traffic boundary

- **Date:** 2026-10-31
- **Decision:** Tours use a **versioned OpenStreetMap road-network snapshot** and the local,
  open-source **GraphHopper** routing engine for road geometry, directed distance, and base
  travel time. Straight-line distances are not used for operational tours. NYC DOT traffic
  speeds are displayed as a timestamped overlay with visible coverage limitations; they do
  **not** modify optimization costs or trigger dynamic rerouting. Experimental comparisons use
  the same road graph and base travel-time matrix for every method.
- **Rationale:** The project owner selected real street routes plus a traffic overlay. A local
  Java-compatible engine preserves reproducibility and avoids a commercial routing service.
  Traffic sensors cover mostly major arterials and highways, so claiming citywide
  traffic-aware collection routing would be misleading.
- **Impact:** Supersedes the Euclidean operational-routing model in D-009 and the fixed
  40 km/h travel-time assumption in D-015. Commercial mapping/routing services, live vehicle
  tracking, and automatic traffic-aware rerouting remain excluded.

## D-022 — Real-location evaluation scenarios

- **Date:** 2026-10-31
- **Decision:** The **60-scenario matrix remains fixed** at 3 synthetic demand profiles × 5
  seeds × 4 sizes (10, 25, 50, and 100 points), but point locations are now sampled without
  replacement within each scenario from the eligible real MN01 DSNY basket records. Each
  scenario retains its source snapshot and road graph version. Distance and base travel time
  use a directed road-network matrix. A separate
  hand-crafted Lower Manhattan demo remains part of the presentation data.
- **Rationale:** Real coordinates and road geometry improve realism while seeded selection and
  synthetic demand preserve reproducibility and controlled capacity comparisons. Traffic is
  not varied within this experiment.
- **Impact:** Amends D-010 and D-016. For directed road costs, NN + 2-opt must evaluate the
  actual cost of reversed segments; the symmetric-TSP edge-delta shortcut is not valid.

## D-023 — Product Backlog granularity

- **Date:** 2026-10-31
- **Decision:** The Product Backlog uses **atomic user stories grouped by epic**. Each story has
  a priority, acceptance criteria, and traceability to the functional requirements.
- **Rationale:** Selected by the project owner. Atomic stories support sprint selection and
  provide a stable basis for deriving UML use cases without hiding several unrelated operations
  in one broad story.

## D-024 — Collection demand and mixed-category capacity

- **Date:** 2026-10-31
- **Decision:** A collection point can have **one or more demand entries**, each identifying one
  waste category and a quantity in kilograms. A tour may serve mixed waste categories; the sum
  of all selected demand quantities is checked against the single vehicle capacity. Separate
  vehicle compartments and category-specific capacities are not modeled.
- **Rationale:** Selected by the project owner. This represents mixed demand without expanding
  the prototype into compartmented-vehicle routing.

## D-025 — Tour lifecycle

- **Date:** 2026-10-31
- **Decision:** Tours use the lifecycle **DRAFT → PLANNED → IN_PROGRESS → COMPLETED**. A tour may
  move to **CANCELLED** before completion. Optimization proposals remain subject to planner
  review, and only a valid planned tour can be started.
- **Rationale:** Selected by the project owner. The lifecycle distinguishes editable planning,
  confirmed work, execution, and retained terminal history while keeping the model simple.

## D-026 — User administration and role hierarchy

- **Date:** 2026-10-31
- **Decision:** Administrators can create user accounts, assign roles, and activate/deactivate
  accounts; public self-registration is excluded. **ADMIN inherits all PLANNER capabilities** and
  additionally manages users and waste categories. **PLANNER** manages operational planning and
  optimization. **AGENT** accesses only assigned execution work.
- **Rationale:** Selected by the project owner. This fulfills the requirement that administrators
  manage user access while avoiding unnecessary role separation during a demonstration.

## D-027 — Collection-execution update authorization

- **Date:** 2026-10-31
- **Decision:** The assigned driver or either assigned collector may record progress, collected
  quantities, and incidents on their assigned tour. Planners and administrators may consult
  execution results and correct them through audited operations.
- **Rationale:** Selected by the project owner. Any present crew member can maintain the shared
  operational record, while privileged corrections remain traceable.

## D-028 — Evaluation-only CSV exchange

- **Date:** 2026-10-31
- **Decision:** CSV exchange is limited to **importing evaluation-scenario points and exporting
  evaluation results**. Export covers input-order baseline, nearest-neighbor, and NN + 2-opt
  results. General CSV import/export for users, vehicles, agents, categories, and operational
  tours is excluded.
- **Rationale:** Selected by the project owner. This keeps CSV support focused on reproducible
  research workflows rather than broad master-data migration.

## D-029 — Dashboard and audit scope

- **Date:** 2026-10-31
- **Decision:** Dashboards include **operational resource/tour summaries and retained evaluation
  comparisons**. The audit log records sign-in and data-change events with actor, UTC timestamp,
  action, entity type/identifier, outcome, and concise details. Field-level before/after diffs
  are excluded.
- **Rationale:** Selected by the project owner. This supplies useful operational and research
  visibility with a bounded, implementable audit model.

## D-030 — Agent-to-user account relationship

- **Date:** 2026-10-31
- **Decision:** An operational agent record has an **optional one-to-one link** to an `AGENT`
  user account. An agent can be managed without login access; only an agent with a linked,
  active account can authenticate and access assigned tours.
- **Rationale:** Selected by the project owner. This separates workforce records from credentials
  without duplicating agent identity or requiring every operational record to have login access.

## D-031 — Stop execution outcomes

- **Date:** 2026-10-31
- **Decision:** A tour stop begins as **PENDING** and finishes as either **COLLECTED** or
  **NOT_COLLECTED**. Collected stops record actual quantities by waste category; not-collected
  stops require an incident/reason. A tour can be completed only when every stop has a terminal
  outcome.
- **Rationale:** Selected by the project owner. Explicit terminal outcomes support reliable map
  display, collection history, and completion validation.

## D-032 — UML notation and first-pass behavioral coverage

- **Date:** 2026-11-01
- **Decision:** UML sources use **Mermaid inside Markdown**. The first pass contains the four
  required diagrams: a use-case diagram, a class diagram, one sequence diagram for
  optimize/review/accept, and one activity diagram for the tour lifecycle.
- **Rationale:** Selected by the project owner. Mermaid follows the repository convention and the
  four diagrams cover the assignment minimum without duplicating behavioral workflows.

## D-033 — Main class-diagram scope

- **Date:** 2026-11-01
- **Decision:** The main class diagram covers the **operational domain only**: users, agents,
  collection points and demand, categories, vehicles, depots, tours, stops, and collection
  results. Optimization/evaluation classes, controllers, DTOs, repositories, and UI classes are
  excluded from this diagram; optimization interactions appear in the sequence diagram.
- **Rationale:** Selected by the project owner. This keeps the main model focused on operational
  concepts rather than a premature implementation architecture.

## D-034 — Use-case organization

- **Date:** 2026-11-01
- **Decision:** The use-case model is a **grouped system overview**, not 39 isolated story bubbles.
  Each grouped use case has a short description and explicit traceability to its user stories and
  functional requirements. `ADMIN` inheritance of planner behavior is shown explicitly.
- **Rationale:** Selected by the project owner. Grouping preserves readability while retaining
  atomic-story traceability.

## D-035 — Sprint model tailored for AI-agent execution

- **Date:** 2026-11-01
- **Decision:** Development is organized in **ten scope-bounded sprints (S1–S10)** selected from
  the Product Backlog in dependency order, ending with a metrics/closure sprint. Sprints have no
  calendar dates (per D-006); each closes with a scripted review and a written retrospective.
  Sprint backlogs in `docs/05-scrum.md` §3 are written for an AI-agent executor: ordered
  self-contained tasks citing repo paths and governing documents, contract-sync rules, and
  machine-verifiable done checks. Two deliberate deviations from strict priority order:
  US-05/US-07 are pulled into S1 so deactivation semantics exist before demand validation, and
  US-33 is split — MN01 import groundwork in S3, full demonstration load in S7 once the tour
  lifecycle exists.
- **Rationale:** Requested by the project owner ("create the sprints tailored for an AI agent").
  Because the sprint executor has no memory between sessions, tasks must name where to work and
  cite fixed decisions instead of restating models; completion must be verifiable by commands,
  not impressions. The optimizer is split engine-first (S5) before its review UI (S6) so the
  isolated algorithm core is fully tested before dependencies accrue. No sprint is claimed
  executed until its review/retrospective notes are written.

## D-036 — Sprint execution pipeline and git workflow

- **Date:** 2026-11-01
- **Decision:** Each sprint is run by a manager agent: an Opus agent refines and plans the
  sprint backlog (`docs/05-scrum.md` §3), then a Sonnet agent executes it, and the manager
  verifies the done checks independently before moving on. Each sprint is developed on branch
  `sprint/sN` with one conventional commit per task and merged into `main` with `--no-ff` once it
  is verified. No remote is configured and nothing is pushed. Untracked owner research notes
  (`research*.txt`) are left untouched. If done checks fail, up to three Sonnet fix rounds are
  run. If checks still fail, the scope is narrowed and the gap is recorded honestly in the sprint
  review and in this log, without pausing the pipeline.
- **Rationale:** Requested by the project owner, who wants fully autonomous completion with
  every decision collected beforehand.

## D-037 — Routing integration: Overpass extract and embedded GraphHopper

- **Date:** 2026-11-01
- **Decision:** The versioned road-network snapshot is an OSM XML extract of an MN01 bounding
  box plus a margin, retrieved with `curl` from the Overpass API. The query, retrieval date and
  SHA-256 checksum are documented in `docs/geospatial-data.md`, and the checksum forms the routing
  version ID. GraphHopper is embedded as a Maven library inside the Spring Boot backend, behind the
  `routing/` package. It does not run as a separate server. The optimization core stays free of
  GraphHopper dependencies (AGENTS.md §7).
- **Rationale:** This needs no new extraction tooling, keeps the file small, and makes the
  extract scriptable and reproducible. Running everything in one process simplifies the demo and
  the tests.

## D-038 — Authentication mechanism

- **Date:** 2026-11-01
- **Decision:** Spring Security JSON login with a server-side `HttpSession` cookie and CSRF
  protection for the same-origin SPA served through the Vite proxy. Logout invalidates the
  session on the server. JWT is not used.
- **Rationale:** Simplest secure option for a same-origin SPA. Deactivation and sign-out take
  effect immediately (US-02, US-05).

## D-039 — Basemap tiles

- **Date:** 2026-11-01
- **Decision:** The Leaflet maps use the standard OSM tiles (`tile.openstreetmap.org`) with
  visible "© OpenStreetMap contributors" attribution, at low-volume dev/demo usage that follows
  the OSM tile usage policy.
- **Rationale:** Free, needs no key, and is consistent with the ODbL attribution already
  required (D-020).
- **Superseded by D-054** (2026-11-02): the maps now use a custom minimal vector style.

## D-040 — Frontend verification level

- **Date:** 2026-11-01
- **Decision:** No frontend test framework is added. Frontend correctness is verified by the
  strict TypeScript build (`npm run build`), the backend test suite, and scripted HTTP checks
  against the running apps. Presentation screenshots in S10 are captured with tools already on
  the machine (for example headless Firefox), and no browser-automation dependency is added.
- **Rationale:** Chosen by the project owner. This keeps the dependency footprint small.

## D-041 — Presentation format

- **Date:** 2026-11-01
- **Decision:** The final deck is a self-contained HTML slide deck at
  `presentation/index.html`, versioned in git and opened in any browser. No presentation date is
  fixed. The deck is produced in S10.
- **Rationale:** Chosen by the project owner.

## D-042 — S1 implementation conventions

- **Date:** 2026-11-01
- **Decision:**
  - API status convention: 200 read/update, 201 create with `Location`, 204 logout; 400
    validation, malformed JSON, unknown enum, bad UUID; 401 unauthenticated, bad credentials or
    deactivated account; 403 role denial or CSRF failure; 404 unknown id; 409 duplicate or state
    conflict (including self-deactivation); 422 domain-rule violation (for example an inactive
    category). Every error body is `ApiError{status, error, message, fieldErrors?}`.
  - CSRF: cookie `XSRF-TOKEN`, header `X-XSRF-TOKEN`, plain (non-XOR) token handler, token
    delivered by `GET /api/auth/csrf`. The cookie is cleared on logout, so clients fetch a new
    token afterwards.
  - Usernames are trimmed and lower-cased before storage and lookup.
  - Every authenticated request re-checks that the account still exists and is active
    (`ActiveAccountFilter`), so deactivation is immediate.
  - `dev` is the default Spring profile and seeds `admin`, `planner` and `agent` development
    accounts; tests run with the `test` profile and an in-memory database.
  - An administrator cannot deactivate their own account. No user or category delete endpoint
    exists; only deactivation.
  - `PUT /api/waste-categories/{id}` replaces name and description (an omitted description
    clears it).
- **Deviations from the S1 plan:** the CSRF endpoint returns the constant header name
  `X-XSRF-TOKEN` instead of `CsrfToken.getHeaderName()` (MockMvc test infrastructure could swap
  the repository); the real cookie flow is tested in its own `@DirtiesContext` class.
- **Rationale:** Conventional choices made during S1 so later sprints reuse them.

## D-043 — S2 implementation conventions

- **Date:** 2026-11-01
- **Decision:**
  - Service area: collection-point coordinates must lie in an NYC bounding box (longitude
    -74.30…-73.65, latitude 40.45…40.95, inclusive, configurable as `wastems.service-area`).
    Values outside it return 422 with a hint that longitude and latitude may be swapped; values
    outside the WGS84 ranges (longitude ±180, latitude ±90) stay 400 bean-validation errors.
  - Every collection point keeps at least one demand entry (OCL `HasCurrentDemand`; removing the
    last entry is 422) and has at most one entry per waste category (409). Only a newly assigned
    category must be active; an existing entry may keep an inactive category when its quantity is
    edited.
  - `/api/collection-points` and `/api/depots` are PLANNER/ADMIN only; AGENT receives 403 and
    anonymous 401. Depots are read-only; the MN01G depot is seeded in `dev`.
  - Status change is `PATCH /api/collection-points/{id}/status` with `{"status":"ACTIVE|INACTIVE"}`
    (an enum mirroring the class diagram, unlike the boolean `active` of users and categories).
  - The `dev` map seed (`DevMapDataSeeder`: 2 demo categories, the MN01G depot and 5 DSNY baskets
    keyed by `8znf-7b2c` + basket id, real locations with synthetic demand, one inactive) is
    idempotent and will be absorbed by the S3 importer by upserting on the source key.
  - Imported point names use the format `STREET1 & STREET2` (`streetname1 & streetname2`).
  - `vite.config.ts` accepts `API_PROXY_TARGET` (default `http://localhost:8080`) so a second
    frontend can proxy to a scratch backend without touching the owner's servers.
  - Leaflet is drawn with `CircleMarker`s only (no default marker images); the lat/lng order flip
    happens only in `components/maps/geo.ts` (`toLatLng`).
- **Deviations from the S2 plan:**
  - `CollectionPointService.addDemand` calls `flush()` instead of `saveAndFlush(point)`: on a
    managed point `save` merges a copy of the new demand, so the returned entity had no id.
  - `WasteCategoryServiceTest` also gained `points.deleteAll()` before `deleteAll()` of
    categories (the plan named only the controller test); the new `DepotControllerTest` saves its
    users because the active-account filter requires them to exist.
  - `isValidKg` lives in `src/lib/validation.ts` and the form uses `useWatch` instead of `watch`,
    to keep own code free of lint warnings. The shadcn `Tooltip` was not used for the disabled
    "remove last entry" hint (no provider mounted); a `title` attribute is used instead.
  - The plan's trailer name `Claude Opus 5.5` was replaced by the one given in the executor
    instructions, `Claude Sonnet 5.5`.
- **Rationale:** Conventional choices made during S2 so later sprints (importer, tour drafts,
  evaluation scenarios) reuse them.

## D-044 — S3 implementation conventions

- **Date:** 2026-11-01
- **Decision:**
  - `Vehicle.registration` is trimmed, whitespace-collapsed and upper-cased, unique (409), at most
    20 characters. Capacity is positive with at most 2 decimals (400); a service-level backstop
    throws 422 (OCL `PositiveCapacity`) and is unit-tested only.
  - Agent display names are not unique; the UUID is the identity. Vehicles and agents have no
    `DELETE` (405); they are made unavailable instead.
  - Availability changes use `PATCH /{id}/status {"status":"AVAILABLE|UNAVAILABLE"}`;
    `?status=` filters the lists. `VehicleService.findUsable()` and `AgentService.findUsable()`
    are the usable-resource queries for S4.
  - Account link: the foreign key is `operational_agent.user_account_id` (unique).
    `PUT`/`DELETE /api/agents/{id}/user-account` is ADMIN only (rule placed above the
    PLANNER rule in `SecurityConfig`). Rule order: 404 (agent, then user), 422 (non-AGENT
    account), idempotent for the same pair, 409 for either side already linked. Unlink is
    idempotent (204) and deletes nothing. Activation is independent of the link. The optional
    `agentId` on user creation is atomic (any failure rolls the user back).
    `UserResponse.linkedAgentId/linkedAgentName` and `CurrentUserResponse.agentId` expose it.
  - The MN01 import upserts on dataset + id and refreshes only `sourceOwnershipType`; the
    name, coordinates, status and demand of an existing point are kept.
  - Synthetic demand is the category `Mixed litter` with `10 + floorMod(hash(basketId), 21)` kg,
    origin `SYNTHETIC`. An existing (even inactive) `Mixed litter` category is reused, with a
    warning when inactive.
  - Name rule: `STREET1 & STREET2`, otherwise the non-blank street, otherwise `Basket <id>`
    (at most 120 characters).
  - Skip rules: blank id, blank type, duplicate id in the file, unparsable or out-of-area
    coordinates; each skipped row is a warning (at most 20, then `... more warnings omitted`).
    `dsnyEligibleRowCount` counts every row with a `DSNY ` type, skipped or not.
  - Snapshot id `dataset:rows:sha12` (SHA-256 over the classpath bytes) is stored in
    `source_snapshot`, a provenance record outside the D-033 class diagram; `importedAt`
    changes only when the checksum changes.
  - The import runs at `dev` start (`@Order` 10 accounts, 20 map sample, 30 import) and through
    `POST /api/admin/import` (ADMIN). The last report lives in memory;
    `GET /api/admin/import/report` returns 404 before any run.
  - `optimizationEligible` = ACTIVE and (no label or `DSNY ` label), exposed in
    `CollectionPointResponse` and filterable with `?eligible=true|false`;
    `findEligibleForPlanning()` applies it.
- **Deviations from the S3 plan:**
  - None in behaviour; the done-check script ran unmodified (138 checks, `FAILS=0`). The
    `ApiExceptionHandler` needed no 405 handler (Spring's default already returned 405).
  - On the Users page the 409 branch maps to the username field only when the message
    mentions the username (link conflicts show a toast), to keep the existing mapping while
    supporting the new 409 on the optional agent link.
  - `useWatch` replaced `watch` for the role field (lint, same as S2). Mn01 helper types
    (`Warnings`, `SourceFile`) are private nested types of `Mn01ImportService`.
  - The trailer name `Claude Opus 5.5` of the plan was replaced by `Claude Sonnet 5.5` given
    in the executor instructions.
- **Rationale:** Conventions fixed during S3 so tour drafts (S4), the optimizer scenarios (S5)
  and the S7 demonstration reuse the resource, link and import semantics.

## D-045 — S4 implementation conventions

- **Date:** 2026-11-01
- **Decision:**
  - Entities follow the class diagram: `Tour`, `TourStop`, `StopDemand`, with `TourStatus`
    (`DRAFT`, `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`) and `TourStopStatus`
    (`PENDING`, `COLLECTED`, `NOT_COLLECTED`). S4 creates only `DRAFT` tours and `PENDING`
    stops. `cancellationReason`, `roadLegsGeoJson`, `roadGraphVersion` and the stop outcome
    fields are added by S6-S8 together with their behaviour. `Tour.revision` was added to the
    class diagram.
  - Stop order is `visitOrder` 1..n, contiguous and unique, maintained by the domain (append on
    add, renumber on remove, permutation on reorder). There is no database unique constraint on
    `visit_order` (it would break reorders); `(tour_id, collection_point_id)` is unique.
  - Adding a stop snapshots the point's demand entries into `StopDemand`; name, coordinates and
    status are read live (S5 freezes coordinates in the evaluation scenario).
  - New stops must be `optimizationEligible`: inactive 422, active non-DSNY 422, duplicate 409.
    A stop whose point later becomes inactive or ineligible is kept and reported
    (`INACTIVE_STOP`, `INELIGIBLE_STOP`).
  - `PUT /api/tours/{id}/assignment` replaces vehicle, driver and collectors together. Rule
    order: 404 tour, 409 not draft, 422 more than two / duplicate collectors / driver as
    collector, 404 unknown vehicle or agent, 422 newly placed unavailable resource. A resource
    already on the tour that became unavailable may stay (history) and is reported.
  - Over-capacity (sum of all planned categories against the single vehicle capacity, equality
    allowed) is reported, not rejected, in a draft; it is the blocking `OVER_CAPACITY` issue for
    S6 confirmation.
  - `Tour.structuralIssues()` returns, in this order: `NO_STOPS`, `NO_VEHICLE`, `NO_DRIVER`,
    `VEHICLE_UNAVAILABLE`, `DRIVER_UNAVAILABLE`, `COLLECTOR_UNAVAILABLE` (per collector),
    `INACTIVE_STOP`/`INELIGIBLE_STOP` (per stop), `OVER_CAPACITY`. `structurallyValid` means no
    issue; S5 and S6 reuse it, S6 adds the route checks.
  - The planning date must be today or later on create and whenever it is changed; an unchanged
    past date may be kept on edit.
  - `Tour.revision` (starts 0) is incremented by every successful mutating request and exposed in
    responses, so S5 can record the draft version it evaluated and S6 can refuse a stale proposal.
  - No cross-tour conflict check (D-012): the same vehicle or agent may serve several tours on
    one day. No `DELETE /api/tours/{id}` (405); abandonment is the S8 cancellation.
  - Statuses: `POST /api/tours` 201 with `Location`; `POST .../stops` 201 with the stop
    `Location` and the whole tour as body; `DELETE .../stops/{stopId}` answers 200 with the tour
    (not 204) because the remaining stops are renumbered and the client needs the new order.
  - `/api/tours/**` is PLANNER/ADMIN (`AGENT` 403, anonymous 401); S7 adds `GET /api/tours/mine`
    above this rule.
  - The map shows the input order as straight dashed lines, labelled everywhere "Input order
    (straight lines, not a route)"; no distance or routing call exists in S4 (S5).
- **Deviations from the S4 plan:**
  - None in behaviour; the done-check script ran unmodified (113 checks, `FAILS=0`), with no
    path fixes needed.
  - The trailer name of the plan was replaced by `Claude Sonnet 5.5` given in the executor
    instructions.
  - `nonDraftTourIsLocked` takes the stop id from the persisted instance returned by the first
    `save()`; reading it after the second (merging) `save()` raised a lazy-initialization error.
  - The tour status labels live in `frontend/src/lib/tour-labels.ts` (not in the page file)
    because oxlint `only-export-components` flags constants exported from a page; the create
    dialog uses `useWatch` instead of `watch`.
  - The tour header form and the assignment panel are both remounted with
    `key={tourId-revision}`; the planner caption also names the layer "Input order (straight
    lines, not a route)"; permanent stop-number tooltips are styled by the CSS class
    `tour-stop-number` in `src/index.css`.
  - Tour mutation hooks refresh the detail with `setQueryData` and invalidate only the list
    queries (`['tours','list']`); `useCreateTour` invalidates all tour queries.
- **Rationale:** Conventions fixed during S4 so the S5 evaluation freezes a tour draft by its
  revision, S6 confirms it with the same structural validation, and S7/S8 add execution and
  cancellation on the same entities.

## D-046 — S5 implementation conventions

- **Date:** 2026-11-01
- **Decision:**
  - **Snapshot.** The MN01 road extract is committed as
    `backend/src/main/resources/routing/mn01-roads.osm` (4 148 737 bytes, SHA-256
    `e1f2fabaa937...341d6a01`) with its Overpass query `mn01-roads.overpassql`; `tools/fetch-road-snapshot.sh`
    refreshes it. A refresh produces new bytes and a new version: the file and
    `wastems.routing.expected-sha256` are replaced together and retained runs keep their version ID.
  - **Version ID.** `roadGraphVersion` = `wastems.routing.version-prefix` + the first 12 hex of the
    SHA-256 of the classpath extract = `osm-mn01-e1f2fabaa937`.
  - **GraphHopper.** `graphhopper-core` 11.1 embedded behind `routing/` only; profile `car` with the
    bundled `car.json` custom model, `TurnCostsConfig.car()`, contraction hierarchies, OSM turn
    restrictions. `jackson-dataformat-xml` is excluded so Spring MVC registers no XML converter.
  - **Cache and start-up.** The graph cache lives in `wastems.routing.cache-dir` (default
    `routing-cache`, git-ignored) in `<versionId>-gh11.1/`; initialization is lazy and thread-safe
    (failures are not cached) plus a `dev` warm-up that only logs a warning (NFR-10). The router
    refuses a missing file, a checksum mismatch or a failed import with `RoutingUnavailableException`.
  - **Snapping and legs.** Every node must snap to a drivable road within 100 m before any leg is
    routed (`POINT_NOT_ROUTABLE`); one GraphHopper request per ordered pair (`NO_ROAD_PATH` on
    error); distance and base time are rounded to 3 decimals before optimization so the retained
    matrix reproduces every indicator. GraphHopper's paths minimize its car weighting while the
    optimizer minimizes the sum of the directed leg distances (documented in `docs/geospatial-data.md` §4).
  - **Independence rule.** `optimization/` imports only `java.*` and `com.ase.wastems.optimization.*`,
    enforced by `OptimizationIndependenceTest`; `routing/` may import `optimization/`, never the reverse.
  - **Methods.** `INPUT_ORDER`; capacity-aware `NEAREST_NEIGHBOR` (strict `<`, ties to the lowest input
    index, capacity filter with 1e-9 tolerance); `NN_PLUS_2_OPT` with best-improvement 2-opt on the exact
    directed delta (reversed internal legs re-costed from prefix sums, epsilon 1e-6 m, first best move
    wins). One trip from and back to the depot, no reload; capacity and coverage validated identically.
  - **Duration and timing.** `durationS` = sum of directed base travel times + n x
    `wastems.optimization.service-time-seconds` (600 s, D-015); the 40 km/h speed of D-015 is superseded
    by GraphHopper base time (D-021). Execution time is `System.nanoTime()` around each method's
    construction/improvement (NN + 2-opt includes its NN), not routing.
  - **Scenario freezing.** `TourService.loadDraftAndBuildScenario`: DRAFT only (409); structural issues
    give 422 and retain nothing; it freezes tour id and revision, depot, each stop's point id, source
    ids, name, coordinates and planned demand total, vehicle capacity and source snapshot IDs
    (`<dataset>:unknown` when no snapshot row exists, `USER_ENTERED` when a depot or point has no
    source dataset). It never writes the tour.
  - **Retention.** `EvaluationRun` (+ `EvaluationStop`, `MethodResult` element collections) without a
    foreign key to `tour`; status `COMPLETED` or `ROUTING_FAILED`.
  - **API and statuses.** `POST /api/tours/{id}/evaluations` (201 + `Location`; `ROUTING_FAILED` is also
    201), `GET /api/tours/{id}/evaluations`, `GET /api/evaluations/{runId}`, `GET .../matrix`; 422 not
    ready, 409 not a draft, 404 unknown, 400 malformed id, **503** when the road snapshot is unusable
    (addition to D-042). `/api/evaluations` replaces the planned `/api/optimization` of D-018.
- **Rationale:** One fixed road version and one fixed frozen scenario make the three methods
  comparable and every retained indicator recomputable (NFR-05, NFR-07, NFR-09); failures are retained
  or explained instead of silently altering the operational draft.
- **Deviations from the S5 plan:**
  - The trailer name is `Claude Sonnet 5.5` as given in the executor instructions.
  - The plan's `EvaluationServiceTest` item 1 says matrix `nodeCount` 4 for a fixture with four stops
    and a depot; the matrix has 5 nodes and the test asserts 5 (the API test with three stops asserts 4).
  - Done-check script corrections (three assertions that were wrong in the script, not in the code; the
    original is kept next to it as `s5-done-check.orig.sh`): the anonymous POST now carries a CSRF token
    (without one Spring's CSRF filter answers 403 before authentication); the frozen depot check accepts
    the real imported name `MN01G Garage, 353 Spring St` (prefix `MN01G` and source identifier `MN01G`);
    the second backend's warm-up warning is polled for up to 40 s instead of grepped once (it is logged
    after the login check). First run: `FAILS=3`; second run with these corrections: 116 checks, `FAILS=0`.
  - No GraphHopper API deviated from the plan; the code compiled against 11.1 as specified.

## D-047 — S6 implementation conventions

- **Date:** 2026-11-01
- **Decision:**
  - **Route reference on `Tour`.** Nullable columns `road_legs_geojson` (lob, copied byte for byte from
    `MethodResult.legsGeoJson`), `road_graph_version`, `route_evaluation_run_id` (no foreign key, like
    `evaluation_run.tour_id`), `route_method`, `route_distance_m`, `route_duration_s`; either all are set
    (`hasRoute()`) or all are null. They are set only by accepting the NN + 2-opt proposal or by keeping the
    input order (the `INPUT_ORDER` legs of the same run). `addStop`, `removeStop`, `reorderStops` and
    `setDepot` with a different depot clear the route in the domain; `assign(...)` and `setPlanningDate(...)`
    keep it (crew and date do not change the geometry; capacity is rechecked at confirmation). Accept
    reorders first, then attaches.
  - **Fresh versus stale.** A run is fresh for a tour iff its tour id and revision equal the tour's and the
    frozen stop point ids in input order equal the tour's current stop point ids in visit order; otherwise
    accept and reject answer 409 and change nothing (D-045 revision rule).
  - **Accept and reject.** `POST /api/tours/{id}/proposal-acceptances` and `.../proposal-rejections`, body
    `{"evaluationRunId": uuid}`, both 200 with the `TourResponse`; the tour stays `DRAFT` and `revision + 1`.
    Accept applies the `NN_PLUS_2_OPT` point order and route; reject leaves the order unchanged and attaches
    the `INPUT_ORDER` route, because confirmation needs a route (OCL `Tour::confirm()`
    `VersionedRoadRoute`, activity diagram "Keep current DRAFT order"). Rule order, first failing wins: 400
    missing or malformed body; 404 tour; 404 run; 422 run of another tour; 409 tour not `DRAFT`; 422 run
    `ROUTING_FAILED`; 409 run already `ACCEPTED`; 409 stale; 422 the needed method result is missing or
    infeasible.
  - **Decision on the run.** `EvaluationRun` gets `decision` (`ProposalDecision`), `decided_by`,
    `decided_at`, written in the same transaction as the tour change. Method results are never altered, so a
    rejected proposal stays retained (US-24).
  - **Confirmation.** `POST /api/tours/{id}/confirmation`, no body, 200 with `PLANNED` and `revision + 1`;
    404 unknown tour, 400 malformed id, 409 not `DRAFT`, 422 with `ApiError.issues` (`{code, message}`) when
    any issue exists, in which case nothing changes. Issue order: the S4 structural issues, then
    `PLANNING_DATE_PAST` (date before today), then `ROUTE_MISSING` or `ROUTE_OUTDATED`. `ROUTE_OUTDATED`
    (service level) means the route's run no longer exists or a current stop point's or the depot's
    coordinates differ (> 1e-9) from those frozen in that run. `Tour.structuralIssues()` is unchanged, so the
    S5 evaluation gate is unaffected.
  - **Response fields.** `TourResponse` adds `route` (omitted when none), `confirmationIssues` (empty when the
    tour is not `DRAFT`) and `readyToConfirm`; `TourSummaryResponse` adds `routeMethod`; the run responses
    add `decision`, `decidedBy`, `decidedAt`; `ApiError` adds `issues` (omitted when null). Every
    `TourResponse` is built through `TourController.respond(...)`.
  - **Concurrency.** Accept, reject and confirm read the tour with `PESSIMISTIC_WRITE`
    (`TourRepository.findByIdForUpdate`).
  - **Lock.** Unchanged S4 rule: every mutation calls `requireDraft` (409 for `PLANNED`); accept, reject and
    confirm join that set. There is no un-confirm (D-025; cancellation is S8).
  - **No audit until S8.** The audit events of accept, reject and confirm belong to US-39.
  - **Activity diagram.** The edge `FEASIBLE -- No --> KEEP` became `FEASIBLE -- No --> FIX`: without a
    feasible proposal there is no feasible input-order route either (capacity is checked identically for all
    methods and an over-capacity draft never reaches the optimizer), so the planner corrects the draft and
    evaluates again.
  - **Mermaid render procedure.** `@mermaid-js/mermaid-cli` 11 with Chrome is installed in the session
    scratchpad, never in the repo: `PUPPETEER_CACHE_DIR=<scratch>/mermaid/.cache
    <scratch>/mermaid/node_modules/.bin/mmdc -i docs/03-uml/<file>.md -o <scratch>/mermaid/out/<file>.svg`;
    exit 0, the SVG written and no "syntax error" in it.
  - **Frontend.** Traffic is not shown and not an optimizer input (D-021). The `[lon, lat]` to `[lat, lng]`
    flip stays in `components/maps/geo.ts`. The UI is verified by build, lint, bundle strings and proxy
    checks only (D-040).
- **Rationale:** Making acceptance, keeping the input order and confirmation explicit, revision-checked and
  atomic keeps the algorithm from silently changing operational work (US-24), and a single confirmation gate
  with structured issues makes the `PLANNED` state trustworthy (US-25, NFR-02).
- **Deviations from the S6 plan:**
  - The trailer name is `Claude Sonnet 5.5` as given in the executor instructions.
  - The plan's closing `end` of the sequence-diagram block was kept by the scripted replacement, leaving one
    extra `end` that failed to parse; it was removed before the docs commit and the diagram renders.
  - The plan says the done-check has about 120 checks; the script prints 173 `PASS` assertions. It ran
    unmodified (no `s6-done-check.orig.sh`) and ended with `FAILS=0` on the first run.
  - The tests copy the evaluation fixture, as planned. The confirmation test builds its extra basket with the
    category found through `categories.findAll()`. No API of the plan needed correction.

## D-048 — S7 implementation conventions

- **Date:** 2026-11-01
- **Decision:**
  - **Crew and visibility.** The crew of a tour is its driver and its collectors. A caller is a crew
    member iff its account (by username) is linked to the driver or one collector. An agent sees a tour
    iff it is crew and the tour is not `DRAFT`. Unknown tour: 404. Existing but not visible: 403 "You are
    not assigned to this tour."; an AGENT account without a linked agent: 403 "Your account is not linked
    to an operational agent." and an empty `/mine` list. A deactivated account gets 401 (unchanged).
    Execution endpoints are AGENT only in `SecurityConfig`; ADMIN inherits PLANNER, not AGENT, so
    planners and admins get 403 (planner corrections are S8). Agents read point names and coordinates only
    through their tours; `/api/collection-points` stays 403.
  - **Endpoints** (`TourExecutionController`): `GET /api/tours/mine[?status=]` (in-progress first, then
    planned, completed, cancelled; then planning date; then creation time), `GET /api/tours/mine/{id}`,
    `POST /api/tours/{id}/start`, `POST /api/tours/{id}/stops/{stopId}/outcome`,
    `POST /api/tours/{id}/completion`; all answer 200 with the tour. The literal `/mine` mapping wins over
    `/{id}`.
  - **Rule orders.** Start: 400, 404, 403, 409 (not `PLANNED`), 422 with every start issue, 200. Outcome:
    400 (ids, body, bean and cross-field checks), 404 tour, 403, 404 stop, 409 tour not `IN_PROGRESS`, 409
    stop not `PENDING`, 404 category, 422 duplicate category, 422 inactive category that is not planned on
    the stop, 200. Completion: 400, 404, 403, 409 (not `IN_PROGRESS`), 422 one `STOP_PENDING` per pending
    stop, 200.
  - **Start issues.** The structural issues of S4, then `ROUTE_MISSING` or `ROUTE_OUTDATED` (S6
    semantics), empty unless `PLANNED`. There is no date check: a planned tour can start on any day, so a
    reviewer can run the demo days later; `PLANNING_DATE_PAST` stays a confirmation issue. **No recheck
    after the start** (availability, point status and coordinates are not rechecked while `IN_PROGRESS` or
    at completion): history must stay as executed.
  - **Outcome body** `{status, quantities[{categoryId, actualQuantityKg}], incidentReason}`. Bean
    validation: status required; per quantity category and a non-negative amount with at most 8 + 2 digits;
    reason at most 500 characters. Cross-field 400s (`InvalidRequestException`, same body as bean
    validation): `PENDING` status, collected without quantities, not collected without a reason, not
    collected with quantities. A collected stop may carry an optional incident note; the reason is trimmed.
    Categories need not be planned on the stop (an agent may find other waste) but must be active unless
    planned.
  - **Concurrency.** Start, outcome and completion read the tour with `PESSIMISTIC_WRITE`
    (`findByIdForUpdate`).
  - **Responses.** `TourResponse` adds `startedAt`, `startedBy`, `completedAt`, `completedBy`,
    `startIssues`, `readyToStart`, `readyToComplete`; `TourStopResponse` adds `incidentReason`,
    `outcomeRecordedAt`, `outcomeRecordedBy`, `actualQuantities`, `actualTotalKg`; `TourSummaryResponse`
    adds `origin` and `terminalStopCount`. `TourIssuesException` is the common 422-with-issues exception
    (confirm, start, completion) and `ForbiddenException` maps to 403.
  - **Domain.** `Tour` gains started/completed time and user, `start`, `complete`, `startIssues`,
    `completionIssues`, `isCrewMember`; `TourStop` gains `incidentReason`, `outcomeRecordedAt`,
    `outcomeRecordedBy`, `actualQuantities`, `recordCollected`, `recordNotCollected`; new entity
    `CollectedQuantity` (unique per stop and category); new issue code `STOP_PENDING`. All columns are
    nullable or new tables, so `ddl-auto: update` upgrades an existing database in place (verified on a
    database created by the S6 jar and on a copy of the owner's database file).
  - **Demo load (US-33).** Triggered by a `dev` start-up runner (`DemoStartupLoader`, order 50, after
    accounts, map sample, MN01 import and routing warm-up) and by `POST /api/admin/demo` (ADMIN;
    `GET /api/admin/demo/report`, 404 before any load), gated by `wastems.demo.enabled` (false in
    `application.yml`, true in `application-dev.yml`, `--wastems.demo.enabled=false` skips it, POST then
    answers 409). The runner never fails start-up. `DemoRecord` (outside the D-033 class diagram) gives
    every demo item a key: a key with a record is never created again, even if a user removed the entity;
    without a record the item is adopted by its natural key (never modified) or created. Existing entities
    are never modified by a later load. Each demo tour advances forward only, one guarded step at a time
    (evaluate, accept the NN + 2-opt proposal, confirm, start, scripted outcomes, complete) and never
    touches a tour at or beyond its target; a failing step adds a warning and stops that tour only.
    Demo tours are dated the day of the first load; start has no date check. Everything created is
    `SYNTHETIC` (agent names end with "(demo)", outcome reasons with "(synthetic demo outcome)",
    categories with "(synthetic demo category)"). Demo accounts: `agent`, `collector`, `agent2`.
  - **No audit until S8.** The audit events of start, outcomes, completion and the demo load belong to
    US-39.
  - **Frontend.** Pages `/my-tours` and `/my-tours/:id` (AGENT only), planner-side stop states and
    Execution card, and a "Demonstration data" card on the Data import page. Verified by build, lint,
    bundle strings and proxy checks only (D-040).
- **Rationale:** Server-side crew scoping and a revalidation at start make the execution loop trustworthy
  (US-03, US-14, US-16, US-17, US-26 to US-29), freezing the record after completion keeps history as
  executed, and an identity-based, forward-only demo load lets a reviewer try every lifecycle state without
  duplicates or lost progress (US-33, NFR-10, NFR-12).
- **Deviations from the S7 plan:**
  - The trailer name is `Claude Sonnet 5.5` as given in the executor instructions.
  - The forward-only guard of the demo loader also lets DRAFT and IN_PROGRESS targets run their own
    guarded step when the tour is already at its target: the plan's "never touches a tour at its target"
    would have left the draft demo tour without its evaluation and prevented retrying the scripted
    outcomes. A tour beyond its target is never touched.
  - `TourExecutionService` takes `TourService` and `WasteCategoryRepository` only; the demo service
    reaches the other repositories directly. The service constants and the second inactive-category
    warning (once per load, for adopted and recorded categories) are small additions.
  - The done-check ran unmodified (no `s7-done-check.orig.sh`): 191 `PASS` lines, `FAILS=0` on the first
    run. No plan API needed correction.


## D-049 — S8 implementation conventions

- **Date:** 2026-11-01
- **Decision:**
  - **Roles.** Cancel (US-30), correct (US-31) and history (US-32): PLANNER and ADMIN (D-027); agents
    keep `/api/tours/mine`. The audit log (US-39, US-03) is ADMIN only. AGENT is refused all four with
    403, anonymous gets 401. `SecurityConfig` has no new URL rule (the new tour endpoints fall under the
    `/api/tours/**` PLANNER rule, `/api/admin/**` is ADMIN); it only gains a logout handler.
  - **Audit model.** `AuditEvent` (table `audit_event`): UUID, `occurred_at` (UTC, milliseconds), `actor`
    (at most 50), `action`, `entity_type`, `entity_id` (at most 80), `outcome` (`SUCCESS` or `FAILURE`),
    `details` (at most 500). `@Immutable`, no setters, outside the D-033 class diagram. Immutability:
    `AuditEventRepository` extends Spring Data `Repository` (not `CrudRepository`) and declares only
    `save`, `findById`, `count`, `findAll` and the paged `search`; the API has two GETs, so PUT, PATCH,
    DELETE and POST answer 405 for everyone.
  - **Actor.** `AuditService.currentActor()` is the authenticated, non-anonymous principal name, else
    `system` (start-up runners). Sign-in events use the attempted username (trimmed, lower-cased, cut to
    50). The actor is a string, so deactivating or unlinking an account keeps its rows (US-05).
  - **Hook point.** One service-level component, `AuditService`, called by the mutating service methods
    right after the change succeeded and inside the same transaction: a refused request rolls back and
    leaves no event. `record*` never throws (it truncates instead of validating), because an exception
    would mark the surrounding transaction rollback-only. Exceptions: sign-in is recorded by
    `AuthController` (failure in a `catch` that rethrows), sign-out by a `LogoutHandler`,
    `EvaluationService.evaluate` inside the transaction that saves the run, `Mn01ImportService.importAll`
    and `DemoDataService.load` one summary event per run. Only `AuthController` and `AuditEventController`
    use `AuditService` in `web/`.
  - **Outcomes and secrets.** `FAILURE` rows exist only for failed sign-ins and for retained
    `ROUTING_FAILED` evaluation runs; refused data-change requests are not audited. Details never contain
    a password, hash, CSRF token or session id (`USER_CREATED` names the username and role only; the demo
    summary names counts only). Free-text reasons inside details are cut to 200 characters.
  - **Catalogue (38 actions).** `SIGN_IN`, `SIGN_OUT`, `USER_CREATED`, `USER_STATUS_CHANGED`,
    `CATEGORY_CREATED`, `CATEGORY_UPDATED`, `CATEGORY_STATUS_CHANGED`, `POINT_CREATED`, `POINT_UPDATED`,
    `POINT_STATUS_CHANGED`, `DEMAND_ADDED`, `DEMAND_UPDATED`, `DEMAND_REMOVED`, `VEHICLE_CREATED`,
    `VEHICLE_UPDATED`, `VEHICLE_STATUS_CHANGED`, `AGENT_CREATED`, `AGENT_UPDATED`, `AGENT_STATUS_CHANGED`,
    `AGENT_ACCOUNT_LINKED`, `AGENT_ACCOUNT_UNLINKED`, `DATA_IMPORTED`, `TOUR_CREATED`, `TOUR_UPDATED`,
    `TOUR_STOP_ADDED`, `TOUR_STOP_REMOVED`, `TOUR_STOPS_REORDERED`, `TOUR_ASSIGNED`, `TOUR_EVALUATED`,
    `PROPOSAL_ACCEPTED`, `PROPOSAL_REJECTED`, `TOUR_CONFIRMED`, `TOUR_STARTED`, `STOP_OUTCOME_RECORDED`,
    `TOUR_COMPLETED`, `TOUR_CANCELLED`, `STOP_OUTCOME_CORRECTED`, `DEMO_LOADED`. Entity types: `USER`,
    `WASTE_CATEGORY`, `COLLECTION_POINT`, `VEHICLE`, `AGENT`, `TOUR`, `DATA_IMPORT`, `DEMO_DATA`. Stop-level
    events use the tour as entity, so one filter returns a tour's whole trail. Link and unlink events are
    written only when something changed. Details formats are in the service code (for example
    `username <u>, role <ROLE>`, `from <STATUS>; <n> of <m> stops resolved; reason: <reason>`,
    `correction <id> of stop <n> <point>: <PREV> -> <NEW>; reason: <reason>`).
  - **Seeds, import and demo.** Start-up seeding of the dev accounts and map sample is not audited
    (bootstrap). The MN01 import records `DATA_IMPORTED` per run; the demo load records one `DEMO_LOADED`
    (counts only), and the tour steps it performs through the services are their own events (actor `system`
    at start-up, the admin for `POST /api/admin/demo`).
  - **Cancellation.** `POST /api/tours/{id}/cancellation` `{reason}` (`@NotBlank`, at most 500), 200 with the
    tour. Rule order: 400, 404, 409 (`COMPLETED` or `CANCELLED`: "Only a DRAFT, PLANNED or IN_PROGRESS
    tour can be cancelled; this tour is X."), 200. The tour keeps stops (pending stays `PENDING`),
    outcomes, quantities, crew, vehicle and route, and records `cancellationReason`, `cancelledAt`,
    `cancelledBy`, `cancelledFromStatus`; revision + 1; pessimistic lock. Every later edit, evaluation,
    confirmation, start, outcome and completion answers 409; `DELETE` stays 405. The crew sees a cancelled
    tour iff it had been confirmed (`cancelledFromStatus` `PLANNED` or `IN_PROGRESS`): `Tour.isVisibleToCrew()`
    and `TourRepository.findAssignedTo` agree.
  - **Correction.** `POST /api/tours/{id}/stops/{stopId}/corrections` `{status, quantities, incidentReason,
    reason}` (same body rules as the S7 outcome plus a required reason of at most 500). Rule order: 400, 404
    tour, 404 stop of another tour, 409 tour not `IN_PROGRESS`/`COMPLETED`/`CANCELLED`, 409 pending stop,
    404 category, 422 duplicate category, 422 inactive category that is neither planned on the stop nor
    already recorded, 422 nothing changes, 200. One `OutcomeCorrection` is appended with the previous and
    the new status, incident text and quantities (category id, name at that time, kilograms), the
    correcting user, UTC time and reason; the stop's `outcomeRecordedBy/At` keep the original recording;
    corrections are never edited or deleted. Quantities are updated in place (kept categories), removed
    (dropped) or added (new): clearing and re-adding would hit the unique key because Hibernate flushes
    inserts before deletes. Correction times are strictly increasing per stop so the order is stable.
    `OutcomeQuantities` holds the category rules shared with outcome recording.
  - **History.** `GET /api/tours/history?from&to&status&vehicleId&agentId&pointId`: only `COMPLETED` and
    `CANCELLED` tours; `from`/`to` inclusive on the planning date; `agentId` matches the driver or a
    collector; `pointId` matches any stop; order planning date desc, terminal time desc, creation desc.
    400: `status` other than `COMPLETED`/`CANCELLED` (field `status`), `from` after `to` (field `to`),
    malformed date or uuid. The detail is the existing `GET /api/tours/{id}`; no reopen, no delete. The
    literal mapping wins over `/{id}`.
  - **Audit search.** `GET /api/admin/audit-events?from&to&actor&action&entityType&entityId&outcome&page&size`
    (`from` inclusive, `to` exclusive, ISO-8601 instants with `Z`; `actor` exact after trim and lower-case;
    page from 0, size 1…200, default 50; newest first) and `GET /api/admin/audit-events/{id}`. 400 for
    unknown enums, malformed instants, `from` not before `to` (field `to`), `page` < 0, `size` out of range.
    The null-tolerant JPQL variant worked with a null `Instant` parameter, so the sentinel fallback was not
    needed.
  - **Schema.** All new `tour` columns are nullable; new tables `outcome_correction`,
    `outcome_correction_previous_quantity`, `outcome_correction_new_quantity` and `audit_event`, so
    `ddl-auto: update` upgrades an S7 database in place (verified from the S7 jar and on a copy of the
    owner's database file).
  - **Frontend (D-020, D-040).** Cancellation card and dialog, cancellation facts, a *Correct* dialog per
    recorded stop, corrections under each stop (planner and agent), a cancelled notice for the crew, the
    `/history` page (PLANNER) and the `/admin/audit` page (ADMIN); verified by build, lint, bundle strings and
    proxy checks only.
- **Rationale:** One hook inside the change transaction makes the log exactly as truthful as the data
  (no event without a committed change, none missing), keeps controllers free of audit code and keeps
  `record` unable to poison a transaction. An append-only repository type plus a GET-only API makes
  immutability structural rather than a convention. Retaining previous values in a dedicated entity keeps
  corrections traceable (US-31) without making history editable.
- **Deviations from the plan.**
  - The commit trailer is `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`, as the executing
    environment instructs (the plan named another model as a fallback).
  - `TourStop.correctOutcome` makes the correction time strictly increasing per stop (the plan only said
    `correctedAt ASC`), so two corrections in the same millisecond keep their order.
  - `Mn01ImportServiceTest` built `Mn01ImportService` directly and was updated for the new constructor
    parameter (anticipated by the plan's constructor risk).
  - The T8 and T9 frontend commits share `App.tsx`, `app-shell.tsx` and `home-page.tsx`; the audit
    parts were held back from the T8 commit so each commit builds.
  - `TourStopList` now takes the whole `tour` instead of `stops` and `tourStatus`, because the correction
    dialog needs `tour.correctable`.
  - The done-check ran unmodified: 173 `PASS` lines, `FAILS=0` on the first run, no
    `s8-done-check.orig.sh`.

## D-050 — S9 implementation conventions

- **Date:** 2026-11-02
- **Decision:**
  - **Roles.** Dashboard, explorer reads, CSV import and export and the traffic overlay: PLANNER and ADMIN
    (two new `SecurityConfig` matchers, `/api/dashboard/**` and `/api/traffic/**`). Running an experiment
    batch (`POST /api/admin/experiments`): ADMIN only. AGENT 403, anonymous 401.
  - **Scenario kinds and batches.** `ScenarioKind` is `TOUR_DRAFT`, `EXPERIMENT`, `CSV_IMPORT`. A new
    entity `EvaluationBatch` (table `evaluation_batch`, outside the D-033 class diagram) records one
    experiment execution or one CSV import; runs reference it by a plain `batch_id`. A batch is
    *canonical* iff it has all three profiles, seeds exactly 1-5 and sizes exactly 10, 25, 50, 100 (any
    repetitions). Aggregates exist per batch only, so the demo tour evaluations never enter them.
  - **Generator `mn01-v1`.** Pool: the 238 DSNY baskets of the committed snapshot sorted by `basketid`
    (`String.compareTo`). Locations: `java.util.Random(seed)` Fisher-Yates prefix; the first `size`
    entries are the input order. Demand: `java.util.Random(31*seed+code)` per point in input order;
    `UNIFORM_LOW`(1) 10-30 kg, `UNIFORM_HIGH`(2) 30-60 kg, `HOTSPOT`(3) one point in five 60-120 kg else
    5-20 kg, whole kilograms, category `Mixed litter`, synthetic. Vehicle capacity fixed at 10 000 kg (an
    assumed rear-loader payload; the largest canonical total is 4 639 kg, so every canonical scenario is
    feasible and the experiment measures sequencing); service time 600 s; depot MN01G. Scenario key
    `mn01-<PROFILE>-s<seed>-n<size>`. Sizes are nested and the profiles of a seed and size share the
    points. The same profile, seed and size give identical stops, matrices, orders, distances and
    durations on any database (only execution time, ids and timestamps differ).
  - **Experiment trigger.** `POST /api/admin/experiments` (synchronous, at most 180 runs, field-level
    400s, 201 with `Location`) and `tools/run-experiment.sh OUT_DIR [--wastems.experiment.NAME=VALUE]`
    (profile `experiment`, random port, own H2 database and routing cache under `OUT_DIR`, writes
    `results.csv`, `scenarios.csv`, `batch.json`, `run-info.txt`, `run.log`; exit codes 0 completed, 1
    error, 2 aborted, 64 non-empty directory; `WASTEMS_SKIP_BUILD=1` skips the rebuild). Each run is saved in
    its own transaction; a routing outage aborts the batch and audits it as `FAILURE`.
  - **CSV import (US-37).** `POST /api/evaluations/imports` with a raw `text/csv` body; strict whole-file
    validation first (columns, types, ranges, kilograms with at most two decimals, category, point
    identity, duplicate point/category/order, constant capacity/profile/seed per scenario, limits 1 MiB,
    5 000 rows, 10 scenarios, 100 points); any error is a 400 with at most 100 `rowErrors` and nothing is
    created; then 409 (no MN01G depot) or 503; then the batch, its runs and one audit event are saved in
    one transaction. Imported points must already exist and keep their ids and source keys.
  - **CSV export (US-38).** One row per run and planning method (37 columns, `docs/evaluation-csv.md`);
    routing-failed runs give three rows with empty indicators; the percent change
    `(value - inputOrder) / inputOrder x 100` is computed only in `EvaluationComparison` and is empty
    unless the run is completed, both results are feasible and the baseline is above 0. The batch
    scenarios export uses the import format (round trip).
  - **Aggregates.** Per batch, grouped all / by size / by profile and per method: mean of the per-run
    changes over comparable runs (not a ratio of sums), min and max, mean distance and duration, mean
    execution time, runs, comparable and excluded runs.
  - **Dashboard.** `GET /api/dashboard/operations?from&to` over planning dates; resource counts are
    current; tour counts by status for tours planned in the period; stop progress and collected kilograms
    over PLANNED, IN_PROGRESS and COMPLETED tours only; links to filtered lists; no stop list view; the
    page states that it is recorded data, not live tracking.
  - **Traffic overlay.** A committed, timestamped snapshot
    (`backend/src/main/resources/traffic/mn01-traffic-links.json`, SHA-256 `2a1b9f1f...`, snapshot id
    `nyc-dot-i4gi-tjb9:12:2a1b9f1faa7f`, feed time 2026-11-01T20:08:05Z), refreshed only by
    `tools/fetch-traffic-snapshot.py`; the running application never calls the feed. Links are `OBSERVED`
    (no more than 15 minutes older than the feed time), `STALE`, `NO_DATA` or `INVALID` (blank id,
    unparsable time, unusable geometry, speed above 100 mph; counted, not drawn); bands under 10, 10-25,
    25 mph and over; geometry from the decoded polyline (doubled backslashes reduced) else `link_points`
    (last token dropped at 255 characters); times converted from `America/New_York`; a missing snapshot
    answers 200 with `available: false`. The package `traffic/` is isolated (enforced by
    `TrafficIsolationTest`); the texts are the coverage note, the display-only note, the attribution and
    the NYC DOT disclaimer.
  - **H2 ENUM finding and schema.** Hibernate maps string enums to native H2 `ENUM` columns. The three
    affected columns (`audit_event.action`, `audit_event.entity_type`, `evaluation_run.scenario_kind`) and
    the new enum columns are mapped with `@JdbcTypeCode(SqlTypes.VARCHAR)`; `SchemaUpgrade` converts any
    remaining `ENUM` column of those three at start-up and back-fills `evaluation_run.point_count`. New
    nullable columns `evaluation_run.batch_id`, `scenario_key`, `repetition`, `point_count`,
    `evaluation_stop.demand_breakdown` and the table `evaluation_batch`. A fresh Hibernate VARCHAR enum
    column carries a `CHECK (col in (...))` constraint, so a future enum value would need `SchemaUpgrade`
    to drop it.
  - **Audit.** Two new actions (catalogue 40), `EVALUATION_EXPERIMENT_RUN` and
    `EVALUATION_SCENARIOS_IMPORTED`, entity type `EVALUATION_BATCH` (9 types), one event per batch written
    in the transaction that saves the batch; outcome `FAILURE` when a run is `ROUTING_FAILED` or the batch
    was aborted. Reads, exports and the overlay are not audited.
- **Rationale:** One generator, one runner and one export format make the S10 campaign reproducible and
  checkable by an independent recomputation. A committed snapshot keeps the traffic layer reproducible and
  offline and makes the D-021 boundary structural. Validating the whole CSV before writing anything keeps
  imports atomic; a single percent-change function keeps the explorer, the aggregates and the export
  consistent.
- **Deviations from the plan.**
  - Hibernate 6.6 (Spring Boot 3.5) itself alters the three existing `ENUM` columns to `VARCHAR` on
    `ddl-auto: update` (the plan said it never changes a column type), so the `SchemaUpgrade` conversion
    never logs on an S8 database; it remains as a safety net and is covered by its unit test. The done-check
    assertion expecting three `Schema upgrade: ... converted` log lines was replaced by a check that the
    three columns are `VARCHAR` afterwards.
  - The `ExperimentCommandRunnerTest` case with properties `[HOTSPOT]`, `[2]`, `[5, 3]` and two repetitions
    asserts 4 runs (1 x 1 x 2 sizes x 2 repetitions); the plan said 2.
  - `TrafficOverlayService` has two constructors (one with a `Clock` for tests); the public one carries
    `@Autowired`.
  - The tests that import MN01 clean the imported baskets, depot and source snapshots after themselves
    (the plan only asked for cleanup before), so later test classes see the database they expect.
  - `ExperimentOutputWriter` reads the batch through `EvaluationExplorerService` rather than a repository.
  - The commit trailer is `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`, as the executing
    environment instructs.
  - Done-check corrections (original kept as `s9-done-check.orig.sh`): (1) the A2 ENUM check above; (2)
    two greps for the word "traffic" matched pre-existing prose in `RoadNetworkMatrixProvider.java` and
    `evaluation-indicator-table.tsx` and now look for code references (`com.ase.wastems.traffic`,
    `TrafficOverlay`, `/api/traffic`, `useTrafficOverlay`, the traffic components); (3) phase C logged in
    right after the port opened, before the `dev` runner had created the accounts, and now waits for the
    seeder. No expected value of the plan was weakened; the final run has 162 `PASS` lines and `FAILS=0`.

## D-051 — S10 conventions

- **Date:** 2026-11-02
- **Decision:**
  - **Campaign.** One canonical batch with three repetitions
    (`bash tools/run-experiment.sh metrics/experiment --wastems.experiment.repetitions=3
    --wastems.experiment.label="S10 canonical campaign"`): 60 scenarios x 3 repetitions = 180 runs = 540
    rows, exactly at the 180-run limit of D-050. Distances, durations, orders and feasibility are read from
    repetition 1 (`tools/experiment-stats.py` proves that all repetitions are identical); execution time
    uses all repetitions. The run is made on a clean tracked tree so `run-info.txt` records
    `tracked_changes: 0`.
  - **Committed results.** `docs/results/experiment/` (`results.csv`, `scenarios.csv`, `batch.json`,
    `run-info.txt`, `run.log`; never the H2 `db/` or `routing-cache/`), `docs/results/experiment-stats/`
    (outputs of `tools/experiment-stats.py`) and `docs/results/metrics/` (summaries copied by
    `bash tools/metrics.sh --publish`). The raw tool reports stay in the git-ignored `metrics/`.
    `.gitignore` gets `!docs/results/**/*.log` and `!docs/results/metrics/` (the existing `metrics/` rule
    would otherwise ignore the published summaries too). Nothing under `docs/results/` is edited by hand.
  - **JDepend.** The `metrics` profile adds `guru.nidi:jdepend` 2.9.5 as a dependency of
    `jdepend-maven-plugin` 2.1: JDepend 2.9.1 cannot read Java 8+ constant-pool entries (183
    `Unknown constant: 18` errors, 90 of 273 class files analysed). `tools/metrics.sh` now fails when
    that message appears and guards against stale classes in `backend/target/classes`.
  - **Complexity.** Java: a second PMD execution (`pmd-cc-all`, `tools/pmd/wastems-cc-all.xml`,
    `methodReportLevel` 1) gives the full distribution; the quality ruleset gives the hot spots (10 or
    more). TypeScript: oxlint `eslint/complexity` with `max: 0` (`tools/oxlint-complexity.json`),
    vendored `components/ui` excluded. CKJM's bytecode complexity is not reported; CK and coupling metrics
    are Java-only.
  - **Statistics.** Descriptive statistics over the 60 scenarios; paired comparisons over the 20 distinct
    routing instances (the three profiles are identical because the capacity never binds) with an exact
    two-sided Wilcoxon signed-rank test, plain Python (`tools/experiment-stats.py`, with `--self-test`).
  - **Screenshots.** `tools/capture-screenshots.mjs` is an optional tool; it needs a puppeteer installed
    outside the repository (`PUPPETEER_DIR`) and is not a dependency (D-040 holds). The 12 images are
    committed under `presentation/assets/`.
  - **Deck.** One self-contained `presentation/index.html` (inline CSS and JS, no CDN, no web font),
    images as files in `presentation/assets/`, keyboard navigation, `#n` in the URL, print CSS. The
    architecture diagram is the Mermaid source `presentation/assets/architecture.mmd` rendered to
    `architecture.svg`. The deck carries no date: the presentation date stays open (AGENTS.md section 4).
  - **Traceability.** `docs/traceability.md` maps FR-01 to FR-30 to user stories, use cases, code and
    tests, with the gaps listed.
- **Rationale:** Committed, small result files let a reviewer check every number without re-running
  anything, and one fixed batch keeps the report reproducible (NFR-09). Using the 20 instances instead of 60
  scenarios avoids pseudo-replication. Failing loudly on the JDepend parse error prevents a silently
  partial coupling report.
- **Deviations from the plan:**
  - An extra commit `chore: let git track the committed Part 6 results` carries the `.gitignore` change,
    so that the metrics run recorded in `docs/results/metrics/environment.txt` has `tracked_changes: 0`.
    The campaign itself ran at commit `5b2e9cf` (the campaign commit is in `run-info.txt`).
  - `tools/ts-complexity-summary.py` also matches `async function` and `constructor` lines (the plan's
    expression missed 13 of the 709 reported functions) and exits 1 on any unparsed complexity line.
  - `tools/jdepend-summary.py` prints the strongly connected packages computed from the dependency graph
    in addition to JDepend's cycle paths, which list the same cycle once per package.
  - `tools/capture-screenshots.mjs` takes the card screenshot with `element.screenshot()` (a clip with
    `captureBeyondViewport: false` failed with a 0-height error for a card taller than the viewport). The
    screenshots committed in T6 come from a capture run on the same source tree (scratch database, 18 OSM
    tile responses failed and 69 succeeded; every committed image shows its tiles).
  - The execution-time maximum of the committed campaign (all sizes, NN + 2-opt) differs from the
    planner's timing run; only distances are deterministic, as expected.
  - Done-check correction (original kept as `s10-done-check.orig.sh`): the two checks that grep for the
    all-size mean now use `grep -qF --`, because the value starts with `-` and was read as an option; no
    expected value was changed.
  - Further done-check corrections, same original kept: (1) the C check counted `grep '^BAD'` and matched the
    checker's own total line `BADS 0`, so it now counts `^BAD `; (2) the J capture step ran after the
    walkthrough had cancelled the only draft tour and so could not find a `DRAFT` tour; the Vite start, the SPA
    route checks and the capture now run right after J step 1 (no assertion removed); (3) the A check for
    `backend/routing-cache/` assumed the owner's cache exists; it is created lazily, was absent, and nothing in
    S10 deleted it (every S10 run used an explicit scratch cache directory), so it is now an `INFO` line
    while `backend/data/` is still required. Final run: 169 `PASS` lines in phases A to J, `FAILS=0`.
  - `docs/06-metrics.md` was assembled by a throw-away script that copies the tables and summaries
    verbatim from `docs/results/`; the script is not part of the repository.

## D-052 — Sweep baseline as a fourth planning method (amends D-008; refines D-022)

- **Date:** 2026-11-02
- **Context:** The S10 research answer compares NN + 2-opt with the *input order*. In the seeded
  experiment (D-050 generator `mn01-v1`) the input order is a Fisher-Yates shuffle of the sampled
  baskets, so the reported -60 % distance change is mostly a property of a random, strawman
  baseline (S10 review, `docs/06-metrics.md` section 7). No public dataset gives the real DSNY stop
  sequence: the Litter Basket Inventory, the DSNY collection frequencies, PlowNYC and the NYC truck
  routes were checked and none contains a visit order. A realistic, published, deterministic
  baseline is therefore taken from the literature instead of from operations data.
- **Decision:** The project owner chose the **sweep** heuristic (Gillett & Miller, 1974, "A
  Heuristic Algorithm for the Vehicle-Dispatch Problem", *Operations Research* 22(2), 340-349,
  <https://doi.org/10.1287/opre.22.2.340>) as an additional, geometry-based baseline. The manager
  fixed the design below; it is not reopened without the owner.
  - **Methods.** `PlanningMethod` gets a fourth value `SWEEP`; the declared (and evaluation) order
    is `INPUT_ORDER`, `SWEEP`, `NEAREST_NEIGHBOR`, `NN_PLUS_2_OPT`. `INPUT_ORDER` stays as the
    worst-case reference. The research answer becomes **NN + 2-opt versus sweep**; the comparison
    with the input order is still reported.
  - **Module.** A new framework-free class `optimization/SweepBaseline`; the package keeps
    depending on `java.*` only (`OptimizationIndependenceTest`). `PlanningScenario` gains optional
    WGS84 coordinates of every node (index 0 = depot, 1..n = stops in input order) through a second
    constructor; the existing four-argument constructor remains and means "no coordinates". The
    service (`EvaluationService`) always passes them. Without coordinates the `SWEEP` outcome is
    infeasible with the reason "The sweep baseline needs the WGS84 coordinates of the depot and of
    every stop." This keeps the 13 test call sites unchanged and never fabricates a sweep result.
  - **Angle.** Local equirectangular projection around the depot: x = (lon - lon0) x cos(lat0),
    y = lat - lat0; theta = atan2(y, x) normalised to [0, 2 pi) (a negative value gets + 2 pi; a
    result equal to 2 pi after rounding becomes 0). Stops are sorted by ascending theta; ties by the
    planar distance sqrt(x^2 + y^2) to the depot, then by node index. (A positive scaling of x does
    not change the cyclic order of angles; the projection makes theta a geometric angle and fixes
    the distance tie-break, it does not tune the result.)
  - **Ray choice ("best sweep ray").** With the sorted list S[0..n-1], every start position k is
    evaluated counter-clockwise (S[k], S[k+1], ..., S[k-1], indexes mod n) and clockwise (S[k],
    S[k-1], ..., S[k+1]) on the exact directed road **distance** of the closed tour depot -> ... ->
    depot (`TourCost.distance` on the same `DirectedCostMatrix` as the other methods). The cheapest
    of the 2n candidates is kept; ties go to counter-clockwise before clockwise, then to the lowest
    start position (strict improvement only, candidates in that order). This is the strongest pure
    sweep, O(n^2) with n <= 100.
  - **Feasibility.** Capacity and coverage are validated exactly as for `INPUT_ORDER` (the
    module's common checks; one trip, no intermediate depot return, no capacity-driven route
    split).
  - **Configuration string.** `SweepBaseline.CONFIGURATION` states the method, the projection, the
    tie-breaks, the 2n-ray selection on directed road distance and "single trip, no depot reload";
    the module appends the chosen ray (`; chosen ray: start stop <node>, CCW|CW`).
  - **Comparison only.** `SWEEP` is evaluated and retained with every run (tour drafts,
    experiment and CSV-import batches) and appears in the comparison table, the evaluation
    explorer, the aggregates, the CSV export, the experiment and the statistics. Proposal
    acceptance is unchanged: accept NN + 2-opt or keep the input order; a sweep route is never
    attached to a tour (`Tour.routeMethod` is only `INPUT_ORDER` or `NN_PLUS_2_OPT`, stated as an
    OCL invariant). The existing per-method route layer loop draws the sweep legs; no bespoke map
    layer is added, and the sweep layer is unchecked by default.
  - **Percent changes.** The change against the input order is kept everywhere. A change against
    sweep, `(value - sweep) / sweep x 100`, is added for `NEAREST_NEIGHBOR` and `NN_PLUS_2_OPT`
    (empty for `INPUT_ORDER` and `SWEEP`); sweep against the input order is the `SWEEP` row's
    existing change column. `EvaluationComparison.changePct` stays the single formula (its
    reference parameters are renamed, not duplicated). The results export gains four columns after
    `duration_change_pct`: `sweep_distance_m`, `sweep_duration_s`, `distance_change_vs_sweep_pct`,
    `duration_change_vs_sweep_pct` (41 columns). The scenario import/export format is unchanged,
    so the round trip still holds (scenario points already carry coordinates).
  - **Runs retained before this decision** have no `SWEEP` result. Views show "—", the aggregates
    count them as not comparable for `SWEEP` and for the change against sweep, and the export
    writes no `SWEEP` row for a completed run without a `SWEEP` result (a `ROUTING_FAILED` run
    still gives one row per method, now four). Nothing is back-filled.
  - **Schema.** `evaluation_method_result.method` (and `tour.route_method`) are native H2 `ENUM`
    columns in existing databases (checked on a copy of `backend/data/wastems.mv.db`), which would
    reject `SWEEP`. Both are mapped with `@JdbcTypeCode(SqlTypes.VARCHAR)` (length 20 fits
    `SWEEP`) and `SchemaUpgrade` converts them like the D-050 columns.
  - **Experiment and report.** The S10 campaign (60 scenarios x 3 repetitions, D-051 command) is
    re-run, now 720 rows; `tools/experiment-stats.py` adds sweep to its checks, tables and chart and
    exact Wilcoxon tests NN + 2-opt vs sweep, NN vs sweep and sweep vs input order; the code metrics
    are re-run with `bash tools/metrics.sh --publish`; `docs/results/` is regenerated and
    `docs/06-metrics.md` is rewritten from those files only.
- **Rationale:** Sweep is a classical, citable, deterministic construction heuristic that a
  planner could apply with a map and a ruler, so it is a far more realistic stand-in for a manual
  sequence than a random shuffle, while needing no unpublished data. Choosing the best of all 2n
  rays on the same directed road distance makes the baseline as strong as a pure sweep can be, so a
  remaining gain of NN + 2-opt is not an artefact of an unlucky ray. Keeping the input order shows
  the worst case and keeps the S10 numbers comparable. Optional coordinates keep the module
  framework-free and the change small without inventing a result when coordinates are absent.
- **Impact:** Amends D-008 (four methods instead of three; the "baseline" of the research answer
  is now sweep). Refines D-022 (same scenarios and directed matrix; sweep additionally uses the
  real WGS84 coordinates already retained). D-050 and D-051 formats change as stated above.

## D-053 — S11 conventions (deviations from the S11 plan)

- **Date:** 2026-11-02
- **Decision:** The executor followed D-052 and the S11 plan with these deviations only.
  - **Literature count.** At the owner's ruling source 9 (Liang et al., 2022) was replaced by Gillett
    and Miller (1974) instead of adding an eleventh source; the review keeps ten sources and the
    ranges `[7-10]` and `[6-10]` became `[7, 8, 10]` and `[6-8, 10]`.
  - **Refactor after the campaign.** The `PlanningScenario` constructor reached CC 35; its validation
    was split into two helpers (`bd77433`) after the campaign and the first metrics run. The refactor
    is behaviour-neutral (419 tests; a 144-row re-run reproduced all distances, durations, orders and
    sweep distances) so the campaign was not repeated; the code metrics were re-run afterwards.
  - **Result commits.** The campaign (`a52c957`) and the two metrics runs (`586cfcd`, `367d114`) are
    separate commits, so that the metrics run on a clean tree.
  - **Order of T5.** `docs/evaluation-csv.md` was written after the campaign because its example is
    copied from the real `results.csv`; its column list was compared with `RESULT_COLUMNS` by script.
  - **Sweep colour.** `#9333ea` instead of `#7c3aed`, which the depot marker already uses.
  - **Statistics.** The tool prints a second chart (`distance-change-vs-sweep-by-size.svg`); the
    Holm adjustment covers all nine paired tests; a count of beats, ties and loses is added for NN
    and NN + 2-opt against sweep and for sweep against NN.
  - **Deck.** One slide was added (results against the input order), so the deck has 30 slides; only
    the sequence and activity SVGs changed and were copied; the four evaluation screenshots were not
    re-captured and the slide states that they show the S10 interface.
  - **Demo values.** The sweep distance of the demo draft (8.44 km) was taken from an evaluation over
    HTTP on a fresh `dev` start on a scratch database.
- **Rationale:** Each change keeps the measured data honest and the repository runnable; none changes
  the D-052 design.

## D-054 — Minimal vector basemap (supersedes D-039)

- **Date:** 2026-11-02
- **Decision:** The Leaflet maps draw OpenStreetMap data from the OpenFreeMap vector tiles
  (`tiles.openfreemap.org`, OpenMapTiles schema, no key) with MapLibre GL 6.14, embedded as a
  Leaflet layer by `@maplibre/maplibre-gl-leaflet` 0.1.4. The style is our own
  (`frontend/src/components/maps/basemap-style.ts`): no businesses, house numbers or transit
  icons; neighbourhood names, major street names from zoom 14 and minor street names from zoom 17;
  neutral white/grey roads; green parks and woods; blue water labelled only for rivers, bays and
  the sea. Attribution: "OpenFreeMap © OpenStreetMap contributors". Markers, routes and the traffic
  overlay stay Leaflet layers on top. The CARTO Voyager raster tiles were previewed and rejected.
- **Rationale:** The standard OSM raster tiles bake in labels, POI icons and colours, so the
  project owner found the map overcrowded and could not restyle it. Vector tiles keep OSM data but
  let the style hide clutter, and neutral roads keep the coloured traffic, route and stop layers
  readable. The worker URL is set explicitly (`setWorkerUrl`, Vite `worker.format: 'es'`)
  because Vite's dependency pre-bundling breaks MapLibre's relative worker path.
- **Map UI (same day):** every map is limited to Manhattan (`MANHATTAN_BOUNDS`, minimum zoom
  12, `geo.ts`), frames its data on first load, and has *Reset view* and *Fit to data* buttons,
  a scale bar and a legend. Markers share one palette (`map-theme.ts`): teal dots with a white
  ring for collection points (smaller when zoomed out), numbered badges for stops (ink in drafts,
  status colours once planned), a violet square for the depot. A single attached route gets a
  white casing; overlapping comparison routes do not. Candidate points start hidden on locked tours.
  The sweep route on the comparison map is now shown by default like the other three methods
  (amends the D-052 default).

## D-055 — Demonstration reset and enriched demo dataset (amends D-023 demo scope)

- **Date:** 2026-11-02
- **Decision:** The demonstration data can be wiped and rebuilt at any time. The reset deletes
  every `SYNTHETIC`-origin tour (cascading stops, outcomes and corrections), the `TOUR_DRAFT`
  evaluation runs of those tours, the demand entries recorded by the last demo load (tracked in
  `demo_record`), the `SYNTHETIC` vehicles and agents, and all `demo_record` rows; it then reloads
  the dataset. It refuses (409) when the MN01 import is missing or when non-demo tours still
  reference demo vehicles or agents, without deleting anything. Kept on every reset: user
  accounts, the imported MN01 data (points, depot, its base demand rows, source and routing
  snapshots), `EXPERIMENT`/`CSV` evaluation runs and batches, the audit log and the categories —
  categories are re-adopted by the reload, not deleted (a category is deactivated, never deleted).
  Entry points: `POST /api/admin/demo/reset` (ADMIN), the *Reset demonstration data* button on the
  demonstration card, and the one-shot start-up flag `--wastems.demo.reset=true`
  (`wastems.demo.reset: false` by default; `AuditAction.DEMO_RESET` in the audit log, trigger
  `RESET` in the load report). Controlled exception to the S3/S7 rule that agents and vehicles
  are never deleted, only made unavailable:
  demo (`SYNTHETIC`-origin) vehicles and agents may be deleted by the reset — user-owned and
  experiment data must never lose a referenced resource this way, hence the tour-reference guard.
  The reloaded dataset is richer: 5 vehicles (`DEMO-01`–`DEMO-05`, one unavailable), 7 agents
  (three linked to the demo accounts, one unavailable), 30 extra demand rows (Recyclables/Organics
  on real MN01 baskets), and six tours covering every lifecycle state: draft (12 stops), planned
  (8 stops, accepted proposal, routed 4962 m), planned (6 stops), in-progress (7 stops, first
  three visited with one not-collected), completed (9 stops, one not-collected, one corrected
  retained outcome), cancelled (6 stops from PLANNED). Every synthetic reason in stories carries
  the "(synthetic demo ...)" marker (D-048), synthetic values are stable, and the load remains
  idempotent (re-adoption, no duplicates).
- **Rationale:** A reviewer should see the whole tour lifecycle — including cancellation and a
  privileged correction — in one session without handcrafting data, and should be able to rebuild
  that loss-free state in one action. The reset therefore touches only data the loader itself
  created (origin `SYNTHETIC`, tracked via `demo_record`), keeps all accounts and imported
  evidence, and checks the preconditions before deleting anything.

## D-056 — One-command seeded demo from a fresh clone (`tools/demo.sh`)

- **Date:** 2026-11-02
- **Decision:** The repository ships `tools/demo.sh`, a portable launcher that takes a fresh clone
  to a working, pre-seeded demo. It checks JDK 21+ and Node 20.19+/22.12+ (sourcing
  `~/.local/opt/ase-env.sh` only as a fallback), runs `npm ci` when `frontend/node_modules` is
  missing, starts the backend with the `dev` profile (accounts, MN01 import, demonstration load,
  D-023/D-048/D-055), waits until `GET /api/admin/demo/report` answers 200 for the current run, then
  starts the frontend and prints the URL and the accounts. `--reset` passes the one-shot
  `--wastems.demo.reset=true`. Ports come from `ASE_BACKEND_PORT` / `ASE_FRONTEND_PORT`. Ctrl-C
  stops both process groups. No application code changed: the seeding itself is still the `dev`
  profile's start-up runners; the script only orchestrates and verifies it.
- **Rationale:** The demonstration already seeded itself on a default `dev` start, but a reviewer
  who cloned the repository still had to know the toolchain, install the frontend dependencies and
  start two servers, and `tools/dev.sh` depends on tmux, fnm and the author's `~/.local/opt` layout.
  The HTTP port opens before the start-up runners finish, so a port check alone would report ready
  too early; the demonstration report only exists once the load of this run completed, which makes it
  a reliable readiness signal. `tools/dev.sh` stays as the author's tmux workflow.
