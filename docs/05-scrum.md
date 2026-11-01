# Part 5 — Scrum

## 1. Product Backlog

### 1.1 Backlog conventions

The backlog contains **atomic user stories grouped by epic**, as decided in D-023. Each story is
written from a user perspective, has testable acceptance criteria, and references the functional
requirements in `docs/01-analysis.md`.

Priority describes implementation order, not permission to silently remove agreed Extended-scope
functionality:

- **P0 — Foundation:** required before the main workflow can be used or demonstrated.
- **P1 — Core workflow:** planning, optimization, execution, and operational history.
- **P2 — Extended completion:** supporting visibility, exchange, correction, and audit features.

Across epics, P0 stories come first, then P1, then P2. Within a priority, epics and story numbers
give the recommended order. Dependencies may require a later-numbered P0 item, such as the MN01
seed data, to be completed earlier in practice.

The roles are `ADMIN`, `PLANNER`, and `AGENT`. An administrator inherits all planner capabilities
(D-026). “Collection agent” in a story means the operational person using a linked, active `AGENT`
account.

### 1.2 Epic E01 — Authentication, users, and waste categories

#### US-01 — Authenticate — P0

**As a registered user, I want to sign in with my credentials so that I can access the functions
allowed for my role.**

**Acceptance criteria**

- Given an active account and valid credentials, the user is authenticated and directed to an
  appropriate application view.
- Invalid credentials produce a generic error and do not establish an authenticated session.
- A deactivated account cannot authenticate, and passwords are never returned by the API.

**Traceability:** FR-01, FR-27; NFR-01, NFR-11.

#### US-02 — Sign out — P0

**As an authenticated user, I want to sign out so that another person cannot continue using my
session.**

**Acceptance criteria**

- Signing out invalidates the current authentication session or token.
- Subsequent requests that require authentication are rejected until the user signs in again.
- The interface returns to the sign-in view without exposing protected operational data.

**Traceability:** FR-01; NFR-01.

#### US-03 — Enforce role permissions — P0

**As a system user, I want the application to permit only operations allowed for my role so that
operational and administrative data remain protected.**

**Acceptance criteria**

- `ADMIN` can perform every planner operation as well as user, waste-category, and audit
  administration.
- `PLANNER` cannot manage users, waste categories, or the audit log; `AGENT` cannot perform
  planning or administration.
- An agent can access only tours to whose crew that agent is assigned.
- Authorization is enforced by the backend, not merely by hiding frontend controls.

**Traceability:** FR-02, FR-12, FR-27, FR-30; NFR-01.

#### US-04 — Create a user account — P0

**As an administrator, I want to create a user account and assign its role so that a person can
access the system with the appropriate permissions.**

**Acceptance criteria**

- The administrator can create an active account with a unique username, an initial password,
  and exactly one of `ADMIN`, `PLANNER`, or `AGENT`.
- Passwords are stored only in a securely hashed form.
- An `AGENT` account may be linked to one currently unlinked operational agent; a non-`AGENT`
  account cannot be linked as an agent.
- Public self-registration is not available.

**Traceability:** FR-01, FR-09, FR-27; NFR-01, NFR-11.

#### US-05 — Activate or deactivate a user account — P1

**As an administrator, I want to activate or deactivate an account so that I can control access
without deleting operational history.**

**Acceptance criteria**

- Deactivation prevents new authentication and causes protected operations to reject the
  deactivated account.
- Reactivation restores access according to the account's existing role and agent link.
- Deactivating an account does not delete its agent, assignments, collection results, or audit
  history.

**Traceability:** FR-27; NFR-01, NFR-02.

#### US-06 — Create and edit a waste category — P0

**As an administrator, I want to create and edit waste categories so that demand and collection
results use a controlled classification.**

**Acceptance criteria**

- A category has a unique, non-blank name and an optional description.
- The administrator can correct category details without changing the identity retained by
  existing demands and results.
- Demand and actual-quantity values associated with a category are expressed in kilograms.

**Traceability:** FR-03, FR-06; NFR-02.

#### US-07 — Deactivate a waste category — P1

**As an administrator, I want to deactivate an obsolete waste category so that it cannot be used
for new demand while historical records remain understandable.**

**Acceptance criteria**

- A deactivated category cannot be selected for a new or edited demand entry.
- Existing demands, tour results, and evaluation history retain the category reference.
- Deactivation does not delete the category or rewrite historical data.

**Traceability:** FR-03, FR-06, FR-14; NFR-02.

### 1.3 Epic E02 — Collection points and demand

#### US-08 — Create a collection point — P0

**As a planner, I want to create a collection point with its location and initial demand so that
it can be considered during tour planning.**

**Acceptance criteria**

- The point has a unique identifier, non-blank name, WGS84 longitude/latitude, active status,
  and at least one valid demand entry.
- Longitude and latitude ranges and coordinate order are validated; quantities are positive
  kilogram values associated with active waste categories.
- Imported source dataset, source identifier, and source ownership/type are retained when
  available, while synthetic demand is identified as synthetic.

**Traceability:** FR-04, FR-05, FR-06; NFR-02, NFR-12.

#### US-09 — Inspect and filter collection points — P0

**As a planner, I want to inspect and filter collection points in synchronized list and map views
so that I can find and understand collection demand geographically.**

**Acceptance criteria**

- The map displays the depot and active/inactive points according to the selected filters and
  provides pan, zoom, layer controls, and readable attribution.
- The user can filter by status and waste category and can select a point in either the list or
  map to see the same details.
- Details include coordinates, source/provenance, and all demand entries; every essential action
  remains available without using the map.

**Traceability:** FR-04, FR-05, FR-06, FR-22; NFR-03, NFR-12.

#### US-10 — Edit point details and coordinates — P1

**As a planner, I want to correct collection-point details and coordinates through a form or map
click so that planning uses accurate locations.**

**Acceptance criteria**

- The planner can edit permitted details using numeric coordinate fields or by clicking the map
  and confirming the change.
- The same WGS84 validation applies to both input methods, and invalid changes are not saved.
- A successful update is reflected consistently in the form, list, map, and future planning
  scenarios without rewriting retained evaluation snapshots.

**Traceability:** FR-04, FR-05, FR-23; NFR-02, NFR-03, NFR-09.

#### US-11 — Manage point demand entries — P0

**As a planner, I want to add, edit, and remove waste-category demand entries at a collection
point so that the optimizer uses current collection requirements.**

**Acceptance criteria**

