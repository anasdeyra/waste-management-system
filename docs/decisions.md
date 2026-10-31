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
  in D-008–D-010 and D-015–D-016.

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

## D-007 — Prototype scope

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
  - CSV import/export;
  - audit log.

  Explicitly excluded: connected fill-level sensors or other IoT hardware, real-time GPS
  tracking, external commercial mapping or routing services, a citizen-facing mobile
  application, and billing or payment management.
- **Rationale:** Selected by the project owner. The exclusions preserve the scope boundary
  already recorded in `docs/01-analysis.md` §8.
- **Resolution:** "Multiple vehicles" means a fleet with multiple single-vehicle tours per
  planning day; the optimizer plans one tour per run (D-012).

## D-008 — Optimization method and baseline

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

## D-009 — Routing and scenario model

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

## D-010 — Evaluation datasets

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

## D-015 — Duration parameters

- **Date:** 2026-10-31
- **Decision:** The duration model defaults to **40 km/h average speed** and **10 minutes
  of service time per stop**. Both values remain configurable per scenario.
- **Rationale:** Typical urban collection assumptions; configurability keeps the
  evaluation transparent and reproducible.

## D-016 — Evaluation matrix

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

  The API is organised by resource (`/api/collection-points`, `/api/vehicles`, `/api/agents`,
  `/api/tours`, `/api/waste-categories`, `/api/optimization`, `/api/dashboard`,
  `/api/auth`). During development the Vite server proxies `/api` to
  `http://localhost:8080`, so the browser only ever talks to a single origin.
- **Rationale:** Two independent builds keep the frontend toolchain (Node, Vite) out of the
  Maven build and make the metrics of Part 6 attributable to each part. The proxy removes
  the need for CORS in development while the production build can point
  `VITE_API_BASE_URL` to any deployment.
- **Consequence:** The API contract is mirrored by the TypeScript types in
  `frontend/src/types/api.ts`; both sides must be changed together.
