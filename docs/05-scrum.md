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
**Status:** Done in S1.

#### US-02 — Sign out — P0

**As an authenticated user, I want to sign out so that another person cannot continue using my
session.**

**Acceptance criteria**

- Signing out invalidates the current authentication session or token.
- Subsequent requests that require authentication are rejected until the user signs in again.
- The interface returns to the sign-in view without exposing protected operational data.

**Traceability:** FR-01; NFR-01.
**Status:** Done in S1.

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
**Status:** Partially done in S1 (admin/category enforcement and role hierarchy); planning and tour scoping verified in S2–S7. S2: collection points and depots are PLANNER/ADMIN only, AGENT 403. S3: vehicles/agents PLANNER/ADMIN, links and import ADMIN only, AGENT 403. S4: tours PLANNER/ADMIN, AGENT 403.

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
**Status:** Done (S1; agent link in S3 with US-16).

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
**Status:** Done in S1.

#### US-06 — Create and edit a waste category — P0

**As an administrator, I want to create and edit waste categories so that demand and collection
results use a controlled classification.**

**Acceptance criteria**

- A category has a unique, non-blank name and an optional description.
- The administrator can correct category details without changing the identity retained by
  existing demands and results.
- Demand and actual-quantity values associated with a category are expressed in kilograms.

**Traceability:** FR-03, FR-06; NFR-02.
**Status:** Done in S1.

#### US-07 — Deactivate a waste category — P1

**As an administrator, I want to deactivate an obsolete waste category so that it cannot be used
for new demand while historical records remain understandable.**

**Acceptance criteria**

- A deactivated category cannot be selected for a new or edited demand entry.
- Existing demands, tour results, and evaluation history retain the category reference.
- Deactivation does not delete the category or rewrite historical data.

**Traceability:** FR-03, FR-06, FR-14; NFR-02.
**Status:** Done in S1.

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
**Status:** Done in S2 (import-specific provenance via the S3 importer).

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
**Status:** Done in S2 (map interaction verified by build only, D-040).

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
**Status:** Done in S2; tour demand snapshots verified in S4; evaluation snapshots in S5.

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
**Status:** Done in S2; tour demand snapshots verified in S4; evaluation snapshots in S5.

#### US-12 — Deactivate a collection point — P1

**As a planner, I want to deactivate a collection point so that it is excluded from new tours
without losing its history.**

**Acceptance criteria**

- An inactive point cannot be added to a new tour or evaluation scenario.
- Existing tour and evaluation records retain the point and its historical values.
- The point remains visible when an inactive-point filter is selected and can be distinguished
  from active points.

**Traceability:** FR-04, FR-14, FR-22; NFR-02.
**Status:** Done in S2/S4 (rejected for new tour stops, kept and flagged on existing ones); scenarios in S5.

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
**Status:** Done in S3; identity on tour assignments verified in S4.

#### US-14 — Change vehicle availability — P0

**As a planner, I want to mark a vehicle available or unavailable so that new tour assignments
use only operable resources.**

**Acceptance criteria**

- An unavailable vehicle cannot be assigned to a new tour or confirmed as a valid planned
  resource.
- Availability is rechecked when a tour is confirmed and started.
- Changing availability does not erase or rewrite historical assignments.

**Traceability:** FR-07, FR-10, FR-11; NFR-02.
**Status:** Partially done (S3 availability; S4 rejection on new assignment and kept historical assignments); recheck at confirm/start in S6/S7.

#### US-15 — Create and edit an agent — P0

**As a planner, I want to register and edit operational agents so that I can assign drivers and
collectors to tours.**

**Acceptance criteria**

- An agent has a unique identifier, a non-blank display name, and an availability status.
- The planner can edit agent details without changing the identity retained by existing tour
  assignments.
- Only information needed for assignment and operational follow-up is required.

**Traceability:** FR-09; NFR-02, NFR-11.
**Status:** Done in S3; identity on tour assignments verified in S4.

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
**Status:** Done in S3 for link rules; execution-access consequences verified in S7.

#### US-17 — Change agent availability — P0

**As a planner, I want to mark an agent available or unavailable so that crews contain only
people available for collection work.**

**Acceptance criteria**

- An unavailable agent cannot be assigned as driver or collector on a new tour.
- Availability and crew distinctness are rechecked when a tour is confirmed and started.
- Changing availability does not erase historical assignments or collection results.

**Traceability:** FR-09, FR-10, FR-11; NFR-02.
**Status:** Partially done (S3 availability; S4 rejection on new assignment and kept historical assignments); recheck at confirm/start in S6/S7.

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
**Status:** Done in S4 (draft creation/edit); the PLANNED gate is enforced by confirmation in S6.

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
**Status:** Done in S4 (map/list selection, explicit order); scenario freezing in S5.

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
**Status:** Done in S4 (assignment rules, capacity report); blocking at confirmation in S6.

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
**Status:** Groundwork done in S3 (idempotent MN01 basket/depot import, provenance, snapshot identity); demonstration operations in S7.

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
| Status | **S1–S4 executed; S5–S10 planned.** Review and retrospective slots of S5–S10 stay empty until each sprint actually closes. |

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
| S1 | Secure foundation: identities, roles, waste categories | US-01…US-07 | Done (frontend verified by build and proxy checks only, no browser walkthrough) |
| S2 | Collection points and demand on the real map | US-08…US-12 | Done (frontend verified by build, bundle and proxy checks only, no browser walkthrough) |
| S3 | Fleet, crew, and MN01 import groundwork | US-13…US-17 (+ US-33 groundwork) | Done (frontend verified by build, lint and proxy checks only, no browser walkthrough) |
| S4 | Tour drafts: stops, order, crew, capacity | US-18…US-20 | Done (frontend verified by build, lint, bundle and proxy checks only, no browser walkthrough) |
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
  snapshot identity must exist before tour planning (S4), but a *complete* demonstration load
  (including tours in several lifecycle states) requires the whole lifecycle, so the story is
  closed in S7. S2 shows only a five-basket `dev` sample keyed like the S3 import.
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