- A point can retain several demand entries for different waste categories, each with a positive
  quantity in kilograms.
- Add, edit, and remove operations validate the category and quantity and leave at least one
  demand entry on an active point.
- Mixed-category demand is summed when the point participates in a vehicle-capacity check.
- Editing current demand does not alter the frozen inputs of a retained evaluation run.

**Traceability:** FR-06, FR-08, FR-15; NFR-02, NFR-07, NFR-09.

#### US-12 — Deactivate a collection point — P1

**As a planner, I want to deactivate a collection point so that it is excluded from new tours
without losing its history.**

**Acceptance criteria**

- An inactive point cannot be added to a new tour or evaluation scenario.
- Existing tour and evaluation records retain the point and its historical values.
- The point remains visible when an inactive-point filter is selected and can be distinguished
  from active points.

**Traceability:** FR-04, FR-14, FR-22; NFR-02.

### 1.4 Epic E03 — Vehicles, agents, and access links

#### US-13 — Create and edit a vehicle — P0

**As a planner, I want to register and edit vehicles so that tours can be assigned to a known
fleet with explicit capacities.**

**Acceptance criteria**

- A vehicle has a unique identifier or registration, a positive capacity in kilograms, and an
  availability status.
- The planner can correct vehicle details while preserving its identity on existing tours.
- Invalid or non-positive capacities are rejected.

**Traceability:** FR-07, FR-08; NFR-02.

#### US-14 — Change vehicle availability — P0

**As a planner, I want to mark a vehicle available or unavailable so that new tour assignments
use only operable resources.**

**Acceptance criteria**

- An unavailable vehicle cannot be assigned to a new tour or confirmed as a valid planned
  resource.
- Availability is rechecked when a tour is confirmed and started.
- Changing availability does not erase or rewrite historical assignments.

**Traceability:** FR-07, FR-10, FR-11; NFR-02.

#### US-15 — Create and edit an agent — P0

**As a planner, I want to register and edit operational agents so that I can assign drivers and
collectors to tours.**

**Acceptance criteria**

- An agent has a unique identifier, a non-blank display name, and an availability status.
- The planner can edit agent details without changing the identity retained by existing tour
  assignments.
- Only information needed for assignment and operational follow-up is required.

**Traceability:** FR-09; NFR-02, NFR-11.

#### US-16 — Manage an agent's login link — P1

**As an administrator, I want to link or unlink an operational agent and an `AGENT` account so
that login access can be controlled independently of the workforce record.**

**Acceptance criteria**

- The link is one-to-one: neither the agent nor the user account can be linked to a second
  counterpart.
- Only an `AGENT` account can be linked to an operational agent.
- An agent without a linked active account has no authenticated execution access, while its
  assignments and history remain intact.
- Unlinking an account does not delete either record or past collection results.

**Traceability:** FR-01, FR-02, FR-09, FR-27; NFR-01, NFR-11.

#### US-17 — Change agent availability — P0

**As a planner, I want to mark an agent available or unavailable so that crews contain only
people available for collection work.**

**Acceptance criteria**

- An unavailable agent cannot be assigned as driver or collector on a new tour.
- Availability and crew distinctness are rechecked when a tour is confirmed and started.
- Changing availability does not erase historical assignments or collection results.

**Traceability:** FR-09, FR-10, FR-11; NFR-02.

### 1.5 Epic E04 — Tour planning and intelligent optimization

#### US-18 — Create a draft tour — P1

**As a planner, I want to create a draft tour for a planning date and depot so that I can prepare
collection work before confirming it.**

**Acceptance criteria**

- A new tour starts in `DRAFT` with a valid planning date and the selected depot.
- The draft can exist while stops and resources are still incomplete, but it cannot start or
  become `PLANNED` until all planning validations pass.
- The planner can consult and edit the draft before confirmation.

**Traceability:** FR-11; NFR-02.

#### US-19 — Select and arrange tour stops — P1

**As a planner, I want to select active collection points and arrange their input order through
synchronized map and list interactions so that the tour has an explicit baseline sequence.**

**Acceptance criteria**

- The planner can add or remove eligible stops from either the map or list, with no duplicate
  point in one tour.
- The current input/selection order is visible and editable without requiring map interaction.
- The selected points, coordinates, demands, depot, and order form the scenario used by all three
  planning methods.
- Inactive points cannot be newly added.

**Traceability:** FR-11, FR-15, FR-19, FR-23; NFR-03, NFR-07.

#### US-20 — Assign the vehicle and crew — P1

**As a planner, I want to assign one vehicle, one driver, and up to two collectors so that the
tour has a valid operating crew and capacity.**

**Acceptance criteria**

- The assignment contains exactly one available vehicle, exactly one available driver, and zero
  to two available collectors.
- Every assigned person is distinct, and driver and collector assignment roles remain explicit.
- The system sums all selected mixed-category demand and reports any excess over vehicle
  capacity; an over-capacity tour cannot become `PLANNED`.

**Traceability:** FR-06, FR-08, FR-10, FR-11; NFR-02.

#### US-21 — Run the three-method evaluation — P1

**As a planner, I want to evaluate input order, nearest neighbor, and nearest neighbor plus 2-opt
on the same scenario so that I can review an optimized proposal and measure its impact fairly.**

**Acceptance criteria**

- All methods receive identical selected points, frozen demands, depot, vehicle capacity,
  service times, directed distance/base-time matrix, and road-graph snapshot.
- Each feasible method starts and ends at the depot and visits every selected point exactly once;
  no point is silently omitted.
- NN + 2-opt recalculates directed costs for reversed segments and does not use a symmetric-edge
  shortcut.
- The run retains each method's order, road-following leg geometry, distance, estimated duration,
  execution time, feasibility checks, algorithm configuration, and source/routing snapshot IDs.
- A failed run does not modify or corrupt the existing operational tour.

**Traceability:** FR-15, FR-16, FR-19, FR-20, FR-21; NFR-04 to NFR-09.

#### US-22 — Understand an infeasible or failed optimization — P1

**As a planner, I want a useful explanation when a scenario is infeasible or cannot be routed so
that I can correct the inputs instead of receiving a misleading tour.**

**Acceptance criteria**

- The system identifies the failed validation or unavailable routing input, such as excess
  demand or a missing directed road leg.
- No partial proposal is presented as a valid optimized tour.
- The draft and all previously retained operational/evaluation data remain unchanged.

**Traceability:** FR-16, FR-18; NFR-02, NFR-05, NFR-06.

