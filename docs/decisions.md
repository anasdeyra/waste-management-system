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