**Review.** 2026-11-01.

- `cd backend && ./mvnw test`: `Tests run: 46, Failures: 0, Errors: 0, Skipped: 0` (repository,
  authentication, CSRF, role-matrix, user-administration, category and seed tests).
- `cd frontend && npm run build`: exit 0 (strict TypeScript, Vite). `npm run lint`: no findings in
  own code (three fast-refresh warnings in vendored `components/ui`).
- Scripted HTTP done-check against a freshly started backend (fresh H2 file, `dev` profile).
  Ports 8080 and 5173 were already held by foreign, older dev servers, so the check ran the
  backend on 8081 with a scratch database and a second Vite on 5174 proxying to it; the Vite
  proxy configuration is otherwise the committed one.

| Check | Observed |
|---|---|
| CSRF endpoint and cookie | 200, `{"headerName":"X-XSRF-TOKEN",...}`, cookie `XSRF-TOKEN` set |
| Login `admin` | 200 `{"id","username":"admin","role":"ADMIN"}`, `JSESSIONID` set, no password in body |
| `GET /api/auth/me` with session | 200 |
| Login without CSRF token | 403 |
| Wrong password / unknown user | both 401, identical body `Invalid username or password.` |
| `GET /api/users` anonymous / `PLANNER` / `AGENT` | 401 / 403 / 403 |
| `GET /api/users` as `ADMIN` | 200, 3 users, no `passwordHash` |
| Create user / duplicate / invalid | 201 / 409 / 400 with `fieldErrors` |
| New user logs in | 200 |
| Deactivate user, then existing session / new login | 200, then 401 / 401 |
| Reactivate, login | 200, role `PLANNER` kept |
| Admin deactivates self | 409 |
| Category create / duplicate (case-insensitive) / blank | 201 / 409 / 400 |
| Category rename | 200, same id |
| Category write as `PLANNER`, `AGENT` | 403, 403 |
| Category read as `PLANNER`, `AGENT` | 200, 200 |
| Deactivate category; `?active=true` excludes it, plain list includes it | 200; 0 / 1 matches |
| Unknown category id | 404 |
| Logout, then `/me` and `/api/users` with old cookie | 204, 401, 401 |
| Seed passwords in backend log | 0 matches |
| Same flow through the Vite proxy (csrf, login, me, logout, 401s) | identical results; `/login` serves the SPA root element |

Acceptance criteria: US-01, US-02, US-05, US-06, US-07 are met as far as the API and the build
can show. US-03 is met for the user and category endpoints (role matrix) and the
`ADMIN > PLANNER` hierarchy. US-04 is met except the optional agent link.

Deferred: US-03 checks that `AGENT` is refused planning endpoints and sees only its own tours
(S2–S7, when those endpoints exist); US-04 agent link (S3 with US-16); US-06 kilogram unit is
enforced by the demand model in S2 (the UI only states it); FR-30 sign-in audit events (S8).

Known gaps: the React pages were verified by the strict build and by serving the SPA through
the proxy, not by clicking through them in a browser (D-040). A browser walkthrough of login,
user creation, deactivation and category editing remains for the reviewer. Passwords of dev
accounts are documented development credentials.

**Retrospective.**

- *Went well.* The plan's security design worked with few changes: explicit
  `SecurityContextRepository.saveContext` in the login controller, `PathPatternRequestMatcher`
  bean (no matcher ambiguity appeared), and a per-request active-account filter made
  deactivation immediate. Backend tests passed on the first full run of T3 and T4. The frontend
  built on the first attempt.
- *Went badly.* (1) The `csrf()` test post-processor permanently swaps the token repository of
  the shared `CsrfFilter`, so a cookie-flow test failed depending on test order (header name
  `X-CSRF-TOKEN`, no cookie); it needed its own class with `@DirtiesContext(BEFORE_CLASS)`.
  (2) Some test helper names clashed with static imports (`status`, `put`, `post`) and cost
  three compile rounds. (3) The done-check could not use the planned ports because foreign dev
  servers occupied 8080/5173; this was only noticed after the first start failed.
- *Process changes for S2.* Check `ss -ltnp` for ports 8080/5173 before starting servers and
  prefer an alternate port plus scratch database over killing processes that are not ours; name
  test helpers with a verb prefix that cannot collide with static imports; keep any test that
  depends on the real CSRF cookie flow isolated in its own `@DirtiesContext` class.

### 3.5 Sprint S2 — Collection points and demand on the real map

**Sprint goal.** A `PLANNER` manages collection points and their demand entries through a
synchronized list, form and Leaflet map of Lower Manhattan, sees the real MN01G depot and real
DSNY basket locations with their source provenance, and the backend rejects invalid coordinates,
demands and role violations.

**Entrance criteria.** S1 done (green baseline, categories with activation). The permanent MN01
importer (all 449 baskets, snapshot identity) remains S3 groundwork (D-035). S2 therefore shows
only a small `dev`-profile sample: the MN01G depot and five DSNY baskets copied verbatim from the
included snapshots, keyed by source dataset + source identifier so that the S3 importer upserts
them instead of duplicating them. Tests seed their own fixtures.

**Scope notes.**

- Names follow the class diagram: `Depot`, `CollectionPoint`, `DemandEntry`,
  `CollectionPointStatus {ACTIVE, INACTIVE}`, `DataOrigin {IMPORTED_REAL, SYNTHETIC,
  USER_ENTERED}`; fields `sourceDataset`, `sourceIdentifier`, `sourceOwnershipType`,
  `quantityKg`.