#### US-23 — Compare route geometry and indicators — P1

**As a planner, I want to compare baseline, nearest-neighbor, and optimized routes and indicators
so that I can understand where the measured improvement comes from.**

**Acceptance criteria**

- Distinct map layers show ordered stops and road-following geometry for the available methods.
- A synchronized view shows visit order, total directed road distance, estimated duration,
  execution time, and constraint/coverage results for each method.
- Distance and duration changes relative to input order use the same scenario and formulas.
- Basemap attribution remains visible, and traffic information is not presented as an optimizer
  cost input.

**Traceability:** FR-18, FR-19, FR-20, FR-24; NFR-03, NFR-06, NFR-07, NFR-12.

#### US-24 — Accept or reject an optimized proposal — P1

**As a planner, I want to review and explicitly accept or reject an optimized proposal so that an
algorithm cannot silently change operational work.**

**Acceptance criteria**

- Accepting a valid proposal applies its visit order and road-route reference to the editable
  draft tour.
- Rejecting or discarding a proposal leaves the draft's current operational order unchanged.
- Acceptance alone does not start the tour or bypass later planning validation.
- The retained evaluation still contains all compared methods, including a rejected proposal.

**Traceability:** FR-16, FR-17, FR-21; NFR-05, NFR-06, NFR-09.

#### US-25 — Confirm a tour as planned — P1

**As a planner, I want to confirm a valid draft as `PLANNED` so that its assigned crew can execute
a reviewed collection tour.**

**Acceptance criteria**

- Confirmation revalidates the date, depot, stops, coverage, capacity, vehicle, driver,
  collectors, and resource availability.
- A valid draft moves from `DRAFT` to `PLANNED`; an invalid draft remains editable and displays
  useful errors.
- Ordinary structural edits to stops, order, and resources are no longer allowed while the tour
  is `PLANNED`.

**Traceability:** FR-08, FR-10, FR-11, FR-17; NFR-02.

### 1.6 Epic E05 — Collection execution and history

#### US-26 — Consult an assigned tour — P1

**As an assigned collection agent, I want to consult my tour's ordered stops and road route so
that I know what collection work to perform.**

**Acceptance criteria**

- A linked, active agent can see only tours to whose crew that agent is assigned.
- The view includes date, status, depot, ordered stops, demands, road-following geometry, and
  pending/terminal stop states.
- The ordered-stop list provides the essential information without requiring map interaction.
- An unassigned or unlinked agent receives no access to the tour.

**Traceability:** FR-02, FR-12, FR-24; NFR-01, NFR-03.

#### US-27 — Start a planned tour — P1

**As an assigned collection agent, I want to start a planned tour so that the system can
distinguish pending work from active collection.**

**Acceptance criteria**

- Only an assigned crew member can start the tour, and only from `PLANNED`.
- Starting rechecks that the assigned vehicle and agents remain available and that the tour is
  otherwise valid.
- A successful start changes the tour to `IN_PROGRESS`; its stops begin or remain `PENDING`
  until an outcome is recorded.

**Traceability:** FR-10, FR-11, FR-13; NFR-02.

#### US-28 — Record a stop outcome — P1

**As an assigned crew member, I want to record collected quantities or a not-collected reason at
each stop so that actual execution is documented.**

**Acceptance criteria**

- The assigned driver or either assigned collector can move a `PENDING` stop on an
  `IN_PROGRESS` tour to `COLLECTED` or `NOT_COLLECTED`.
- A collected stop records non-negative actual quantities by waste category; a not-collected
  stop requires a non-blank incident/reason.
- The actor and UTC update time are retained, and map/list stop states update consistently.
- An agent cannot update a stop on an unassigned tour.

**Traceability:** FR-02, FR-13, FR-24; NFR-01, NFR-02, NFR-03.

#### US-29 — Complete a tour — P1

**As an assigned crew member, I want to complete an active tour after resolving every stop so
that the finished work becomes operational history.**

**Acceptance criteria**

- A tour can move from `IN_PROGRESS` to `COMPLETED` only when every stop is `COLLECTED` or
  `NOT_COLLECTED`.
- Completion retains the route, assigned resources, stop outcomes, quantities, incidents, and
  timestamps.
- Ordinary execution updates are rejected after completion; later privileged correction follows
  US-31.

**Traceability:** FR-11, FR-13, FR-14; NFR-02, NFR-05.

#### US-30 — Cancel a tour — P2

**As a planner, I want to cancel a tour before completion so that abandoned work is explicitly
distinguished from completed collection.**

**Acceptance criteria**

- A `DRAFT`, `PLANNED`, or `IN_PROGRESS` tour can move to `CANCELLED`; a completed or already
  cancelled tour cannot be cancelled again.
- The planner supplies a cancellation reason.
- Cancellation retains the tour, any recorded stop outcomes, and its history; it is not a hard
  delete.

**Traceability:** FR-11, FR-14; NFR-02, NFR-05.

#### US-31 — Correct an execution result — P2

**As a planner or administrator, I want to correct an erroneous collection result with a reason
so that the operational record is accurate and the correction remains traceable.**

**Acceptance criteria**

- The privileged user can correct a stop outcome, quantity, or incident through a dedicated
  operation rather than an unrestricted historical edit.
- A non-blank correction reason is required, and the actor and UTC timestamp are recorded.
- The correction creates an audit event; ordinary agents cannot perform privileged corrections.

**Traceability:** FR-13, FR-14, FR-30; NFR-01, NFR-02.

#### US-32 — Consult operational history — P2

**As a planner, I want to search completed and cancelled tours and inspect their results so that
I can review past collection work.**

**Acceptance criteria**

- The planner can filter history by planning date, status, vehicle, agent, and collection point.
- A history detail retains resources, stop order, terminal outcomes, actual quantities,
  incidents, cancellation reason where applicable, and relevant timestamps.
- Historical tours cannot be reopened as new work or hard-deleted through this view.

**Traceability:** FR-12, FR-14; NFR-03, NFR-09.

### 1.7 Epic E06 — Real MN01 data and map context

#### US-33 — Load the reproducible MN01 demonstration — P0

**As an administrator, I want to load reproducible MN01 source extracts and demonstration
operations so that a reviewer can immediately try the real-location workflow.**

**Acceptance criteria**

- A documented local setup/seed command loads the included eligible DSNY MN01 basket extract and
  the MN01G garage as the initial depot, preserving source IDs and snapshot identifiers.
- Real source locations are clearly distinguished from synthetic demands, vehicles, agents,
  users, and collection outcomes.
