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

## D-008 — Optimization method and baseline (amended by D-022)

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