- Every point keeps at least one demand entry (OCL `HasCurrentDemand`), with at most one entry
  per waste category. The S3 importer must therefore give each imported point a synthetic
  demand entry.
- Collection-point and depot endpoints are `PLANNER`-only (and so `ADMIN`); `AGENT` receives
  403 (US-03). Depots are read-only in S2.
- The US-10/US-11 criterion "edits do not alter retained tours/evaluation runs" cannot be tested
  before snapshot entities exist; it is verified in S4 (`StopDemand`) and S5 (evaluation runs).
  US-12 "inactive point cannot join a new tour/scenario" is prepared by an eligible-points query
  here and enforced in S4/S5.
- Frontend files use kebab-case (AGENTS.md §7): `src/components/maps/base-map.tsx` (`BaseMap`),
  `points-map-layer.tsx` (`PointsMapLayer`), `src/pages/collection-points-page.tsx`
  (`CollectionPointsPage`).

**Task backlog**

1. (M) Domain + persistence: enums, `Depot`, `CollectionPoint` (WGS84 `longitude`/`latitude`,
   status, origin, source fields, cascaded demands), `DemandEntry` (category FK, positive
   `quantityKg`, origin) and repositories — D-019, D-024. **Tests:** cascade round-trip; unique
   source key; unique category per point; status/category search.
2. (M) API `/api/collection-points` (list with `status`/`categoryId` filters, get, create with
   ≥1 demand, edit details/coordinates, status change, demand add/edit/remove) and read-only
   `/api/depots`. **Tests:** role matrix incl. `AGENT` 403; longitude/latitude range (400) and
   NYC service-area check that catches swapped coordinates (422); positive kg; inactive category
   rejected (422); duplicate category (409); last demand cannot be removed (422); inactive points
   excluded from the eligible-points query.
3. (S) `dev` seed: two demo categories, the MN01G depot and five DSNY sample baskets (real
   locations, synthetic demand), idempotent. **Test:** seeding twice gives identical counts.
4. (M) Map foundation per `docs/geospatial-data.md` §5: Leaflet + React Leaflet with visible
   "© OpenStreetMap contributors" attribution (D-039); `BaseMap`, `PointsMapLayer` with
   depot/active/inactive styling and layer control, popup inspection, map-click coordinate
   capture.
5. (M) `CollectionPointsPage`: filters, list, details, point form (numeric fields or map click,
   confirmed by Save), demand editor, selection synchronized between list and map, every action
   available without the map (US-09, NFR-03). Contract sync in `src/types/api.ts`.
6. (S) README and status updates; review and retrospective.

**Automated done checks** (D-040: no frontend test framework)

- `./mvnw test` green including the rules above.
- `npm run build` green; the built bundle contains the OSM attribution string.
- Scripted HTTP checks against a backend on a spare port with a scratch database: seed visible,
  filters, validation status codes, role matrix, idempotent restart; same calls through a Vite
  dev server proxying to it.
- Not machine-checked: the visual list↔map synchronisation. It is listed for a reviewer
  walkthrough and is not claimed as verified.

**Review.** 2026-11-01.

- `cd backend && ./mvnw test`: `Tests run: 70, Failures: 0, Errors: 0, Skipped: 0` (46 from S1
  plus 24 new: collection-point repository, service, controller role/validation matrix, depot
  controller, dev map seed).
- `cd frontend && npm run build`: exit 0 (strict TypeScript, Vite; only the >500 kB chunk
  warning, now 793 kB with Leaflet). `npm run lint`: no findings in own code (three fast-refresh
  warnings in vendored `components/ui`).
- Scripted HTTP done-check (`s2-done-check.sh`, 51 checks, 0 failures) against a backend built
  from the branch on spare port 8081 with a scratch H2 file, and a second Vite on 5174 proxying
  to it. The owner's servers on 8080/5173 and `backend/data/` were not used or touched; the
  script ran unmodified.

| Check | Observed |
|---|---|
| Logins `planner`, `admin`, `agent` | 200, 200, 200 |
| Depots as `PLANNER`; content | 200; 1 depot, `MN01G`, `IMPORTED_REAL` |
| Collection points seeded | 200; 5 points, all `IMPORTED_REAL` from `8znf-7b2c`, all demand `SYNTHETIC` |
| Status filter `ACTIVE` / `INACTIVE` | 4 / 1 |
| Category filter `Recyclables` | 2 |
| `?status=FOO` | 400 |
| `AGENT` list / depots / create | 403 / 403 / 403 |
| Anonymous list | 401 |
| `ADMIN` list | 200 |
| Create valid point | 201, origin `USER_ENTERED` |
| Latitude 95 / longitude -200 | 400 / 400 |
| Swapped longitude/latitude | 422 |
| Quantity 0 / empty demands | 400 / 400 |
| Same category twice | 409 |
| Create category, deactivate it, use it in a new point | 201, 200, 422 |
| Edit name and coordinates | 200; GET returns `-74.006`, `40.716` |
| Edit with latitude 95 | 400 (previous values kept) |
| Add demand / duplicate category | 201 / 409 |
| Edit demand / remove demand / remove last demand | 200 / 204 / 422 |
| Deactivate point; in `ACTIVE` list / in `INACTIVE` list | 200; absent / present |
| Unknown id / malformed id | 404 / 400 |
| Restart on the same database: depots / points / demo categories | 1 / 6 (5 seed + 1 test) / 2 (no duplicates) |
| `npm run build`; OSM attribution and `leaflet-container` CSS in bundle | exit 0; both found |
| Same calls through the Vite proxy (login, list, `AGENT` 403) | 200, 200, 403; `/collection-points` serves the SPA root element |
| Dev passwords in the backend log | 0 matches |