- Demonstration records include enough categories, resources, points, and tours to exercise the
  main workflow without undisclosed manual data entry.
- Repeating the documented setup does not create ambiguous duplicate demo identities.

**Traceability:** FR-26; NFR-09, NFR-10, NFR-12.

#### US-34 — Display the NYC traffic overlay — P2

**As a planner, I want to display official NYC DOT traffic links with their observation time and
coverage limitations so that I can see available traffic context without confusing it with
routing costs.**

**Acceptance criteria**

- The optional layer displays official traffic-link geometry and available attributes, with a
  visible observation timestamp and required attribution/disclaimer.
- Unsensed streets are not colored or described as having observed traffic status.
- Stale, unavailable, or partial feed behavior is visible to the user and does not corrupt saved
  operational data.
- Toggling or refreshing traffic does not change the directed optimization cost matrix or cause
  automatic rerouting.

**Traceability:** FR-25; NFR-07, NFR-10, NFR-12.

### 1.8 Epic E07 — Dashboards and evaluation CSV exchange

#### US-35 — View the operational dashboard — P2

**As a planner, I want a summary of resources, tours, and collection progress so that I can assess
current operational work at a glance.**

**Acceptance criteria**

- The dashboard summarizes available/total vehicles and agents and tours by lifecycle status.
- It summarizes pending, collected, and not-collected stops for the selected planning period.
- Selecting a summary leads to the corresponding filtered operational view where applicable.
- The dashboard does not imply real-time GPS tracking or live municipal operations.

**Traceability:** FR-07, FR-09, FR-11, FR-13, FR-28; NFR-03.

#### US-36 — Explore retained evaluation comparisons — P2

**As a planner, I want to explore retained scenario comparisons on an evaluation dashboard so
that I can inspect the optimizer's measured impact and reproducibility information.**

**Acceptance criteria**

- The user can select a retained scenario and compare all feasible methods' distance, estimated
  duration, execution time, point coverage, and constraint checks.
- Percentage changes relative to input order are calculated from the retained values and are not
  shown for missing/infeasible results as if they were improvements.
- The detail exposes demand profile/seed and size where applicable, algorithm configuration,
  source snapshot, road graph/routing version, and visit order.
- Aggregate summaries identify the included scenarios and do not mix the separate hand-crafted
  demo into the fixed 60-scenario experiment.

**Traceability:** FR-20, FR-21, FR-28; NFR-04, NFR-06, NFR-07, NFR-09.

#### US-37 — Import an evaluation scenario CSV — P2

**As a planner, I want to import evaluation-scenario points from a documented CSV schema so that
I can reproduce a controlled experiment without manually re-entering every point.**

**Acceptance criteria**

- The schema and a valid example are documented, including point identity/source reference,
  WGS84 coordinates, input order, and synthetic demand category/quantity rows where applicable.
- The import validates headers, data types, coordinate ranges, positive kilogram quantities,
  category references, duplicate point/order entries, and required fields.
- Row-specific errors are reported, and an invalid import does not partially create a scenario.
- Imported values retain their real/synthetic provenance and can be used identically by all three
  planning methods; this story does not import operational tours or general master data.

**Traceability:** FR-15, FR-29; NFR-02, NFR-07, NFR-09, NFR-12.

#### US-38 — Export evaluation results CSV — P2

**As a planner, I want to export retained evaluation results so that I can independently inspect
and analyze the baseline, intermediate, and optimized measurements.**

**Acceptance criteria**

- The export contains one identifiable result per scenario and planning method, including
  infeasible/failed outcomes rather than silently dropping them.
- Columns include scenario and method IDs, point count, seed/profile where applicable, source and
  road/routing snapshots, configuration, visit order, distance, duration, execution time, and
  feasibility/coverage checks.
- Values are sufficient to recalculate reported distance and duration changes relative to input
  order.
- Operational tours, users, vehicles, agents, and waste categories are not part of this CSV
  export.

**Traceability:** FR-20, FR-21, FR-29; NFR-07, NFR-09.

### 1.9 Epic E08 — Auditability

#### US-39 — Record and review audit events — P2

**As an administrator, I want sign-in and data-change events to be recorded and searchable so
that important system activity can be traced to an actor and time.**

**Acceptance criteria**

- Successful and failed sign-in attempts and create/update/status/correction operations produce
  audit events with actor or attempted username, UTC timestamp, action, entity type/identifier
  where applicable, outcome, and concise details.
- Passwords, password hashes, and secret credentials are never written to audit details.
- The administrator can filter events by UTC time range, actor, action, entity, and outcome.
- Ordinary users cannot edit or delete audit entries; field-level before/after diffs are not
  required.

**Traceability:** FR-01, FR-30; NFR-01, NFR-11.

## 2. Scope and traceability checks

The backlog covers FR-01 through FR-30. In particular:

- the single-vehicle optimizer supports multiple independent tours per planning day, not a
  simultaneous multi-vehicle CVRP;
- mixed categories share one vehicle-capacity total, with no compartment model;
- optimization requires planner review and never silently changes an operational tour;
- operational routes use the versioned directed road network, while NYC traffic remains display
  only;
- CSV exchange is evaluation-only, as narrowed by D-028; and
- connected sensors, real-time GPS, automatic traffic-aware rerouting, public registration,
  commercial mapping/routing, a citizen application, and billing remain excluded.

## 3. Sprint plan (tailored for AI-agent execution)

### 3.1 Sprint model

| Aspect | Setting |
|---|---|
| Team | Product owner and reviewer: project owner. Developer and record keeper: the AI coding agent, acting session by session (D-005). |
| Cadence | Scope-bounded sprints with **no calendar dates** (D-006). A sprint ends when every committed story's done checks pass and the review/retrospective notes are written. |
| Selection | Stories are selected in backlog priority order, adjusted by hard dependencies; each sprint states its entrance criteria. |
| Sizing | Relative task complexity (XS/S/M/L), because plan-based elapsed time is meaningless for an AI agent. The task backlog, not a calendar, defines the sprint length. |
| Status | **No sprint has been executed yet.** Every sprint is planned, with review and retrospective slots left empty until the sprint actually closes. |

Differences from a human-team sprint backlog, and why:

- **Self-contained tasks.** Each task names the packages, files, or pages to touch so an
  executor with session-only memory can resume without unstated context. Where a model is
  already fixed, the task cites the governing document instead of restating it.