Acceptance criteria: US-08 is met (create with demand, source fields, provenance, coordinate and
demand validation). US-09 is met on the API side (list, filters, provenance fields); the list,
map and layer-control UI is built but not browser-verified. US-10 is met (edit name and
coordinates, validation, inactive points editable). US-11 is met (add, edit, remove, duplicate
and last-entry rules, inactive category kept on edit). US-12 is partly met: deactivation,
status filter and the eligible-points query (`findEligibleForPlanning`, unit-tested) exist.

Deferred: US-10/US-11 "edits do not alter retained tours/evaluation runs" (snapshot entities,
S4/S5); US-12 exclusion of inactive points from new tours/scenarios (S4/S5); the full 449-basket
MN01 import (S3); US-03 agent tour scoping (S7).

Known gaps: the list-map synchronisation, map-click coordinate capture and visibility of the
attribution control were verified only by the strict build, bundle inspection and proxy checks,
not in a browser (D-040). Reviewer walkthrough: sign in as `planner`, open Collection points,
set the status filter to Inactive (one point appears), click a list row (the map flies to it),
click a marker (the list row highlights), then New point, Pick location on map, click the map
and Save.

**Retrospective.**

- *Went well.* The planned layering (domain, repository with fetch-joined reads, service
  rules, thin controller) worked as written: `open-in-view: false` caused no lazy-loading error
  because every read goes through `findWithDemandsById` or `search`. The null-tolerant JPQL
  filter query worked on H2 without splitting. The done-check script passed on its first
  run, and the S1 lessons held (verb-prefixed test helpers, scratch ports and database, no
  foreign process touched).
- *Went badly.* (1) `addDemand` first used `saveAndFlush` on the managed point; Spring Data
  merged a copy of the new demand, so the returned entity had no id (`$.id` missing in the
  201 body). It was found by the controller test and fixed with a plain `flush()`. (2) The new
  `DepotControllerTest` first failed with 401 for every role because the per-request active-
  account filter requires the user row to exist; the fixtures now save the users. (3)
  `WasteCategoryServiceTest` also deletes categories, so it needed the same
  `points.deleteAll()` ordering as the controller test; the plan named only the controller
  test. (4) Two lint warnings in own code (a helper exported from a component file, and
  `watch`) were fixed after the first build.
- *Process changes for S3.* Run `./mvnw test` after adding each controller test class, not
  only at task end; when a plan names one test class for FK-cleanup ordering, grep for every
  `deleteAll()` on the parent table before starting; run `npm run lint` together with
  `npm run build` after each frontend task.

### 3.6 Sprint S3 — Fleet, crew, and MN01 import groundwork

**Sprint goal.** A `PLANNER` manages vehicles and operational agents with availability rules, an
`ADMIN` controls the one-to-one agent ↔ `AGENT`-account link, and the included MN01 basket and
depot snapshots are imported idempotently with a recorded snapshot identity, ready to feed tour
planning (S4) and the evaluation scenarios (S5).

**Entrance criteria.** S2 done (green baseline: 70 backend tests, frontend build); categories,
collection points with demand, and the `dev` map sample exist.

**Scope notes.**

- Names follow the class diagram: `Vehicle` (`registration`, `capacityKg`, `status`, `origin`),
  `Agent` (`displayName`, `status`, `origin`), `ResourceStatus {AVAILABLE, UNAVAILABLE}`. The
  login link is the optional one-to-one `AppUser`–`Agent` association (D-030); its foreign key
  lives on `Agent`. The `UUID` is the unique identity required by US-13/US-15; the vehicle
  registration is also unique.
- Vehicles and agents are `PLANNER`/`ADMIN` resources (`AGENT` 403). Linking and unlinking an
  account is `ADMIN` only (US-16). Resources are never deleted, only made unavailable, so
  identities stay stable for the tour assignments that arrive in S4.
- The US-04 agent link deferred from S1 lands here: account creation accepts an optional agent
  to link, and `/api/users` and `/api/auth/me` expose the link.
- **Import semantics (US-33 groundwork).** All 449 basket rows are imported with their source
  ownership label (D-019, `docs/geospatial-data.md` §3). Only rows whose type starts with
  `DSNY ` (238 in the snapshot) are DSNY service obligations. A point is *optimization-eligible*
  when it is `ACTIVE` and has no source ownership label (user-entered) or a `DSNY ` label; the
  eligible-for-planning query, a new `optimizationEligible` response flag and an `eligible`
  list filter apply this rule, so non-DSNY imported points never silently become DSNY service
  obligations. The natural key is source dataset + source identifier (`8znf-7b2c` + basket id,
  `xw3j-2yxf` + `MN01G`). The import **upserts** on that key: an absent row is inserted; for a
  present row only the source-owned, non-editable label (`sourceOwnershipType`) is refreshed,
  and the planner-editable fields (name, coordinates, status, demand) of the five S2 `dev`
  baskets or of any edited point are kept. Every newly inserted point receives exactly one
  `SYNTHETIC` demand entry in the `Mixed litter` category, with a quantity of 10–30 kg derived
  deterministically from the basket id, so the `HasCurrentDemand` rule holds and real locations
  stay distinguishable from synthetic demand. Rows with a missing id, type or unparsable or
  out-of-area coordinates are skipped and reported, not guessed.
- Snapshot identity = source dataset + row count + the first 12 hex digits of the SHA-256 of
  the included file (for example `8znf-7b2c:449:a0737237e370`), persisted per dataset with the
  full checksum in a `SourceSnapshot` provenance record (outside the operational class
  diagram, D-033) so that S5 scenarios can cite it. Demo vehicles, agents, users, richer
  demands and tours are **deferred to S7** (complete US-33 demonstration), because tours do not
  exist yet.
- Frontend routes: `/vehicles` and `/agents` (`PLANNER`/`ADMIN`), `/admin/data-import`
  (`ADMIN`).

**Task backlog**

1. (M) `Vehicle`, `Agent`, `ResourceStatus` domain + repositories + APIs `/api/vehicles` and
   `/api/agents` (create, edit preserving identity, availability change, `status` filter)
   (US-13, US-14, US-15, US-17). **Tests:** non-positive or missing capacity → 400; duplicate
   registration (case-insensitive) → 409; edit keeps the id; unavailable vehicles/agents are
   excluded from the usable-resource queries; role matrix incl. `AGENT` 403; no delete endpoint.
2. (S) Agent ↔ `AGENT`-account link, `ADMIN` only (US-16, US-04 remainder, D-030):
   `PUT`/`DELETE /api/agents/{id}/user-account`, optional `agentId` on account creation, link
   shown in user and current-user responses. **Tests:** double linking of either side → 409;
   non-`AGENT` account → 422; unlink preserves both records; `PLANNER` 403.
3. (M) **US-33 groundwork:** an MN01 import service reading the classpath snapshots
   `data/nyc-mn01-baskets.csv` and `data/nyc-mn01-depot.csv` per the import semantics above; it
   runs at `dev` startup after the map seed and on demand through `POST /api/admin/import`; the
   last report and the persisted snapshot identities are returned by
   `GET /api/admin/import/report` (`ADMIN` only). **Tests:** the SHA-256 values match
   `docs/geospatial-data.md`; a first import into an empty database yields 449 points, 238 of
   them optimization-eligible, 1 depot; a second import creates nothing; after the `dev` sample
   the import creates 444 points and finds 5, whose demand and status stay intact (237 eligible,
   because one sample basket is inactive); non-DSNY labels are preserved and not eligible.
4. (M) Frontend: Vehicles and Agents pages (tables, create/edit dialogs, availability switches,
   `ADMIN` link dialog), the agent link on the Users page, an `ADMIN` Data import page (report +
   re-run), and the eligibility flag/filter on the collection-points page; contract sync in
   `src/types/api.ts`.
5. (S) README notes; test/build pass.

**Automated done checks**

- `./mvnw test` green incl. the rules above.
- `npm run build` and `npm run lint` green (no findings in own code).
- Scripted HTTP checks on a spare port with a scratch database: the `dev` startup import yields
  as many `8znf-7b2c` points as the CSV has rows and as many `DSNY ` points as the CSV has DSNY
  rows (expected values computed from the file by the script); every new point carries one
  synthetic demand; a manual re-import and a restart create no duplicates and keep the snapshot
  identifier; vehicle/agent validation and role matrix; link rules; same calls through a Vite
  proxy.
- Not machine-checked: the visual behaviour of the new pages (D-040).

**Review.** 2026-11-01.

- `cd backend && ./mvnw test`: `Tests run: 120, Failures: 0, Errors: 0, Skipped: 0` (70 from
  S1-S2 plus 50 new: vehicle repository 3, vehicle controller 7, agent controller 6, fleet
  availability service 4, agent-account link controller 10, agent repository 2, CSV reader 5,
  MN01 import service 9, import controller 2, plus one eligibility test each in the
  collection-point controller and service).
- `cd frontend && npm run build`: exit 0 (strict TypeScript, Vite; only the >500 kB chunk
  warning, now 812 kB). `npm run lint`: no findings in own code (three fast-refresh warnings
  in vendored `components/ui`).
- Scripted HTTP done-check (`s3-done-check.sh`, 138 checks, `FAILS=0`) against a backend built
  from the branch on spare port 8081 with a scratch H2 file, and a second Vite on 5174 proxying
  to it. The owner's servers on 8080/5173 and `backend/data/` were not used or touched; the
  script ran unmodified and expected values were computed from the CSV by the script.

| Check | Observed |
|---|---|
| CSV rows / DSNY rows / basket checksum vs. documented | 449 / 238 / `a0737237e370...` (match) |
| Logins `planner`, `admin`, `agent` | 200, 200, 200 |
| Startup report (`ADMIN`): trigger, rows, DSNY rows | 200; `STARTUP`, 449, 238 |
| Startup basket created / existing / skipped / updated | 444 / 5 / 0 / 0 |
| Snapshot ids; depot created / existing / rows | `8znf-7b2c:449:a0737237e370`, `xw3j-2yxf:1:207851c2e229`; 0 / 1 / 1 |
| Synthetic demands; category; snapshots persisted | 444; `Mixed litter`; both datasets with full SHA-256 |
| Report/import as `PLANNER` / `AGENT` / anonymous | 403 / 403 / 401 (POST as `PLANNER`, `AGENT`: 403) |
| `8znf-7b2c` points; ids; labels vs. CSV | 449; ids equal CSV ids; labels equal CSV `baskettype` |
| `DSNY `-labelled points; all `IMPORTED_REAL` | 238; true |
| Every point has demand; new points | true; one synthetic `Mixed litter` of 10-30 kg |
| Sample `10130067` / `10130345` | 2 demands kept / stays `INACTIVE` |
| Non-DSNY `10110163`: label, eligible | `BID-owned Compactor`, false |
| `optimizationEligible` = ACTIVE and (no label or DSNY label) | true for every point |
| `?eligible=true` / `?eligible=false` | 237 (all DSNY, active) / 212 |
| Depots | 1 |
| Planner renames a non-DSNY point and deactivates `10110239`; manual re-import | 200; `MANUAL`, created/existing/updated/skipped `0 449 0 0`, depot created 0, synthetic 0, same snapshot id |
| After re-import: points; demands; rename; deactivation | 449; 451; kept; kept |
| Vehicle create / normalization / case-insensitive duplicate | 201 / `NY-1001 9000 AVAILABLE USER_ENTERED` / 409 |
| Vehicle capacity 0, -5, missing; blank registration | 400, 400, 400; 400 |
| Vehicle `AGENT` create, list / anonymous / `ADMIN` list | 403, 403 / 401 / 200 |
| Vehicle update keeps id, 8500; capacity 0 | 200; 400 |
| Vehicle unavailable: in `AVAILABLE`, in `UNAVAILABLE`; `?status=FOO` | absent, present; 400 |
| Vehicle unknown id / `DELETE` | 404 / 405 |
| Agent create; body; second agent with the same name | 201; `AVAILABLE USER_ENTERED`, no `linkedUser`; 201 |
| Agent blank name; `AGENT` create, list; anonymous list | 400; 403, 403; 401 |
| Agent update keeps id; unavailable, excluded, available again | 200; 200, excluded, 200 |
| Agent `DELETE` | 405 |
| Link as `AGENT` / `PLANNER` / non-`AGENT` account | 403 / 403 / 422 |
| Link missing `userId` / unknown user | 400 / 404 |
| Link; relink same pair | 200 (`linkedUser` `agent`, active); 200 |
| Second agent for same account / second account for linked agent | 409 / 409 |
| Users list shows link; `/me` of `agent` | agent id and name; `agentId` present |
| Unlink as `PLANNER`; unlink; unlink again | 403; 204; 204 |
| After unlink: agent, account, `/me` | kept; kept and active; no `agentId` |
| Create user with `agentId`; `PLANNER` with `agentId` (not created); linked agent again | 201 with link; 422; 409 |
| Restart on same database: report; snapshot; synthetic | `STARTUP`, `0 449 0`; same id; 0 |
| Restart: points, demands, rename; depots, vehicles; agents, link; demo categories | 449, 451, kept; 1, 1; 2, kept; 2 |
| Dev passwords in the backend log | 0 matches |
| `npm run build`; lint outside `components/ui` | exit 0; 0 findings |
| Through the Vite proxy: logins, vehicles, agents, import report | 200, 200, 200, 200, 200 |
| SPA routes `/vehicles`, `/agents`, `/admin/data-import` | serve the root element |