- **Machine-verifiable done.** Every story carries explicit commands with expected outcomes
  (build, tests, HTTP checks). Perceptual claims ("looks fine") are not accepted as evidence.
- **Contract sync is part of each task.** Changing a backend record changes
  `frontend/src/types/api.ts` in the same commit (D-018).
- **Escalation over invention.** A task that needs an unresolved decision (AGENTS.md §4, e.g.
  extracting the OSM road snapshot) is stopped and raised to the owner instead of guessed.

### 3.2 Agent operating procedure, repeated each sprint

**Start**

1. Re-read `AGENTS.md` (§3 status, §4 open decisions), the latest entries in
   `docs/decisions.md`, this sprint's subsection, and any `docs/01-analysis.md`/
   `docs/03-uml/` sections the sprint cites.
2. Prove a green baseline before the first change: `source ~/.local/opt/ase-env.sh &&
   cd backend && ./mvnw test` and `cd frontend && npm run build`. If either fails, report to
   the owner before starting the sprint.

**Work**

3. Execute the sprint tasks in order; one conventional commit per task
   (e.g. `feat(tours): validate crew assignment`).
4. A story leaves the sprint only when its listed automated checks pass.

**Close**

5. Run the **Review** demo script against both running apps; write the observed results in
   the sprint's review subsection.
6. Write the **Retrospective**: what went well, what went badly, and at least one concrete
   process change for the next sprint.
7. Update `AGENTS.md` §3 and §5, the sprint status table in §3.3, the story status in
   this file, and log any decision in `docs/decisions.md`.

### 3.3 Sprint overview

| Sprint | Theme | Story selection | Status |
|---|---|---|---|
| S1 | Secure foundation: identities, roles, waste categories | US-01…US-07 | Not started |
| S2 | Collection points and demand on the real map | US-08…US-12 | Not started |
| S3 | Fleet, crew, and MN01 import groundwork | US-13…US-17 (+ US-33 groundwork) | Not started |
| S4 | Tour drafts: stops, order, crew, capacity | US-18…US-20 | Not started |
| S5 | Directed routing and the three-method optimizer | US-21, US-22 | Not started |
| S6 | Optimization review, comparison, planning gate | US-23…US-25 | Not started |
| S7 | Agent execution loop and the complete MN01 demo | US-26…US-29, US-33 | Not started |
| S8 | Corrections, history, and auditability | US-30…US-32, US-39 | Not started |
| S9 | Dashboards, evaluation explorer, CSV, traffic overlay | US-34…US-38 | Not started |
| S10 | Metrics campaign and project closure | Part 6 execution + presentation content | Not started |

Notes on the selection:

- **US-05 and US-07 are pulled into S1** although they are P1, because deactivation semantics
  (inactive accounts, inactive categories) must exist before demand entry (S2) can enforce
  "active category" validation. All other epics' small P1 lifecycle stories (US-10, US-12,
  US-16) stay inside their epic's sprint.
- **US-33 is split across S3 and S7.** The MN01 baskets/depot import with provenance and
  snapshot identity must exist before map work, but a *complete* demonstration load (including
  tours in several lifecycle states) requires the whole lifecycle, so the story is closed in
  S7.
- **S5 and S6 split the optimizer** (engine vs. its review UI) so the algorithm core — an
  isolated, heavily testable module per AGENTS.md §7 — is verified before any UI depends on it.
- **S10 has no backlog stories** because it executes Part 6 (metrics + the fixed 60-scenario
  campaign) and prepares presentation content; the deck format remains an open decision
  (AGENTS.md §4).

### 3.4 Sprint S1 — Secure foundation: identities, roles, waste categories

**Sprint goal.** A reviewer can sign in as `ADMIN` or `PLANNER`, manage users and waste
categories within their role, and every protected endpoint enforces authorization on the
backend.

**Entrance criteria.** Green baseline; `SecurityConfig.java` exists as a stub; H2 file mode
configured (D-013).

**Scope notes.**

- Class and enum names follow the class diagram (`docs/03-uml/02-class-diagram.md`):
  `AppUser`, `AppRole`, `WasteCategory`, with `UUID` identifiers.
- The optional `AppUser`–`Agent` login link (US-04 last criterion, D-030) needs the `Agent`
  entity, so it is implemented in S3 with US-16. S1 does not add a link column.
- US-03 is partially verifiable in S1: the role matrix covers user and category administration
  and the `ADMIN > PLANNER` hierarchy. Checks that `AGENT` is refused planning endpoints and can
  only see its own tours are added in the sprints that create those endpoints (S2–S7).
- Sign-in audit events (FR-30) are recorded in S8 (US-39).

**Task backlog**

1. (M) Domain + persistence: `AppUser` (username, password hash, `AppRole` enum, active) and
   `WasteCategory` (name, description, active flag) in `domain/`; Spring Data repositories in
   `repository/`. **Test:** repository round-trip; unique username/unique category name failure.
2. (M) Authentication in `config/` + a `security/` package, following D-038: BCrypt hashing,
   JSON login that stores the security context in a server-side `HttpSession` (session-id
   cookie), cookie-based CSRF token for the SPA, and logout that invalidates the session.
   Endpoints `GET /api/auth/csrf`, `POST /api/auth/login`, `POST /api/auth/logout`,
   `GET /api/auth/me`. Unauthenticated requests get a JSON 401 and forbidden ones a JSON 403
   (no redirects). An account deactivated while signed in is rejected on its next request.
   Bootstrap: the initial `ADMIN` is created by a documented dev-profile seed, never by a public
   endpoint. **Test:** valid login; invalid credentials → generic error; deactivated account
   rejected; login without CSRF token → 403; logout → session rejected; password/passwordHash
   never serialized.
3. (M) User administration API `/api/users` (create with role, activate/deactivate;
   US-04/US-05). **Test:** `PLANNER` and `AGENT` get 403 on all `/api/users` operations;
   `ADMIN` succeeds (role matrix test).
4. (S) Waste-category API `/api/waste-categories` (create, edit preserving identity,
   deactivate; US-06/US-07). **Test:** deactivation excludes category from a lightweight
   "active categories" query later used by demand validation.
5. (M) Frontend foundation: API client with CSRF header handling, auth state held in TanStack
   Query, protected routes, role-aware `AppShell` (nav items per role) under
   `src/components/shared/`, pages `src/pages/login-page.tsx` (`LoginPage`),
   `src/pages/users-admin-page.tsx` (`UsersAdminPage`), `src/pages/waste-categories-page.tsx`
   (`CategoriesPage`); full contract in `src/types/api.ts`. **Check:** `npm run build` passes in
   strict mode.