Acceptance criteria: US-13 is met (identity, positive capacity, unique registration). US-14 is
partly met (availability change and usable-vehicle query; the assignment and recheck rules are
S4/S6/S7). US-15 is met. US-16 is met for link rules, role restriction and unlink preservation
(execution access of linked agents is S7). US-17 is partly met, like US-14. US-04 is now fully
met. US-33 groundwork is done (import, provenance, snapshot identity, idempotency); the demo
operations are S7.

Deferred: US-14/US-17 rejection of unavailable resources on new tours and the recheck at
confirm/start (S4, S6, S7); historical-assignment preservation tests once tours exist (S4); the
US-16 access rule for agents without an active linked account (S7); the US-33 demo vehicles,
agents, users, demands and tours (S7).

Known gaps: the new pages were verified by the build, lint and proxy checks only, not in a
browser (D-040). Reviewer walkthrough: (1) as `planner`: Vehicles, New vehicle, Save, toggle
Available; (2) as `planner`: Agents, New agent; (3) as `admin`: Agents, Link account (pick
`agent`); (4) as `admin`: Users shows the linked agent; (5) as `admin`: Data import shows both
snapshots, Re-run import (created 0); (6) Collection points, Planning eligibility "Not
eligible".

**Retrospective.**

- *Went well.* The done-check script passed on its first run (138 of 138) and all 50 new
  backend tests passed on their first run once the code compiled. The role-hierarchy rule
  order in `SecurityConfig` (`/api/agents/*/user-account` before `/api/agents/**`) worked as
  planned and the planner-403 test confirmed it. `DELETE` returned 405 without any extra
  exception handler. The `@Order` 10/20/30 start-up sequence made the five sample baskets be
  absorbed (444 created, 5 existing) with their two demands kept. Checksums computed over
  classpath bytes matched the documented SHA-256 values.
- *Went badly.* (1) A scripted edit of `CreateUserRequest.java` opened the file for writing
  before reading it, which truncated it; the compile error pointed to it immediately and the
  file was rewritten, costing one extra test run. (2) `watch` from react-hook-form produced
  the same lint warning in `users-admin-page.tsx` that S2 had fixed in another form; it was
  replaced by `useWatch` after the first lint run. (3) The first test run of each task
  printed many expected constraint-violation `ERROR` log lines, so success must be read from
  the Surefire summary, not from grepping for `ERROR`.
- *Process changes for S4.* Write scripted file edits with separate read and write steps and
  check `git diff --stat` right after them; when a form needs reactive field values, start
  from `useWatch` instead of `watch`; read test results from the `Tests run:` summary lines
  (`grep "Tests run:" ... | tail -1`).

### 3.7 Sprint S4 — Tour drafts: stops, order, crew, capacity

**Sprint goal.** A `PLANNER` can create a `DRAFT` tour for a planning date and depot, add
eligible points from the map or a list in an explicit input order, and assign a vehicle, a
driver and up to two collectors; capacity excess, unavailable resources and invalid stops are
reported as structural issues that will block the lifecycle (S6), and invalid assignments are
rejected.

**Entrance criteria.** S3 done (green baseline: 120 backend tests, frontend build); the MN01
import (237 eligible points), vehicles and agents with availability exist.

**Scope notes.**

- Names follow the class diagram: `Tour` (`planningDate`, `status`, `origin`, depot, vehicle,
  driver, collectors), `TourStop` (`visitOrder`, `status`), `StopDemand`
  (`plannedQuantityKg`), `TourStatus`, `TourStopStatus`. S4 creates only `DRAFT` tours with
  `PENDING` stops. Lifecycle transitions land with confirm (S6), start/complete (S7) and cancel
  (S8); the attributes they need (`cancellationReason`, `roadLegsGeoJson`, `roadGraphVersion`,
  stop outcome fields, `CollectedQuantity`) are added by those sprints.
- **Stop order** is `visitOrder` 1..n, unique and contiguous (OCL `UniqueContiguousVisitOrder`),
  maintained by the domain: append on add, renumber on remove, explicit permutation on
  reorder. A point appears at most once per tour (409).
- **Demand snapshot.** Adding a stop copies the point's current demand entries into
  `StopDemand` (class diagram §1); later demand edits do not change the stop (US-11 deferred
  item). Name, coordinates and status are read live; S5 freezes coordinates in the evaluation
  scenario (US-10 deferred item).
- **New stops** must be `optimizationEligible` (D-019/D-044): inactive → 422 (US-12 deferred
  item), active non-DSNY → 422. A stop whose point later becomes inactive or ineligible is kept
  and reported.
- **Assignment** (D-011/D-014): one request replaces vehicle, driver and collectors; more than
  two collectors, duplicate collectors, or the driver as collector → 422 (OCL
  `AtMostTwoCollectors`, `DriverIsNotCollector`). A newly placed resource must be `AVAILABLE`
  (422, US-14/US-17 deferred item). A resource already on the tour that later becomes
  unavailable stays assigned (historical assignment preserved) and is reported.
- **Capacity** (D-024): the sum of all planned demand over all categories is compared with the
  single vehicle capacity (equality is within capacity). In a draft, excess is **reported, not
  rejected**, because stops and vehicle can be edited in any order; it is a blocking issue
  (`OVER_CAPACITY`) for confirmation (OCL `CapacityRespectedAfterDraft`, S6).