6. (S) Update README run instructions with the seed accounts; final `./mvnw test &&
   npm run build`.

**Automated done checks**

- `./mvnw test` green, including: role matrix, login failure handling, CSRF enforcement,
  inactive-user and inactive-category rules.
- `npm run build` green.
- `curl` checks against the running backend: CSRF cookie → login → session cookie and
  `GET /api/auth/me` 200; login without CSRF → 403; `GET /api/users` as `AGENT` or `PLANNER` →
  403; a deactivated user's session and new logins → 401; category create/edit/deactivate as
  `ADMIN`, and 403 for `PLANNER`; logout → guarded endpoint returns 401.

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*

### 3.5 Sprint S2 — Collection points and demand on the real map

**Sprint goal.** A `PLANNER` sees the 449 MN01 basket locations as provable places (source
provenance), manages points and demand entries through synchronized list/map UI, and invalid
coordinates or demands are rejected by the backend.

**Entrance criteria.** S1 done; import of MN01 locations may load via a temporary admin
routine or test fixture — the permanent importer is S3's groundwork, so S2 tests may seed
fixture data directly.

**Task backlog**

1. (M) Domain + persistence: `Depot`, `CollectionPoint` (WGS84 `longitude`/`latitude`,
   active, name, sourceDataset/sourceId/sourceType/`real`vs`synthetic` provenance),
   `DemandEntry` (category FK, positive kg) — D-019, D-024.
2. (M) API `/api/collection-points` + `/api/depots`: create, inspect/filter
   (status, category), edit details/coordinates, demand-entry add/edit/remove, deactivate
   (US-08…US-12), keeping ≥1 active demand for active points. **Tests:** coordinate order and
   range validation; positive-kg rule; inactive category rejected on new demand; editing does
   not touch frozen evaluation snapshots (void check until S5); inactive points excluded from
   "eligible" queries.
3. (M) Map foundation per `docs/geospatial-data.md` §5: Leaflet + React Leaflet dependencies
   with visible OSM attribution; `src/components/maps/BaseMap.tsx`, `PointsMapLayer` with
   depot/active/inactive styling, popup inspection, map-click → coordinate capture for the
   point form. **Check:** browser walkthrough in the review; build green.
4. (M) Frontend pages `src/pages/CollectionPointsPage` (list+filters+details) with map/list
   synchronization and non-map fallback for every essential action (US-09 AC). Contract sync
   in `src/types/api.ts`.
5. (S) Final test/build pass and AGENTS/README status updates.

**Automated done checks**

- `./mvnw test` green incl. validation and filter rules above.
- `npm run build` green.
- Browser: filters change visible markers; selecting in list focuses map and vice versa;
  attribution visible.

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*

### 3.6 Sprint S3 — Fleet, crew, and MN01 import groundwork

**Sprint goal.** Vehicles, agents, and agent–account links are managed with availability and
link-uniqueness rules; the MN01 basket/depot import runs idempotently from the included
snapshots with recorded snapshot identity — ready to feed later sprints.

**Entrance criteria.** S2 done; categories exist.

**Task backlog**

1. (M) `Vehicle` (registration, positive kg capacity, availability) and `Agent` (identifier,
   display name, availability) domain + APIs (US-13…US-15, US-17). **Tests:** non-positive
   capacity rejected; unavailable vehicle/agent excluded from usable-resource queries;
   deactivation preserves existing assignments (assignment model arrives in S4 — resources
   only need identity preservation here).
2. (S) Agent↔`AGENT`-account one-to-one link endpoints, `ADMIN` only (US-16, D-030).
   **Tests:** double linking rejected; non-`AGENT` account rejected; unlink preserves both
   records.
3. (M) **US-33 groundwork:** `data-importer` service reading classpath snapshots
   `backend/src/main/resources/data/nyc-mn01-baskets.csv` and `nyc-mn01-depot.csv` (per
   `docs/geospatial-data.md` §3): parse into `Depot`/`CollectionPoint`, preserve source IDs
   and DSNY/other ownership labels (real provenance), compute a row count + checksum snapshot
   identifier, upsert idempotently (natural key = source dataset + source ID). Demo vehicles,
   agents, users, demands, and tours are **deferred to S7** because tours do not exist yet.
   **Tests:** re-run import produces no duplicates and a stable report; snapshot identifier
   retrievable via `GET /api/admin/import/report`.
4. (M) Frontend: fleet and agents pages (tables, forms, availability toggles, link dialog);
   API contract sync.
5. (S) Test/build pass; README notes.

**Automated done checks**

- `./mvnw test` green incl. the rules above.
- `npm run build` green.
- Startup import (dev profile) ingests 449 baskets + depot without duplicates on second run.

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*

### 3.7 Sprint S4 — Tour drafts: stops, order, crew, capacity

**Sprint goal.** A `PLANNER` can create a `DRAFT` tour, add/map-select active points in an
explicit input order, and assign a valid vehicle/crew; over-capacity or invalid assignments
are reported and block the lifecycle.

**Entrance criteria.** S3 done; imported points and available vehicles/agents exist.

**Task backlog**

1. (L) Tour domain per D-025: `Tour` (status enum, planning date, depot FK), `TourStop`
   (ordered, point FK, per-stop outcome fields used in S7). Lifecycle transitions land with
   confirm (S6) and start/complete (S7); this sprint models state and draft persistence.
2. (M) Draft API `/api/tours`: create `DRAFT` (date, depot), add/remove stops (active points
   only, no duplicates), reorder input order, read tour detail (US-18, US-19). **Tests:**
   inactive point rejected; duplicate point rejected; order persists.
3. (M) Assignment API: vehicle + driver + 0–2 collectors, distinctness, availability,
   mixed-category capacity sum report (US-20, D-011/D-014/D-024). **Tests:** over-capacity
   and unavailable-resource rejections; driver/collector role distinctness.
4. (M) Map interaction 4 from `docs/geospatial-data.md`: stop selection from
   `src/components/maps/` into the tour editor + synchronized ordered-stop list/editors.
5. (M) Frontend `src/pages/TourPlannerPage` (create/edit draft; assignment panel; capacity
   indicator). Contract sync.
6. (S) Test/build pass.

**Automated done checks**

- `./mvnw test` green incl. stop/assignment validation matrix.
- `npm run build` green.
- Browser: map-selected stops appear in the list with correct order; removing from either
  side synchronizes.

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*

### 3.8 Sprint S5 — Directed routing and the three-method optimizer

**Sprint goal.** Runs of input-order, nearest-neighbor, and NN + 2-opt over identical frozen
scenarios produce retained, road-following, directed-cost results; failed or infeasible runs
change nothing.

**Entrance criteria.** S4 done; OSM road snapshot not yet in repo — its acquisition is task 1
of this sprint and **may require a tooling decision**, which the agent escalates rather than
guesses (D-021; AGENTS.md §4, §7 "heavy dependencies").

**Task backlog**

1. (M) Provide the versioned Lower-Manhattan road-network snapshot for GraphHopper: add the
   retrieval command and source to `docs/geospatial-data.md` §3, store the artifact outside
   git-tracked vendoring if large (paths documented), and record a checksum/routing version ID.
   Escalate to the owner before adding any new extraction tooling.
2. (L) `routing/` package: GraphHopper import of the snapshot; expose leg geometry, directed
   distance, and base travel time between WGS84 points; surface routing version ID per
   request. **Tests:** directed asymmetry visible (a one-way pair), version corruption/
   missing-snapshot failure is explicit.
3. (L) Directed cost-matrix builder for a scenario (depot + ordered candidate stops),
   retained with the run. **Test:** matrix indices consistent with stop order; missing leg
   → explicit failure (US-22 input).
4. (L) `optimization/` core with **no Spring, repository, or GraphHopper dependencies**
   (AGENTS.md §7): `DirectedCostMatrix` core interface; `InputOrderBaseline`,
   capacity-aware `NearestNeighbor`, and `NearestNeighbor + 2-opt` with directed
   reversed-segment cost evaluation — no symmetric shortcut (D-008, D-022). **Tests:**
   hand-built asymmetric matrices where the symmetric delta shortcut would pick a different
   (wrong) segment; determinism across two runs; feasibility/point-coverage bookkeeping.
5. (M) `EvaluationRun` retention schema + service (order, leg geometry, directed distance,
   estimated duration via configurable service time/speed — D-015 remnants where applicable,
   execution time, feasibility checks, algorithm configuration, scenario/seed/profile where
   applicable, source + routing snapshot IDs).
6. (M) API `POST /api/tours/{id}/evaluate` (+ run retrieval): frozen identical inputs to all
   three methods; run failure leaves the draft untouched (US-21, US-22). **Tests:** failed
   run → draft unchanged, no partial proposal; run detail exposes all retained fields.
7. (S) Test/build pass; `docs/03-uml/` sequence remains the review reference.

**Automated done checks**

- `./mvnw test` green incl. all optimizer/routing tests above.
- A scripted run: evaluate a small imported scenario and fetch the run detail showing three
  method results with snapshot IDs.
- `npm run build` remains green (no UI yet for evaluation).

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*

### 3.9 Sprint S6 — Optimization review, comparison, planning gate

**Sprint goal.** The planner compares baseline/NN/NN+2-opt geometry and indicators, accepts
or rejects the proposal explicitly, and only a fully valid draft becomes `PLANNED`.

**Entrance criteria.** S5 done; at least one retained evaluation run reachable from the UI.

**Task backlog**

1. (M) Endpoint: evaluation run list/detail for the tour; contract sync. **Tests:** retained
   values round-trip.
2. (L) Comparison UI (US-23): distinct map layers per method, ordered stops, road geometry,
   synchronized indicators (distance, estimated duration, execution time, coverage/checks),
   % changes relative to input order; attribution visible; traffic never shown as optimizer
   input.
3. (M) Accept/reject flow (US-24): accept applies visit order + route reference to the
   editable draft; reject changes nothing; acceptance does not start or confirm the tour.
   **Tests:** accept → draft stop order matches proposal and status stays `DRAFT`; reject →
   draft intact; retained run still lists all methods after rejection.
4. (M) Confirm-to-planned lifecycle endpoint (US-25): full revalidation (date, depot, stops,
   coverage, capacity, vehicle, driver, collectors, availability — D-025). **Tests:** invalid
   draft stays `DRAFT` with structured errors; valid confirms; structural edits (stops,
   order, resources) rejected while `PLANNED`.
5. (M) Frontend tour review/confirm screens on `TourPlannerPage`/`TourPage`; stop-lock UI.
6. (S) Test/build pass.

**Automated done checks**

- `./mvnw test` green incl. accept/reject/confirm rules.
- `npm run build` green.
- Browser: comparison layers toggle independently; indicators match the run detail endpoint
  values for the same run.

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*

### 3.10 Sprint S7 — Agent execution loop and the complete MN01 demonstration

**Sprint goal.** Crew members see only their tours, drive the start→stop-outcome→complete
loop correctly, and a documented demo command loads the full MN01 demonstration so a reviewer
can try the workflow immediately.

**Entrance criteria.** S6 done; agent accounts linkable (S3).

**Task backlog**

1. (M) Agent scope enforcement (US-26): `GET /api/tours/mine` (crew-filtered only); tour
   detail includes ordered stops, demands, road geometry, stop states. **Tests:** assigned
   crew sees the tour; other `AGENT` accounts (incl. correctly linked but unassigned) see
   nothing; no map required to read the essentials.
2. (M) Start restriction (US-27): only crew, only from `PLANNED`, rechecking vehicle/agent
   availability and tour validity. **Tests:** recheck rejections; `IN_PROGRESS` stops remain
   `PENDING` until outcomes are recorded.
3. (M) Stop outcome recording (US-28, D-027/D-031): `COLLECTED` with non-negative per-category
   quantities, `NOT_COLLECTED` with non-blank reason, actor + UTC timestamp, crew-only. **Tests:**
   crew matrix; negative quantity rejected; PENDING-only rule; unassigned tour rejected.
4. (M) Completion gate (US-29): all stops terminal required; execution updates rejected
   afterwards (correction belongs to S8). **Test:** incomplete completion rejected; complete
   retains all data and freezes the record.
5. (L) **US-33 completion** — demo load command (dev profile or `tools/` script documented in
   README): synthetic demand entries, vehicles, agents, demo users incl. linked `AGENT`
   accounts, and tours seeded **per D-031/D-025 lifecycle** — e.g. one `DRAFT` with a retained
   evaluation, one `PLANNED`, one `IN_PROGRESS`, one `COMPLETED` with realistic outcomes
   (real provenance kept separate from synthetic). Idempotent, stable demo identities.
   **Tests:** second run → no duplicate demo identities; demo data flagged `synthetic` where
   synthetic.