- **Structural validation** (`Tour.structuralIssues()`, the activity diagram's "Draft inputs
  structurally valid?" decision): missing stops/vehicle/driver, unavailable vehicle/driver/
  collector, inactive or ineligible stop, over capacity. S5 evaluation and S6 confirmation reuse
  it; S6 adds the road-route checks.
- **Revision.** Every successful structural change increments `Tour.revision`, so an S5
  evaluation can record the exact draft version it froze and S6 can refuse a stale proposal.
- The planning date must be today or later when set or changed. The same vehicle or agent may
  serve several tours on one day (D-012). No tour is deleted (405); abandonment is the S8
  cancellation.
- **No road routing in S4.** The map shows the input order as straight dashed lines labelled
  "not a route"; road geometry and distances arrive in S5 (D-021).
- API `/api/tours` (PLANNER/ADMIN, `AGENT` 403). Frontend routes `/tours` and `/tours/:id`.

**Task backlog**

1. (L) Tour domain and persistence: `Tour`, `TourStop`, `StopDemand`, the status enums,
   `structuralIssues()`, `revision`; `TourRepository` with status/date search; `Tour.revision`
   added to the class diagram. **Tests:** pure domain tests (append/renumber, demand snapshot,
   capacity equality and excess, every issue code); repository round trip of order,
   snapshots and collector order; unique point per tour.
2. (M) Draft and stop API (US-18, US-19): create/read/list/edit draft; add stop, remove stop,
   reorder. **Tests:** 201 + `Location`; validation (400/404/422 date); role matrix; inactive
   and non-DSNY points 422, duplicate 409; order persists; renumbering on removal; non-draft
   edits 409; demand edits do not change the stop snapshot; a deactivated stop is kept and
   flagged.
3. (M) Assignment API and capacity report (US-20): `PUT /api/tours/{id}/assignment`.
   **Tests:** valid crew; over-capacity reported and blocking; collector count, distinctness
   and driver/collector separation 422; unavailable new resources 422; unknown ids 404;
   rejections change nothing; unavailable/renamed resources keep their identity on the tour
   (US-13/US-14/US-15/US-17 history); same resources on two tours of one day allowed.
4. (S) Frontend contract, hooks, Tours list page and draft-creation dialog; navigation.
5. (M) Tour planner page with map interaction 4 (`docs/geospatial-data.md` §5): candidate and
   stop layers with numbered stops and an input-order line labelled "not a route", add/remove
   from map popups or a candidate list, an ordered stop list with move up/down/remove,
   assignment panel, capacity indicator and issues panel, all fed by the same tour query.
6. (S) README; test/build pass.

**Automated done checks**

- `./mvnw test` green incl. the domain, stop and assignment validation matrix above.
- `npm run build` and `npm run lint` green (no findings in own code).
- Scripted HTTP checks on a spare port with a scratch database (D-040): role matrix; draft
  creation and validation; stop add/duplicate/inactive/non-DSNY/reorder/remove with persisted
  order; assignment rules, capacity excess and recovery; preserved historical assignments;
  demand snapshot after point edits; list filters; restart persistence; the same calls through
  a Vite proxy; the bundle contains the "not a route" label and the list reorder control.
- Not machine-checked: the visual map/list synchronisation in a browser (D-040); a reviewer
  walkthrough is written in the review.

**Review.** 2026-11-01.

- `./mvnw test`: 150 tests, 0 failures, 0 errors, 0 skipped (120 from S1-S3 plus 30 new:
  tour domain 8, tour repository 3, tour controller 19). `npm run build` exits 0 (only the
  pre-existing >500 kB chunk warning); `npm run lint` reports only the three pre-existing
  warnings in vendored `src/components/ui/`.
- Done-check `s4-done-check.sh` (113 checks), run unmodified: **`FAILS=0`**. It used the jar on
  port 8081 with a scratch H2 database, Vite on 5174 through `API_PROXY_TARGET`; the owner's
  servers on 8080/5173 and `backend/data/` were not touched, and only the processes the
  script started were stopped.

| Check | Observed |
|---|---|
| Role matrix: `AGENT` list / create; anonymous list; POST without CSRF; `PLANNER`, `ADMIN` list | 403 / 403; 401; 403; 200, 200 |
| Create: missing date / missing depot / bad date / yesterday / unknown depot / valid | 400 / 400 / 400 / 422 / 404 / 201 |
| New draft: `Location`; state; vehicle/driver; issues; capacity | `/api/tours/{id}`; `DRAFT USER_ENTERED` revision 0, no stops/collectors; both absent; `NO_STOPS,NO_VEHICLE,NO_DRIVER`; invalid, total 0, no capacity, not over |
| Stops: add P1-P3; orders; planned total of P1; capacity total; revision | 201 each; `1,2,3`; equals the point total (14.0); 61.0 = sum; 3 |
| Stop rejections: duplicate / inactive `10130345` / non-DSNY / unknown point / bad uuid / unknown tour | 409 / 422 / 422 / 404 / 400 / 404; tour unchanged (3 stops, revision 3) |
| Reorder `[P3,P1,P2]`; persisted; incomplete / duplicate / foreign / null list | 200; order kept on re-read; 422 / 422 / 422 / 400; order unchanged |
| Remove P1; again; re-add P1 | 200 with `[P3,P2]` orders `1,2`; 404; 201 at order 3 |
| Assignment `S4-SMALL` (15 kg) + Dana + `[Eli, Finn]`; `S4-BIG` | 200, `overCapacity` true, invalid, last issue `OVER_CAPACITY`, remaining -46.0; roles explicit; 200, valid, 0 issues |
| Crew rejections: 3 collectors / duplicates / driver as collector / `S4-DOWN` / `Una` as driver and collector / unknown vehicle and agent / bad uuid | 422 / 422 / 422 / 422 / 422, 422 / 404, 404 / 400; revision, vehicle, driver and collectors unchanged |
| History: vehicle unavailable; same assignment re-sent; rename; Eli unavailable; all available | same vehicle id with `VEHICLE_UNAVAILABLE`, invalid; 200; same id, `S4-BIG2`, 4000.0; Eli kept with `COLLECTOR_UNAVAILABLE`; valid |
| Point edits: P2 demand +100 kg; P2 deactivated; reactivated and restored | stop planned total unchanged (28.0); stop kept, `INACTIVE`, `INACTIVE_STOP`, invalid; valid again |
| Header edit (date +2); yesterday | 200, revision +1; 422 |
| List: tour present; `?status=DRAFT` / `PLANNED` / `FOO`; `?planningDate` same / other day | `stopCount` 3, vehicle `S4-BIG2`; present / empty / 400; 1 / 0 |
| Second draft same day with the same vehicle and crew; `DELETE` tour; unknown / malformed id | 201 and 200 (D-012); 405; 404 / 400 |
| Restart on the same database: tour; eligible points; tours; dev passwords in log | unchanged (revision, order, vehicle, driver, collectors); 237; 2; 0 matches |
| `npm run build`; lint outside `components/ui`; routing strings in `frontend/src` | exit 0; 0 findings; 0 files |
| Bundle strings `Input order (straight lines, not a route)`, `Move up` | both present |
| Through the Vite proxy: login, `/api/tours`, tour detail; SPA routes `/tours`, `/tours/{id}` | 200, 200, 200; both serve the root element |

Acceptance criteria: US-18 is met (draft with planning date and depot, editable, incomplete
allowed; the "cannot become PLANNED" half is enforced by S6 confirmation using
`structuralIssues()`). US-19 is met (map and list add/remove, no duplicates, explicit editable
order, inactive and ineligible points rejected; the scenario used by all three methods is
frozen in S5 from these stops, snapshots and revision). US-20 is met for the assignment rules
and the capacity report (blocking at confirmation is S6).

Deferred: confirm gate and lock of structural edits (S6, US-25); recheck of availability at
confirm/start (S6/S7); evaluation-snapshot immutability of point edits (S5); agent access to
assigned tours (S7); cancellation of drafts (S8).

Known gaps: the pages were verified by build, lint, bundle strings and proxy checks only, not
in a browser (D-040): the map/list synchronisation and the popup buttons are untested
visually. Reviewer walkthrough: (1) as `planner` create a vehicle and three agents; (2) Tours,
New draft tour; (3) click candidate points on the map, "Add to tour", see the numbers and the
dashed input-order line; (4) add one more from the candidate list; (5) Move up/down and Remove
in the ordered list, the map numbers follow; (6) assign vehicle, driver and collectors, watch
the capacity indicator and the issues panel.

**Retrospective.**

- *Went well.* The done-check script ran unmodified and passed on its first run (113 of 113),
  and all 30 new backend tests passed on their first run except one fixture problem (below).
  The plan's rule orders (404, 409 draft, 422 rules) and the "newly placed resources must be
  available" rule needed no change. `tours.flush()` after `addStop` gave the stop its id for
  the `Location` header, `initialized(...)` on every service return path avoided any lazy
  loading error with `open-in-view: false`, and wiping tours in `@BeforeEach` and `@AfterEach`
  of `TourControllerTest` kept the other Spring test classes free of foreign-key failures.
  The S3 process changes were applied: no scripted edit truncated a file, the form uses
  `useWatch`, and results were read from the `Tests run:` summary.
- *Went badly.* (1) `nonDraftTourIsLocked` first failed with a `LazyInitializationException`
  because the test read the stops of a tour returned by `save()` after a merge; the stop id is
  now taken from the first (persisted) instance. (2) Lint flagged a constant exported from a
  page (`only-export-components`) and `watch`; the labels moved to `lib/tour-labels.ts` and
  `useWatch` was used. (3) Nothing in the planner UI could be seen in a browser, so the
  map/list synchronisation rests on the single-query design and on the build/bundle checks.
- *Process changes for S5.* Export shared constants from `lib/` rather than from page files;
  take ids for test fixtures from the instance that was persisted, not from a merged copy;
  because S5 freezes scenarios from tours, read the tour through `TourService.get(...)`
  (which initializes the collections) instead of the repository.

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