6. (M) Frontend `src/pages/MyToursPage` + stop action panels; planner-side execution status
   (states visible on tour/map views). Contract sync.
7. (S) Test/build pass; README demo walkthrough section (reviewer path).

**Automated done checks**

- `./mvnw test` green incl. crew authorization and lifecycle gates.
- `npm run build` green.
- Demo command runs twice: no duplicate identities; reviewer can log in as a demo agent and
  execute the prepared tour end-to-end.

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*

### 3.11 Sprint S8 — Corrections, history, and auditability

**Sprint goal.** Cancellation, audited corrections, and searchable history work; the audit
record covers sign-ins and data changes across previously built operations.

**Entrance criteria.** S7 done.

**Task backlog**

1. (S) Cancellation (US-30): reason required; `DRAFT`/`PLANNED`/`IN_PROGRESS` → `CANCELLED`;
   no re-cancel or post-completion cancel; existing outcomes retained. **Tests included.**
2. (L) Audit foundation (US-39): `AuditEvent` domain + `AuditService`; emit on successful/
   failed sign-ins and every create/update/status/correction operation built in S1–S7
   (wire-up checklist over existing services); actor/attempted username, UTC, action, entity,
   outcome, concise details; secrets never logged. **Tests:** representative events from
   each feature area; no password/hash leakage.
3. (M) Privileged correction operation (US-31): dedicated endpoint (not historical edit),
   reason required, actor+UTC recorded, audit event created, `PLANNER`/`ADMIN` only.
   **Tests:** agent gets 403; ordinary agent edits impossible; audit row references the
   correction.
4. (M) History search (US-32): filter completed/cancelled tours by date/status/vehicle/
   agent/point; detail retains outcomes, quantities, incidents, cancellation reason,
   timestamps; no reopen/hard delete. **Tests:** filters return correct subsets.
5. (M) Admin audit review endpoint + page with filters (time range, actor, action, entity,
   outcome); audit rows immutable to all users.
6. (S) Frontend history + audit pages; contract sync; test/build pass.

**Automated done checks**

- `./mvnw test` green incl. audit/correction/history rules.
- `npm run build` green.
- Correction of one demo stop appears both in history and in the audit page.

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*

### 3.12 Sprint S9 — Dashboards, evaluation explorer, CSV exchange, traffic overlay

**Sprint goal.** Operational and evaluation dashboards exist; the fixed 60-scenario
experiment can be generated and run locally; evaluation CSVs import/export per the documented
schemas; the timestamped NYC DOT traffic overlay renders without touching optimizer costs.

**Entrance criteria.** S8 done; retained evaluation runs exist (S5/S6).

**Task backlog**

1. (L) Fixed-matrix experiment support (D-016/D-022): seeded generator sampling real MN01
   DSNY points without replacement (3 profiles × 5 seeds × 4 sizes), per-scenario identity
   (seed/profile/size/source snapshot/routing version), reusing the run retention of S5.
   Batch runner invocable from the backend (documentable command for S10). **Tests:**
   sampled scenarios reproducible; runner produces one retained result per scenario/method
   incl. failure rows.
2. (M) Evaluation-explorer dashboard (US-36): scenario selector, per-method comparisons
   (distance, estimated duration, execution time, coverage/checks), % vs input order from
   retained values (not shown for missing/infeasible as "improvement"), reproducibility
   fields; aggregates scoped to the fixed experiment, hand-crafted demo excluded.
3. (M) Operational dashboard (US-35): resource/tour/stop summaries with filtered-view links;
   no real-time tracking implication. **Tests:** summary math vs seeded demo data.
4. (M) CSV import (US-37): documented schema (`docs/evaluation-csv.md`), strict validation
   (headers, types, coordinate ranges, positive kg, category refs, duplicate
   point/order entries), row-specific errors, atomic failure, provenance retained.
   **Test:** validation matrix incl. partial-failure atomicity.
5. (M) CSV export (US-38): all three methods incl. failures, columns sufficient to recompute
   reported deltas, demo/experiment separation. **Test:** round-trip with an import or
   independent parser check.
6. (L) Traffic overlay (US-34): retrieval of the bounded NYC DOT link snapshot with
   `data_as_of` retention and stale/invalid rejection (per `docs/geospatial-data.md` §4); API
   + Leaflet layer with timestamp, coverage disclaimer text from §6; toggling never changes
   optimization inputs (architecture: separate service/module). **Tests:** stale/invalid
   handling; layer toggle state orthogonal to evaluation.
7. (S) Contract sync; test/build pass; README updates.

**Automated done checks**

- `./mvnw test` green incl. runner determinism and CSV validation/export tests.
- `npm run build` green.
- Demo dashboards render from seeded demo data; explorer lists a small hand-run set of test
  scenarios (the full 60-scenario campaign is S10).

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*

### 3.13 Sprint S10 — Metrics campaign and project closure

**Sprint goal.** Part 6 is *answered with measured data* (tooling executes, reports exist)
and presentation content is ready; the prototype is coherent end-to-end for the reviewer.

**Entrance criteria.** S9 done. **Blocked on open decision:** presentation date and deck
format (PowerPoint vs HTML, AGENTS.md §4) — the agent prepares format-agnostic content and
asks before rendering.

**Task backlog**

1. (L) Execute the fixed 60-scenario campaign (S9 runner) and store all outputs under
   `metrics/` next to the `tools/metrics.sh` reports. Run `bash tools/metrics.sh` for LOC,
   CKJM, PMD, JDepend; keep every number reproducible by a listed command.
2. (M) Write `docs/06-metrics.md` from the real outputs: metric tables pointing at their
   commands, generator parameters, and the research question **answered with the measured
   data**. Do not report placeholder numbers (AGENTS.md §10).
3. (M) Presentation content, deck-agnostic: outline following the six parts, architecture
   diagram, demo script with screenshots, metrics table + key finding, limitations and future
   work. Escalate deck format/date to the owner before rendering.
4. (S) Final sweep: README accuracy, `docs/05-scrum.md` story statuses, backlog
   traceability, `AGENTS.md` §3/§5, decisions log; full regression run (all tests + builds)
   and one scripted end-to-end reviewer walkthrough.

**Automated done checks**

- `bash tools/metrics.sh` produces complete reports; the experiment runner output files exist
  with scenario/method counts (60 scenarios × 3 methods).
- `docs/06-metrics.md` contains only measured values with command references.
- `./mvnw test` and `npm run build` green.

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*
