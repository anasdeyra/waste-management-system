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
**Status:** Done in S1; sign-in attempts audited (S8).

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
**Status:** Done in S1–S8 (role matrix incl. ADMIN-only audit log; agents see only their crew's confirmed tours).

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
**Status:** Done in S2; tour demand snapshots verified in S4; evaluation snapshots verified in S5.

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
**Status:** Done in S2; tour demand snapshots verified in S4; evaluation snapshots verified in S5.

#### US-12 — Deactivate a collection point — P1

**As a planner, I want to deactivate a collection point so that it is excluded from new tours
without losing its history.**

**Acceptance criteria**

- An inactive point cannot be added to a new tour or evaluation scenario.
- Existing tour and evaluation records retain the point and its historical values.
- The point remains visible when an inactive-point filter is selected and can be distinguished
  from active points.

**Traceability:** FR-04, FR-14, FR-22; NFR-02.
**Status:** Done in S2/S4/S5 (rejected for new tour stops, kept and flagged on existing ones, blocking evaluation); a planned tour with a deactivated point cannot start (S7).

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
**Status:** Done in S3/S4/S6/S7 (availability, rejection on new assignment, kept historical assignments, recheck at confirmation and at start).

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
**Status:** Done in S3/S7 (link rules; an unlinked or inactive account has no execution access).

#### US-17 — Change agent availability — P0

**As a planner, I want to mark an agent available or unavailable so that crews contain only
people available for collection work.**

**Acceptance criteria**

- An unavailable agent cannot be assigned as driver or collector on a new tour.
- Availability and crew distinctness are rechecked when a tour is confirmed and started.
- Changing availability does not erase historical assignments or collection results.

**Traceability:** FR-09, FR-10, FR-11; NFR-02.
**Status:** Done in S3/S4/S6/S7 (availability, rejection on new assignment, kept historical assignments, recheck at confirmation and at start).

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
**Status:** Done in S4/S6 (draft creation/edit; PLANNED gate by confirmation).

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
**Status:** Done in S4 (map/list selection, explicit order); scenario frozen for evaluation in S5.

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
**Status:** Done in S4/S6 (assignment rules, capacity report; blocking at confirmation).

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
**Status:** Done in S5 (engine, retention and API); review UI in S6; seeded experiment batches and CSV scenarios reuse the same retention (S9).

#### US-22 — Understand an infeasible or failed optimization — P1

**As a planner, I want a useful explanation when a scenario is infeasible or cannot be routed so
that I can correct the inputs instead of receiving a misleading tour.**

**Acceptance criteria**

- The system identifies the failed validation or unavailable routing input, such as excess
  demand or a missing directed road leg.
- No partial proposal is presented as a valid optimized tour.
- The draft and all previously retained operational/evaluation data remain unchanged.

**Traceability:** FR-16, FR-18; NFR-02, NFR-05, NFR-06.
**Status:** Done in S5/S6 (422 not-ready explanations, retained routing failures, 503 for an unusable snapshot; displayed in the S6 evaluation panel).

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

**Status:** Done in S6 (comparison map layers, indicator table, % changes; verified by build/bundle/HTTP checks, D-040).

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

**Status:** Done in S6 (accept/keep input order with stale refusal; decision retained on the run; audited (S8)).

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

**Status:** Done in S6 (confirmation gate with structured issues; planned tours locked).

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
**Status:** Done in S7 (crew-only list and detail with ordered stops, demands, road route and stop states; verified by tests and HTTP checks, UI by build/bundle, D-040).

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
**Status:** Done in S7 (crew-only start from PLANNED with availability, point and route recheck).

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
**Status:** Done in S7 (COLLECTED with quantities by category / NOT_COLLECTED with reason; actor and UTC time; crew only).

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
**Status:** Done in S7 (completion requires every stop terminal; record frozen; planner corrections in S8).

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
**Status:** Done in S8 (cancellation with reason from DRAFT, PLANNED or IN_PROGRESS; record retained; audited).

#### US-31 — Correct an execution result — P2

**As a planner or administrator, I want to correct an erroneous collection result with a reason
so that the operational record is accurate and the correction remains traceable.**

**Acceptance criteria**

- The privileged user can correct a stop outcome, quantity, or incident through a dedicated
  operation rather than an unrestricted historical edit.
- A non-blank correction reason is required, and the actor and UTC timestamp are recorded.
- The correction creates an audit event; ordinary agents cannot perform privileged corrections.

**Traceability:** FR-13, FR-14, FR-30; NFR-01, NFR-02.
**Status:** Done in S8 (dedicated PLANNER/ADMIN correction with reason; previous values retained; audited).

#### US-32 — Consult operational history — P2

**As a planner, I want to search completed and cancelled tours and inspect their results so that
I can review past collection work.**

**Acceptance criteria**

- The planner can filter history by planning date, status, vehicle, agent, and collection point.
- A history detail retains resources, stop order, terminal outcomes, actual quantities,
  incidents, cancellation reason where applicable, and relevant timestamps.
- Historical tours cannot be reopened as new work or hard-deleted through this view.

**Traceability:** FR-12, FR-14; NFR-03, NFR-09.
**Status:** Done in S8 (history of completed and cancelled tours filtered by date, status, vehicle, agent and point; read-only detail).

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
**Status:** Done in S3/S7 (idempotent MN01 import with provenance; idempotent synthetic demonstration load at dev start and POST /api/admin/demo).

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
**Status:** Done in S9 (committed timestamped NYC DOT snapshot; display-only overlay with observation time, coverage note, attribution and disclaimer; never a cost input).

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
**Status:** Done in S9 (resource, tour and stop-progress summaries for a planning period with links to filtered views).

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
**Status:** Done in S9 (explorer of retained runs and batches with per-method comparisons, reproducibility fields and per-batch aggregates; demo excluded from aggregates).

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
**Status:** Done in S9 (documented CSV schema; strict atomic validation with row errors; three-method evaluation of imported scenarios).

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
**Status:** Done in S9 (one row per run and method including failures; values sufficient to recompute changes).

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
**Status:** Done in S8 (sign-in and data-change audit through one service component; ADMIN search by time, actor, action, entity and outcome; append-only).

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
| Status | **S1–S9 executed; S10 planned.** Review and retrospective slots of S10 stay empty until the sprint actually closes. |

Differences from a human-team sprint backlog, and why:

- **Self-contained tasks.** Each task names the packages, files, or pages to touch so an
  executor with session-only memory can resume without unstated context. Where a model is
  already fixed, the task cites the governing document instead of restating it.
- **Machine-verifiable done.** Every story carries explicit commands with expected outcomes
  (build, tests, HTTP checks). Perceptual claims ("looks fine") are not accepted as evidence.
- **Contract sync is part of each task.** Changing a backend record changes
  `frontend/src/types/api.ts` in the same commit (D-018).
- **Escalation over invention.** A task that needs an unresolved decision (AGENTS.md §4, e.g. a
  new heavy dependency not covered by a decision) is stopped and raised to the owner instead
  of guessed.

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
| S5 | Directed routing and the three-method optimizer | US-21, US-22 | Done (no UI by plan; verified by tests, scripted HTTP checks and an independent matrix-based verifier) |
| S6 | Optimization review, comparison, planning gate | US-23…US-25 | Done (frontend verified by build, lint, bundle strings and proxy checks only, no browser walkthrough) |
| S7 | Agent execution loop and the complete MN01 demo | US-26…US-29, US-33 | Done (frontend verified by build, lint, bundle strings and proxy checks only, no browser walkthrough) |
| S8 | Corrections, history, and auditability | US-30…US-32, US-39 | Done (frontend verified by build, lint, bundle strings and proxy checks only, no browser walkthrough) |
| S9 | Dashboards, evaluation explorer, CSV, traffic overlay | US-34…US-38 | Done (frontend verified by build, lint, bundle strings and proxy checks only, no browser walkthrough) |
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
  campaign) and prepares presentation content; the deck format is fixed by D-041.

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

**Entrance criteria.** S4 done (green baseline: 150 backend tests, frontend build). The
road-snapshot approach is fixed by D-037 (Overpass OSM XML extract, embedded GraphHopper), so no
tooling decision is pending. The planner's spike (2026-11-01) retrieved the extract (4 148 737
bytes, SHA-256 `e1f2fabaa937…341d6a01`, version `osm-mn01-e1f2fabaa937`), imported it with
GraphHopper 11.1 under the Spring Boot 3.5 dependency set without conflicts (the full suite
stayed green), measured an import of ≈2 s, a 101×101 directed matrix in ≈2 s, and directional
legs of 312.6 m versus 797.3 m between two real baskets.

**Scope notes.**

- **Snapshot and version.** The extract is committed under
  `backend/src/main/resources/routing/` with its Overpass query; its SHA-256 is checked at first
  use and forms the routing version ID (`osm-mn01-` + 12 hex). A refresh is a new version. The
  GraphHopper graph cache is derived, git-ignored data (`backend/routing-cache/`).
- **Package boundaries (AGENTS.md §7).** `optimization/` imports only `java.*` and consumes the
  `DirectedCostMatrix` interface (enforced by a test); `routing/` alone imports GraphHopper and
  implements `DirectedCostMatrixProvider`; `service/EvaluationService` connects them. Class
  names follow the sequence diagram (`TourService.loadDraftAndBuildScenario`,
  `EvaluationService`, `DirectedCostMatrixProvider`, `TourOptimizationModule`).
- **Methods (D-008, D-022).** Input order; capacity-aware nearest neighbor (ties to the lowest
  input index); NN followed by best-improvement 2-opt whose move value is the exact directed
  change, re-costing every reversed segment. All three make one trip from and back to the depot
  and visit every stop once; capacity and coverage are checked identically for each.
- **Costs.** One GraphHopper `car` request per ordered pair; every point must snap within 100 m;
  distances and base times are rounded to 3 decimals before optimization so the retained matrix
  reproduces every indicator. Duration = travel time + 600 s per stop (D-015, D-021). Traffic
  is not an input.
- **Freezing and failure semantics (US-21, US-22).** Only a DRAFT without structural issues is
  evaluated (409/422, nothing retained). Names, coordinates and demand are frozen in the run, so
  later point edits do not alter it (US-10/US-11 deferred item). An unroutable point or missing
  leg is retained as a `ROUTING_FAILED` run with an explanation and no method results; an
  unusable snapshot answers 503. The draft is never written by an evaluation.
- **API.** `POST /api/tours/{id}/evaluations` (201 + `Location`), `GET
  /api/tours/{id}/evaluations`, `GET /api/evaluations/{runId}`, `GET
  /api/evaluations/{runId}/matrix`; PLANNER/ADMIN only. No evaluation UI (S6); the TypeScript
  contract gains the types only.

**Task backlog**

1. (S) Versioned snapshot: commit the extract and query, `tools/fetch-road-snapshot.sh`,
   `.gitignore` for the graph cache, `docs/geospatial-data.md` §3.1 (query, date, size,
   checksum, version ID, ODbL) and §4 routing rules.
2. (L) `optimization/` core: `DirectedCostMatrix`, `PlanningScenario`, `TourOptimizationModule`
   with the three methods. **Tests:** hand-built asymmetric matrices where the symmetric delta
   accepts a worsening move or picks a different move; expected orders/distances for a
   three-method example; capacity infeasibility for every method; tie rule; determinism;
   brute-force 2-optimality on random asymmetric matrices; package independence.
3. (L) `routing/`: embedded GraphHopper 11.1 (checksum check, version ID, cache per version,
   lazy init, `dev` warm-up), snapping limit, directed legs with geometry. **Tests:** a one-way
   pair of real baskets differs by direction; far point not routable; checksum mismatch,
   missing and corrupt snapshot fail explicitly.
4. (M) Directed cost-matrix builder (depot + ordered stops). **Tests:** indices follow node
   order; missing leg and unroutable point fail explicitly with node indices; real-snapshot
   matrix is directed.
5. (M) `EvaluationRun` retention (frozen stops, matrix, per-method order, leg GeoJSON,
   distance, duration, execution time, checks, configuration, scenario/seed/profile columns for
   S10, source and routing snapshot IDs) and `EvaluationService`. **Tests:** round trip of a
   completed run; retained routing failure; nothing retained when routing is unavailable.
6. (M) API and contract types. **Tests:** role matrix; real three-method evaluation; draft
   unchanged; retrieval; 422 for not-ready and over-capacity drafts; retained `ROUTING_FAILED`
   for an unroutable point; frozen values after point edits; determinism; 404/400.
7. (S) README, sequence-diagram alignment (201, 503, retained routing failure); test/build pass.
8. (S) Review, retrospective and status updates.

**Automated done checks**

- `./mvnw test` green incl. all optimizer, routing, matrix, retention and API tests above.
- Scripted HTTP checks on spare ports with a scratch database and an empty scratch routing cache
  (D-040): evaluate a six-point real MN01 tour and fetch the run detail showing three method
  results with routing and source snapshot IDs; an independent verifier recomputes every
  distance, duration, the nearest-neighbor order and the 2-optimality of NN + 2-opt from the
  retained matrix; 422 and `ROUTING_FAILED` cases leave the draft unchanged; runs survive a
  restart; a backend with a wrong snapshot checksum answers 503 and retains nothing.
- `npm run build` and `npm run lint` remain green (no UI for evaluation yet).

**Review.** 2026-11-01.

- `./mvnw test`: **195 tests, 0 failures, 0 errors, 0 skipped** (150 from S1-S4 plus 45 new:
  optimization core 16, routing 7, matrix provider 5, evaluation service 6, evaluation API 11).
  `npm run build` exits 0 (only the pre-existing >500 kB chunk warning); `npm run lint` shows the
  three pre-existing warnings in vendored `src/components/ui/` and nothing else.
- Done-check `s5-done-check.sh`: 116 checks, final line `FAILS=0` on the second run (see
  deviations in D-046 for the three script corrections). Backend on 8081 with a scratch H2 and an
  empty scratch routing cache, a second backend on 8082 with a wrong `expected-sha256`, Vite on
  5174. The owner's servers (8080, 5173), `backend/data/` and `backend/routing-cache/` were not
  touched.
- Spike facts (planner, 2026-11-01): extract 4 148 737 bytes, SHA-256 `e1f2fabaa937...341d6a01`,
  version `osm-mn01-e1f2fabaa937`; first import incl. CH preparation about 2 s, cache load about
  0.55 s; 101x101 directed matrix 1.4-2.3 s; directional evidence 312.6 m versus 797.3 m between
  two real baskets; maximum snap distance 21.5 m. Observed by the done-check: the graph
  (3 411 nodes, 4 467 edges) was ready about 1.9 s after the MN01 import line on the empty cache
  and about 0.3 s after it on the restart, with one `Road graph ... ready` line per start. Run A
  (six real eligible points, 5000 kg vehicle): input order `[1,2,3,4,5,6]` 4350.0 m / 4156.4 s /
  0.324 ms; nearest neighbor `[2,1,6,5,4,3]` 3585.0 m / 4059.6 s / 0.032 ms; NN + 2-opt
  `[2,1,6,5,4,3]` 3585.0 m / 4059.6 s / 0.489 ms (2-opt accepted no move on this small tour).

| Group | Check | Observed |
|---|---|---|
| Static | snapshot checksum, size, query file, fetch script syntax, git-ignored cache | `e1f2fabaa937...341d6a01`, 4148737, present, `bash -n` 0, ignored |
| Static | no framework imports in `optimization/`; GraphHopper only in `routing/`; dependency 11.1 with XML exclusion | 0 offending imports, 0 files outside `routing/`, yes, yes |
| Warm-up and cache | graph built by the `dev` warm-up; per-version cache directory | one `Road graph osm-mn01-e1f2fabaa937 ready` line; `osm-mn01-e1f2fabaa937-gh11.1/graph` exists |
| Roles | AGENT, anonymous (with CSRF token), no CSRF, run detail | 403, 401, 403, AGENT 403 / anonymous 401; tour unchanged afterwards |
| Small real-point tour | evaluate six stops | 201 + `Location`; `COMPLETED`; tour id + revision 7; `TOUR_DRAFT`, creator `planner` |
| Small real-point tour | version, engine, snapshot IDs, scenario | `osm-mn01-e1f2fabaa937`; `GraphHopper 11.1`; depot + basket snapshot IDs of the import report; 6 points, 600 s, 5000 kg; stop 1 = P1; depot `MN01G`; matrix 7 nodes |
| Small real-point tour | independent verifier on the retained matrix | all true: methods in order, feasible, identity input order, permutations, distances and travel times, duration formula, legs, NN recomputed, **2-optimal by brute force**, NN + 2-opt <= NN, asymmetric pairs, zero diagonal |
| Retrieval and determinism | detail = POST body; run list; second run | identical; 1 run then 2 newest first; identical orders and distances |
| Not ready (US-22) | no vehicle/driver; tiny vehicle | 422 naming `NO_VEHICLE`/`NO_DRIVER`; 422 naming `OVER_CAPACITY`; 0 runs retained; tour unchanged |
| Unroutable point | Queens point | 201 `ROUTING_FAILED` / `POINT_NOT_ROUTABLE`, names the stop and `limit 100 m`, no results, matrix 404, run retrievable, tour unchanged |
| Frozen scenario | rename and move P1 after the run | run A still shows the original name and longitude |
| Ids | unknown tour/run/runs-of-tour; malformed id; ADMIN | 404, 404, 404; 400; 201 |
| Restart | same DB and cache | graph loaded again (2 ready lines), run A identical, same version, orders and distances; no dev passwords logged |
| Bad checksum | backend on 8082 | warm-up only warns; tour API 200; evaluation 503 naming the checksum mismatch; nothing retained; tour unchanged |
| Frontend | `api.ts`, build, lint, no routing calls, Vite 5174 | evaluation types present; build 0; no lint findings outside `components/ui`; 0 references; login, run detail and run list 200 |

- **US-21 met.** The three methods run on identical frozen inputs from and to the depot and visit
  every point once; 2-opt re-costs reversed segments without the symmetric shortcut (unit tests
  with hand-built asymmetric matrices and the brute-force check of the verifier); orders, legs,
  indicators, execution time, checks, configuration and snapshot IDs are retained; a failed run
  never touches the draft.
- **US-22 met.** Excess demand and other structural issues are explained with 422; an unroutable
  point or missing leg is retained as `ROUTING_FAILED` with a named stop and no partial
  proposal; a missing or corrupt snapshot gives 503; the draft and earlier runs are unchanged.
- **Deferred.** Comparison UI, accept/reject and confirmation (S6, US-23 to US-25); the
  evaluation explorer, CSV export and the 60-scenario campaign (S9/S10, reusing `EvaluationRun`
  with `demandProfile`/`seed`); the route reference on the tour (`roadLegsGeoJson`/
  `roadGraphVersion`, S6 acceptance).
- **Known gaps.** No evaluation UI yet (by plan); the `car` profile approximates a collection
  truck; leg costs follow GraphHopper's fastest-path weighting while the optimizer minimizes
  distance (documented in `docs/geospatial-data.md` §4); runs are not audited yet (S8); the Mermaid
  change to the sequence diagram was not rendered with a tool, only kept syntactically identical.

**Retrospective.**

- *Went well.* The planner's spike removed the integration risk: GraphHopper 11.1, the snapshot
  and the Spring dependency set worked as specified, and every task compiled and passed its
  tests on the first run. The independent verifier recomputing every indicator from the retained
  matrix caught nothing, which is evidence that the retained numbers are self-consistent.
- *Went badly.* Three of the 116 done-check assertions were wrong in the script rather than in
  the code (anonymous POST without a CSRF token yields 403 before authentication; the real depot
  name is `MN01G Garage, 353 Spring St`; the warm-up warning of the second backend is logged
  after login, so a one-shot grep raced). A plan test said `nodeCount` 4 for a five-node fixture.
  On the six-point demo tour 2-opt found no improvement, so it did not demonstrate itself there;
  its value is shown only by the unit tests and later by the S10 campaign.
- *Process changes for S6.* Poll logs instead of grepping once; derive expected fixture values
  from the live data; keep a done-check assertion for an improving 2-opt case in S6/S10.

### 3.9 Sprint S6 — Optimization review, comparison, planning gate

**Sprint goal.** The planner compares the input-order, nearest-neighbor and NN + 2-opt road
routes and indicators on a map and in an accessible table. The planner then explicitly accepts
the proposal or keeps the input order. Only a fully revalidated draft with a current road route
becomes `PLANNED`, and its structure is then locked.

**Entrance criteria.** S5 done (green baseline: 195 backend tests, frontend build, three lint
warnings in vendored `components/ui`). The S5 run list, detail and matrix endpoints exist, and no
decision is pending. Planner fixture (2026-11-01): eight real MN01 baskets (`10130311`,
`10130009`, `10110472`, `10130156`, `10130147`, `10110073`, `10130335`, `10130346`, 171 kg of
synthetic demand) give an input order of 11 736.15 m, nearest neighbor 10 211.798 m, and
NN + 2-opt 7 470.433 m. This is a real case where 2-opt improves on NN (S5 retrospective).

**Scope notes.**

- **Route reference on the tour (class diagram `roadLegsGeoJson`, `roadGraphVersion`).** A draft
  receives a road route only from a retained, completed evaluation run of its *current*
  revision:
  - **accept** applies the NN + 2-opt order and its legs;
  - **keep input order** (the rejection of the proposal) keeps the order and attaches the
    input-order legs of the same run.

  The tour also stores the run id, the method, the distance and the duration. Any change to the
  stops, the order or the depot clears the route. Crew, vehicle and date edits keep it.
- **Stale proposals.** Accept and reject require `run.tourRevision == tour.revision` and the same
  stops in the same input order; otherwise they return 409 and change nothing. Both decisions are
  recorded on the run (`decision`, `decidedBy`, `decidedAt`), and all three method results stay
  retained. Both are tour mutations, so they increment `revision` (D-045).
- **Confirmation gate (D-025, activity diagram, OCL `Tour::confirm()`).**
  `POST /api/tours/{id}/confirmation` revalidates everything and refuses with 422 plus a
  structured `issues` list. It checks the S4 structural issues (`NO_STOPS`, `NO_VEHICLE`,
  `NO_DRIVER`, the vehicle/driver/collector `*_UNAVAILABLE` codes, `INACTIVE_STOP`,
  `INELIGIBLE_STOP`, `OVER_CAPACITY`), plus `PLANNING_DATE_PAST`, `ROUTE_MISSING`, and
  `ROUTE_OUTDATED` (a stop or the depot moved since the route's run). A valid draft becomes
  `PLANNED`. The existing S4 draft-only rule then locks the stops, order, crew, header,
  evaluation, accept and reject (409).
- **No traffic, no new routing call.** Route geometry comes from retained runs only (D-021). The
  traffic overlay remains S9.
- **UI (D-020, map interaction 6).** The tour planner page gains the following:
  - an evaluation panel with a run selector, the routing-failure and 422/503 explanations
    (US-22), and a stale badge;
  - a comparison map with one toggleable road-route layer per method and the stop numbers of a
    selected method;
  - an indicator table showing order, distance, duration and changes from input order,
    execution time and checks;
  - accept and keep-input-order actions, and a confirmation panel with the server's issues;
  - a lock notice for `PLANNED` tours.

  The planner map draws the attached road route.
- **Deferred.** Audit events for accept/reject/confirm go to S8 (US-39). Agent access to planned
  tours goes to S7. Cancellation of drafts or planned tours goes to S8.

**Task backlog**

1. (M) Domain: route reference and confirmation fields on `Tour`, the new issue codes, and
   `confirmationIssues(today)`/`confirm()`. Proposal decision on `EvaluationRun`. Class diagram
   attributes. **Tests:** route cleared by stop/order/depot changes and kept by
   crew/date edits; issue order; `PLANNING_DATE_PAST`; `confirm()` only from `DRAFT`.
2. (M) Accept/reject API (US-24): `POST /api/tours/{id}/proposal-acceptances` and
   `/proposal-rejections` with `{evaluationRunId}`, and the decision in run DTOs. **Tests:** accept
   → order equals the NN + 2-opt point order, route attached, still `DRAFT`, revision + 1, run
   `ACCEPTED` with three results; reject → order unchanged, input-order route, run `REJECTED`
   with three results; stale 409 for both, with nothing changed; accepted twice 409; routing-failed
   run 422; run of another tour 422; 400/404; role matrix.
3. (M) Confirmation API (US-25): `POST /api/tours/{id}/confirmation`, `confirmationIssues` and
   `readyToConfirm` in `TourResponse`, and `issues` in `ApiError`. **Tests:** 422 for each
   blocking issue (route missing, route outdated, over capacity, vehicle/driver/collector
   unavailable, no vehicle/driver, inactive stop, past date) with the tour unchanged; valid
   confirm → `PLANNED` with the route kept; every structural edit, evaluation, accept, reject
   and second confirm → 409.
4. (S) Frontend contract and data layer: `api.ts`, `use-evaluations.ts` (run list, detail,
   evaluate), accept/reject/confirm mutations, and method labels and indicator formatters in
   `src/lib/`.
5. (L) Comparison UI (US-22, US-23): evaluation panel, comparison map layers with toggles,
   indicator table, failure/stale explanations, and attribution plus the
   "traffic is not an optimizer input" note.
6. (M) Review and confirm UI (US-24, US-25): accept / keep-input-order actions, confirmation
   panel with server issues, lock notice, attached route on the planner map, and the route
   column in the tours list.
7. (S) Documentation: sequence diagram (accept/reject endpoints, 409 stale), activity diagram
   (keep-input-order route, confirmation checks) and class diagram attributes, all re-rendered
   with Mermaid CLI from the scratchpad; README; `docs/geospatial-data.md` §5. Then a full
   test/build pass.
8. (S) Review, retrospective and status updates.

**Automated done checks**

- `./mvnw test` green, including the domain, accept/reject and confirmation tests above.
- `npm run build` and `npm run lint` green (no findings in own code).
- Scripted HTTP checks on spare ports with a scratch database and routing cache (D-040). The
  fixed eight-basket tour is evaluated, and the run shows NN + 2-opt shorter than NN and NN
  shorter than the input order. A stale accept or reject returns 409 and changes nothing. The
  accept applies the proposal order and route. Keep-input-order on a second tour attaches the
  input-order route. Each blocking issue returns 422 with its code. A valid confirmation gives
  `PLANNED`. Every edit of the planned tour returns 409. The route and status survive a
  restart. Accept/confirm work through the Vite proxy.
- Bundle strings of the comparison, review and lock UI are present. The sequence, activity and
  class diagrams render with Mermaid CLI (exit 0, no syntax error).
- Not machine-checked: the visual layer toggling in a browser (D-040). A reviewer walkthrough
  is written in the review.

**Review.** 2026-11-01.

- `./mvnw test`: **225 tests, 0 failures, 0 errors, 0 skipped** (195 from S1-S5 plus 30 new: tour
  domain 8, review API 9, confirmation API 13). `npm run build` exits 0 (only the pre-existing
  >500 kB chunk warning); `npm run lint` shows the three pre-existing warnings in vendored
  `src/components/ui/` and nothing else.
- Done-check `s6-done-check.sh`: 173 `PASS` assertions, final line `FAILS=0` on the first run, no
  script change. Backend on 8081 with a scratch H2 and an empty scratch routing cache, Vite on
  5174. The owner's servers (8080, 5173), `backend/data/` and `backend/routing-cache/` were not
  touched.
- Mermaid: the class, sequence and activity diagrams render with `@mermaid-js/mermaid-cli` 11
  (installed in the session scratchpad, not a repo dependency): exit 0, SVG written, no "syntax
  error", re-run inside the done-check. This closes the S5 known gap "not rendered with a tool".
  The first render of the sequence diagram failed because the replacement block left one extra
  `end`; it was fixed before the docs commit.
- Fixture facts (real MN01 baskets, eight eligible points, 171 kg synthetic demand, road graph
  `osm-mn01-e1f2fabaa937`, run 1 of the done-check):

  | Method | Visit order | Distance | Duration | Execution time |
  |---|---|---|---|---|
  | `INPUT_ORDER` | `[1,2,3,4,5,6,7,8]` | 11736.150 m | 6313.2 s | 0.494 ms |
  | `NEAREST_NEIGHBOR` | `[1,5,7,4,8,6,2,3]` | 10211.798 m | 6051.5 s | 0.033 ms |
  | `NN_PLUS_2_OPT` | `[1,5,7,4,8,3,6,2]` | 7470.433 m | 5756.0 s | 0.785 ms |

  Changes versus the input order: nearest neighbor -13.0 % distance / -4.1 % duration; NN + 2-opt
  -36.3 % / -8.8 %. NN + 2-opt is shorter than NN on real data here, unlike the S5 six-point tour.

| Group | Check | Observed |
|---|---|---|
| Static / UML | no traffic or routing engine in `optimization/`; GraphHopper only in `routing/`; diagrams name the review endpoints, the kept input-order route and the route attributes; three diagrams render | 0 offending files; yes; yes; exit 0 and no syntax error for all three |
| Roles and bad requests | AGENT accept/reject/confirm; anonymous with CSRF token; no CSRF; `{}`, malformed run id, unknown run/tour, malformed tour id | 403 x3; 401; 403 x2; 400 x2; 404 x2; 404 / 400; tour unchanged |
| New draft | eight-basket draft, confirm without route | `confirmationIssues` = `ROUTE_MISSING`, not ready, no `route`; 422 with issue `ROUTE_MISSING` and message prefix; tour unchanged |
| Evaluation (2-opt improvement) | run 1 orders, distances, ordering | orders above; 11736 / 10212 / 7470 m; NN + 2-opt < NN < input order; run 1 undecided |
| Stale | reorder after run 1, then accept and reject | 409 "changed since this evaluation" twice; tour unchanged; run 1 undecided with 3 results |
| Accept | run of another tour; accept run 2; accept again | 422 "belongs to another tour"; 200 `DRAFT`, revision + 1, stops in the NN + 2-opt point order, route `NN_PLUS_2_OPT`, run 2, graph `osm-mn01-e1f2fabaa937`, 9 legs, indicators equal to the run; ready to confirm; run `ACCEPTED` by `planner` with 3 results; 409 "already accepted" |
| Keep input order | reject on a second tour, then confirm | 200, same stop ids in order, `DRAFT`, revision + 1, route `INPUT_ORDER`; run `REJECTED` with 3 results; confirm 200 `PLANNED` |
| Blocking issues | `OVER_CAPACITY`, `VEHICLE_UNAVAILABLE`, `DRIVER_UNAVAILABLE`, `COLLECTOR_UNAVAILABLE`, `NO_VEHICLE` + `NO_DRIVER`, `INACTIVE_STOP`, `ROUTE_OUTDATED`, `ROUTE_MISSING` after adding a stop | each 422 with its code in `issues` and in `confirmationIssues`; tour `DRAFT`, unchanged; ready again after repairs; confirm after re-evaluation and accept 200 `PLANNED` |
| Lock | confirm tour A, then add, remove, reorder, assignment, header, evaluation, accept, reject, confirm | `PLANNED`, revision + 1, route kept, not ready, no issues; nine 409s; tour unchanged; listed under `?status=PLANNED` with its route method; run 1 retained, undecided |
| Routing-failed run | Queens point; accept and reject; not-ready evaluation | `ROUTING_FAILED`; 422 "has no proposal" twice; tour unchanged; 422 |
| Restart | same database | tour A identical, run 2 still `ACCEPTED`, no dev passwords logged |
| Frontend / bundle / proxy | `api.ts`, hooks, lat/lng flip, build, lint, bundle strings, Vite 5174 | S6 types and endpoints present; flip only in `geo.ts`; build 0; no lint findings outside `components/ui`; 19 required strings present and the two outdated sentences absent; login, tour detail with route, run list, evaluate 201, accept 200 (CSRF through the proxy), confirm 200 `PLANNED`, SPA route serves the root element |

- **US-23 met**, within D-040's limit: layers, toggles, the indicator table and the percentage
  changes exist in the bundle and read the same run detail the API returns, but they were not
  viewed in a browser.
- **US-24 met.** Accept applies the order and the road route to the draft, which stays `DRAFT`;
  keep-input-order leaves the order and attaches the input-order route; stale proposals return
  409; all methods stay retained.
- **US-25 met.** Full revalidation with structured issues, `DRAFT` to `PLANNED`, structural edits
  answer 409.
- **US-22** UI half met. The S4 deferred items are done: US-18/US-20 "cannot become PLANNED" and
  the US-14/US-17 recheck at confirmation; the recheck at start is S7.
- **Deferred.** Audit events of accept/reject/confirm (S8, US-39); agent view and start of
  planned tours (S7); cancellation of planned tours (S8); the evaluation explorer and CSV (S9);
  the 60-scenario campaign (S10).
- **Known gaps.**
  - No browser walkthrough (D-040).
  - The stop/depot move check compares coordinates only (names and demand snapshots do not change
    the route).
  - `PLANNING_DATE_PAST` is covered by tests only.
  - A planned tour whose point is later deactivated stays `PLANNED` (the recheck at start is S7).
- **Reviewer walkthrough.**
  1. As `planner`, open a draft with stops and crew.
  2. Route evaluation, then `Evaluate routes`.
  3. Toggle the three layers with `Show on map` and choose the numbers.
  4. Read the indicator table and the visit-order lists.
  5. `Accept NN + 2-opt proposal`: the stop list and map numbers follow and the solid road route
     appears.
  6. Confirmation, then `Confirm as planned`: the lock notice appears and the edit controls
     disappear.
  7. On another draft, `Keep input order (reject proposal)` and confirm.
  8. Reorder a stop after an evaluation: the run shows `Stale` and the buttons explain why.

**Retrospective.**

- *Went well.* The planner's fixture (eight real baskets) gave an improving 2-opt case, so the
  done-check now shows NN + 2-opt 36.3 % shorter than the input order, closing the S5 process
  item. All 225 tests and all 173 done-check assertions passed on the first run, with no script
  correction. Rendering the diagrams with a tool was part of the sprint and caught one real
  error.
- *Went badly.* The Mermaid sequence diagram did not parse after the scripted replacement: the
  plan's block boundary left one extra `end`, which only the renderer revealed. The plan quoted
  "about 120 checks" for the done-check while the script prints 173 assertions. The UI was again
  verified without a browser.
- *Process changes for S7.* Render every changed diagram immediately after editing it, before the
  docs commit; count the done-check assertions from the script output instead of quoting an
  estimate; plan a browser walkthrough for S7 if the owner can supply one.

### 3.10 Sprint S7 — Agent execution loop and the complete MN01 demonstration

**Sprint goal.** Crew members see only their own tours and drive the start → stop outcome →
complete loop with server-side revalidation, and a reproducible, idempotent MN01 demonstration
gives a reviewer real locations, synthetic operations and tours in every lifecycle state at the
first `dev` start.

**Entrance criteria.** S6 done (green baseline: 225 backend tests, frontend build, three lint
warnings in vendored `components/ui`). Agent accounts are linkable (S3); planned tours carry a
road route (S6). No decision is pending. The planner verified on 2026-11-01 that the four demo
tours route on the S6 build (draft: the eight-basket S6 fixture; planned, in-progress and completed:
five, five and four other real DSNY baskets).

**Scope notes.**

- **Crew and visibility (US-26, US-03, US-16, D-027, D-030).** The crew is the driver plus the
  collectors. An `AGENT` account sees a tour only when its linked agent is in the crew and the
  tour is not a `DRAFT`. An unknown tour answers 404; a tour that exists but is not visible
  answers 403; an unlinked account gets an empty list and 403; a deactivated account gets 401.
  Agents read point names and coordinates only through their tours. The execution endpoints are
  `AGENT`-only (`ADMIN` inherits `PLANNER`, not `AGENT`); planners follow execution read-only and
  correct results in S8.
- **Endpoints.** `GET /api/tours/mine`, `GET /api/tours/mine/{id}`,
  `POST /api/tours/{id}/start`, `POST /api/tours/{id}/stops/{stopId}/outcome`,
  `POST /api/tours/{id}/completion`, all answering the tour (200) and locking it pessimistically.
- **Start gate (US-27, US-14, US-17, US-12).** Only from `PLANNED`. The start rechecks the S4
  structural issues (vehicle, driver and collectors available, points active and eligible,
  capacity) and the road route (`ROUTE_MISSING`, `ROUTE_OUTDATED` when a point or the depot moved).
  A refusal is 422 with the structured issues and changes nothing. There is no date check, so a
  planned tour can be started on a later day. Nothing is rechecked after the start.
- **Stop outcomes (US-28, D-031).** Only a `PENDING` stop of an `IN_PROGRESS` tour. `COLLECTED`
  needs at least one non-negative quantity (two decimals) per distinct waste category; any active
  category or a category planned on the stop is accepted. `NOT_COLLECTED` needs a non-blank reason
  and no quantities. The actor (account) and the UTC time are kept. Body errors are 400, a second
  outcome is 409, and duplicate or inactive unplanned categories are 422.
- **Completion (US-29).** Only from `IN_PROGRESS` and only when every stop is terminal (422 with
  one `STOP_PENDING` issue per pending stop). The route, crew, stops, outcomes and timestamps are
  kept, and every later execution or planning call answers 409. Correction is S8 (US-31).
- **Demonstration (US-33).** A `dev` start-up runner (after the MN01 import) and an ADMIN endpoint
  `POST /api/admin/demo` (report: `GET /api/admin/demo/report`) load the same synthetic
  demonstration, gated by `wastems.demo.enabled` (on in `dev`, off otherwise):
  - categories `Recyclables` and `Organics` and four extra demand entries;
  - vehicles `DEMO-01…03`;
  - the accounts `collector` and `agent2` (plus the existing `agent`);
  - five agents named `… (demo)`, one without login and one unavailable;
  - four tours on real MN01 baskets: a `DRAFT` with a retained three-method evaluation, a
    `PLANNED` tour, an `IN_PROGRESS` tour with two outcomes, and a `COMPLETED` tour.

  Every demo record has a stable key (`DemoRecord`). It is created once, existing data with the same
  natural key is reused and never modified, and tours only advance forward toward their target
  state. A reload therefore never duplicates anything and never resets what a reviewer did.
  Conflicts with existing data become warnings in the report, never a failed start.
- **Synthetic marking (FR-26, NFR-12).** Demo vehicles, agents, tours and demand entries have the
  `SYNTHETIC` origin. Agent names end with `(demo)`, category descriptions and outcome reasons say
  "synthetic", and the README lists the demo accounts. Real basket and depot locations keep their
  source ids.
- **UI (D-020, D-040).**
  - Agent pages *My tours* (list with progress and role) and the tour page, with a map coloured by
    stop state and the road route, an accessible ordered stop list, start (with server issues),
    per-stop outcome forms and complete.
  - Planner side: stop states and outcomes on the planner page and map, an *Execution* card, and
    progress plus a *Synthetic demo* badge in the tours list.
  - Admin: a *Demonstration data* card.
- **Deferred.** Audit events of execution and of the demo load (S8, US-39); cancellation (S8,
  US-30); corrections (S8, US-31); history search (S8, US-32).

**Task backlog**

1. (M) Domain: `Tour` start/complete fields and rules, `TourStop` outcome fields,
   `CollectedQuantity`, `STOP_PENDING`, class diagram attributes and OCL `Tour::start()` point and
   route preconditions. **Tests:** start/complete guards, outcome guards, issue order.
2. (M) Agent scope (US-26): `GET /api/tours/mine` and `/mine/{id}`, crew and visibility rules,
   `SecurityConfig` AGENT matchers above the PLANNER rule, and execution fields in the tour, stop
   and summary responses. **Tests:** crew sees, draft hidden, other or unlinked agents denied,
   essentials without the map, role matrix, ordering.
3. (M) Start (US-27): `POST .../start` with the revalidation gate. **Tests:** driver and collector
   start, each blocking issue (inactive point, vehicle, driver or collector unavailable, moved
   point) 422 with nothing changed, past date allowed, 409 when not planned, crew/role matrix.
4. (M) Stop outcomes (US-28): `POST .../stops/{stopId}/outcome`. **Tests:** collected with
   quantities, not collected with reason, every 400/404/409/422 case, crew matrix, the planner sees
   the same outcomes.
5. (S) Completion (US-29): `POST .../completion`. **Tests:** pending stops 422, completion keeps
   everything, frozen afterwards, role matrix.
6. (L) **US-33 completion**: `DemoRecord`, `DemoDataService`, start-up runner, admin endpoint,
   `application-dev.yml`. **Tests:** every lifecycle state with the expected facts, second load
   idempotent, reviewer progress not reset, tolerant of existing dev data (warnings, no crash),
   refused before the MN01 import, role matrix, disabled outside `dev`.
7. (L) Agent views: *My tours*, the execution tour page (map, list, start, outcome forms, complete),
   navigation and home texts.
8. (M) Planner execution status and the admin *Demonstration data* card.
9. (S) Activity diagram and use-case notes, rendered diagrams, `docs/geospatial-data.md` §5, README
   (execution curls and **Try the demo**: accounts, real versus synthetic, the four tours, what to
   click); full test/build pass.
10. (S) Review, retrospective and status updates (D-048).

**Automated done checks**

- `./mvnw test` green, including the crew authorization, start, outcome, completion and demo tests.
- `npm run build` and `npm run lint` green (no findings in own code).
- Scripted HTTP checks on spare ports with scratch databases and a scratch routing cache (D-040).
  - An owner-like database is created with the main build and then upgraded: the demo loads with
    warnings and without failing, the existing data is unchanged, and a restart creates nothing.
  - A fresh start shows the demo counts and the four states.
  - `agent`/`agent123` runs the whole loop. Other agents are denied; each start blocker is refused
    with its code; outcome validation and completion rules apply; the completed tour is frozen.
  - An unlinked account and a deactivated account are refused, and so is an unlinked agent.
  - An idempotent restart keeps the executed tour completed.
  - Bundle strings are present, and the agent flow works through the Vite proxy.
- The use-case, class and activity diagrams render with Mermaid CLI (exit 0, no syntax error).
- Not machine-checked: the visual map and forms in a browser (D-040). A reviewer walkthrough is
  written in the review.

**Review.** 2026-11-01.

- `./mvnw test`: **271 tests, 0 failures, 0 errors, 0 skipped** (225 from S1-S6 plus 46 new: tour
  domain 9, agent access 8, start 9, outcomes 8, completion 5, demo 6 + 1 import-controller test).
  `npm run build` exits 0 (only the pre-existing >500 kB chunk warning); `npm run lint` shows the
  three pre-existing warnings in vendored `src/components/ui/` and nothing else.
- Done-check `s7-done-check.sh`: 191 `PASS` lines (counted with `grep -c '^PASS'`), final line
  `FAILS=0` on the first run, no script change. Backend on 8081 with scratch H2 databases and
  scratch routing caches, Vite on 5174. The owner's servers (8080, 5173), `backend/data/` and
  `backend/routing-cache/` were not touched (the owner's database file keeps its earlier
  modification time). Phase A5 ran (it did not `SKIP`): a copy of the owner's database file was
  upgraded by the S7 jar, loaded the demo at the first start (`categories created=1, existing=2`,
  `tours created=4`, `warnings=0`) and created nothing at the second.
- Mermaid: the use-case, class and activity diagrams render with `@mermaid-js/mermaid-cli` 11
  (session scratchpad, not a repo dependency): exit 0, SVG written, no "syntax error", re-run inside
  the done-check. Each changed diagram was rendered right after its edit.
- Demo facts observed (fresh database, `dev` start): start-up report `STARTUP`, categories 1
  created / 2 existing, demand entries 4/0, vehicles 3/0, accounts 2/1, agents 5/0, tours 4/0, no
  warnings, tour states `DRAFT PLANNED IN_PROGRESS COMPLETED`.
  - Draft (8 baskets): one retained run by `demo-loader`, undecided, 11736 / 10212 / 7470 m.
  - Planned (5 baskets): NN + 2-opt route 2386 m, 6 legs, visit order `10130144 10130005 10130062
    10130055 10130038`, run `ACCEPTED` by `demo-loader`; `10130062` has Mixed litter 17, Recyclables
    6, Organics 4.
  - In progress (5 baskets, 2749 m): started by `agent`; stop 1 (`10130118`) collected, Mixed litter
    10.00 and Organics 5.00; stop 2 not collected with the synthetic reason; three stops pending.
  - Completed (4 baskets, 6114 m): completed by `agent2`; collected totals 24.00, 14.40, 16.00 kg;
    the last stop not collected.

| Check | Observed |
|---|---|
| Static / UML | no traffic or routing engine in `optimization/`; GraphHopper only in `routing/`; AGENT matchers above the PLANNER tours rule; class and activity diagrams name the new attributes and rules; demo enabled in dev only; README documents the demo; three diagrams render | all PASS (0 offending files; yes; exit 0 and no syntax error for all three) |
| Owner-like DB upgraded from S6 (A1-A4) | S6 jar creates `DEMO-01` (user-entered 900 kg), `Owner Ann` linked to `agent`, PLANNER `agent2`, inactive basket and category, a confirmed tour; S7 jar with `--wastems.demo.enabled=false`; demo enabled; restart | `Demo load disabled`, POST 409 naming the property, report 404, owner tour readable; start-up not failed, vehicles 2/1, accounts 1/2, tours 4/0, states `DRAFT PLANNED PLANNED PLANNED`, warnings name `DEMO-01`, `'agent'`, `'agent2'`, `10130005`, `Recyclables` and exactly two "no crew member"; planned demo tour has 4 stops; owner tour, links, role and the single `DEMO-01` unchanged; 5 tours; restart `0/3 0/2 0/3 0/3 0/5 0/4`, same ids |
| Owner DB copy (A5) | S7 jar twice on a copy of `backend/data/wastems.mv.db` | ran; demo loaded, no failure; second start `tours created=0` |
| Fresh demo state | start-up report, planner list, vehicles, agents, accounts, draft, planned, in-progress, completed | counts and states above; four `SYNTHETIC` tours; `DEMO-01…03` synthetic, `DEMO-03` unavailable; five `(demo)` agents, Eli unavailable, Dana unlinked; facts of the list above |
| Agent scope and denials | `agent` `/mine`, detail, draft, planner endpoints; `agent2`, `collector`, planner, admin, anonymous, no CSRF | in-progress, planned, completed and no draft; draft 403; planner endpoints 403; `agent2` sees in-progress and completed, planned denied 403 x2; `collector` sees the planned tour only; planner and admin 403 x3; anonymous 401 x2; no CSRF 403 |
| Start gate | inactive point, unavailable vehicle, unavailable collector, moved point, start, start again | 422 `INACTIVE_STOP`, `VEHICLE_UNAVAILABLE`, `COLLECTOR_UNAVAILABLE`, `ROUTE_OUTDATED`, each leaving the tour unchanged; ready again after repair; 200 `IN_PROGRESS` by `agent` with all stops pending; second start 409 |
| Outcomes | validation, lookups, rules, stop 1 and 2, `agent2` | ten 400 checks with the field names; unknown stop, other tour's stop, unknown category 404; duplicate and inactive unplanned category 422; stop 1 collected by `agent`, again 409; stop 2 not collected by `collector`; `agent2` 403 |
| Completion | complete with pending stops, then resolve and complete, then frozen | 422 `STOP_PENDING` x3, tour unchanged; stop 3 with three categories (one `0`), stops 4-5; planner sees the same outcomes; 200 `COMPLETED` by `agent` with route, crew, outcomes and start kept; outcome, start, complete and three planner edits 409 |
| Account rules | unlinked `loner`, deactivated account, unlink and relink Blair | `[]` and 403 "not linked"; 401; `collector` loses and regains the tour |
| Idempotent restart | restart; manual `POST /api/admin/demo` | created 0 everywhere, same four ids, executed tour stays `COMPLETED`, counts unchanged, no new evaluation, no dev passwords logged; POST 200 `MANUAL` created 0; planner and agent 403 |
| Frontend / bundle / proxy | `api.ts`, hooks, lat/lng flip, build, lint, 18 bundle strings, two outdated sentences, Vite 5174 | S7 types and endpoints present; flip only in `geo.ts`; build 0; no lint findings outside `components/ui`; all required strings present and both outdated sentences absent; agent login, `/mine`, start and outcome with CSRF through the proxy 200, admin demo report 200, SPA routes `/my-tours` and `/my-tours/{id}` serve the root element |

- **US-26 met** (crew-only list and detail with ordered stops, demands, road geometry and stop
  states; unassigned and unlinked callers denied), within D-040's limit for the map: the layers and
  colours exist in the bundle and read the same tour the API returns, but were not viewed in a
  browser.
- **US-27 met** (crew only, `PLANNED` only, availability, point and route recheck, stops stay
  `PENDING`).
- **US-28 met** (collected quantities by category, not-collected reason, actor and UTC time, crew
  only, `PENDING` only).
- **US-29 met** (all stops terminal; the record is frozen afterwards; corrections are S8).
- **US-33 met** (documented, reproducible, idempotent demo; real versus synthetic marked), within
  D-040's limit for the UI card.
- **Deferred items closed:** US-03 agent scoping, US-14/US-17 recheck at start, US-16 execution
  access, and the S6 gap "a planned tour whose point is later deactivated stays `PLANNED`" (now
  blocked at start).
- **Deferred.** Audit events of start, outcome, completion and of the demo load (S8, US-39);
  cancellation (S8, US-30); privileged corrections (S8, US-31); history search (S8, US-32);
  dashboards (S9).
- **Known gaps.**
  - No browser walkthrough (D-040).
  - No recheck after the start (by design, D-048).
  - The demo dates are the day of the first load.
  - On a database where the `agent` account is already linked elsewhere, the demo tours that need a
    linked crew stay `PLANNED` (reported as warnings).
- **Reviewer walkthrough.** The seven steps of README §4.6.

**Retrospective.**

- *Went well.* All 271 tests and all 191 done-check assertions passed on the first run, with no
  script correction and no `fix(...)` commit. The pre-checks in the demo's transaction blocks (no
  service call that may throw inside a `TransactionTemplate`) avoided the rollback-only trap
  completely, and the crowded-database phase proved that start-up survives conflicting data. Each
  diagram was rendered right after its edit, as the S6 retrospective asked.
- *Went badly.* The UI was again verified without a browser (build, lint, bundle strings and
  proxy checks). The first run of the demo test showed the draft tour got no evaluation, because the
  forward-only guard skipped a tour already at its target; the guard now makes DRAFT and
  IN_PROGRESS targets re-checkable.
- *Process changes for S8.* Add the audit events of S7's start, outcome, completion and demo load
  together with the S8 operations; plan a browser walkthrough only if the owner can supply one;
  keep counting the done-check assertions from the script output.

### 3.11 Sprint S8 — Corrections, history, and auditability

**Sprint goal.** Planners cancel tours with a reason, correct recorded outcomes through an audited
operation that keeps the previous values, and search the history of completed and cancelled
tours; one service-level audit component records every sign-in and every data change built in
S1–S8, and administrators search that append-only log.

**Entrance criteria.** S7 done (green baseline on `main` `a5edee9`: 271 backend tests, frontend
build, three lint warnings in vendored `components/ui`). Tours execute end to end (S7). No decision
is pending: D-025, D-027, D-029, D-031 and D-038 fix the lifecycle, the correction authority, the
audit scope and the session model.

**Scope notes.**

- **Items deferred to S8 by earlier sprints, all closed here.** Sign-in audit events (S1, FR-30);
  audit events of user, category, collection-point/demand, vehicle, agent and account-link changes
  (S1–S3) and of the MN01 import; audit of draft edits and evaluation runs (S4, S5), of
  accept/reject/confirm (S6), of start, outcomes, completion and the demo load (S7); US-03 audit
  administration; cancellation of drafts (S4), planned (S6) and in-progress tours (S7); planner
  corrections of locked outcomes (S7).
- **Audit (US-39, D-029).** An append-only `AuditEvent` (actor or attempted username, UTC time,
  action, entity type and id, outcome, concise details) outside the D-033 class diagram. One
  `AuditService` is called by the mutating services after a successful change, inside the same
  transaction, so a refused or rolled-back request leaves no event; the actor is the signed-in
  user, or `system` for start-up runners. The catalogue has 38 actions (D-049), from `SIGN_IN`
  (success and failure) and `SIGN_OUT` to `STOP_OUTCOME_CORRECTED`. The MN01 import and the demo
  load each add one summary event; start-up seeding of the dev accounts and map sample is not
  audited. Passwords, hashes and tokens never appear in an event. The only API is
  `GET /api/admin/audit-events` (filters: UTC time range, actor, action, entity type and id,
  outcome; paged, newest first) and `GET /api/admin/audit-events/{id}`, ADMIN only; no endpoint or
  repository method edits or deletes an event.
- **Cancellation (US-30).** `POST /api/tours/{id}/cancellation` with a non-blank reason moves a
  `DRAFT`, `PLANNED` or `IN_PROGRESS` tour to `CANCELLED` and records who, when and the previous
  status; a completed or cancelled tour answers 409. Stops (pending ones stay `PENDING`), outcomes,
  crew and route are kept, and every later edit or execution call answers 409. The crew keeps
  seeing a cancelled tour that had been planned or started, never a cancelled draft.
- **Correction (US-31, D-027).** `POST /api/tours/{id}/stops/{stopId}/corrections` (PLANNER and
  ADMIN; agents 403) changes a recorded outcome of an `IN_PROGRESS`, `COMPLETED` or `CANCELLED` tour
  with a required reason. The same outcome rules as S7 apply; an unchanged outcome is 422 and a
  pending stop 409. An `OutcomeCorrection` retains the previous and new status, incident text and
  quantities with actor and UTC time, the stop keeps its original recorder, and an audit event names
  the correction. A stop never returns to `PENDING`.
- **History (US-32).** `GET /api/tours/history` lists only completed and cancelled tours, filtered
  by planning-date range, status, vehicle, agent (driver or collector) and collection point,
  newest first; the existing tour detail is the read-only history detail (no reopen, no delete).
- **UI (D-020, D-040).** Planner: *Cancellation* card and dialog, cancellation facts, a *Correct*
  dialog per recorded stop, corrections listed under each stop (also for agents), a *History* page.
  Admin: an *Audit log* page with filters and paging. Verified by build, lint, bundle strings and
  proxy checks only.

**Task backlog**

1. (M) Audit foundation: `AuditEvent` + enums, append-only repository with a paged filter query,
   `AuditService` (actor rule, truncation, sign-in/out helpers). **Tests:** actor and `system`,
   truncation, filters and paging, no delete method.
2. (L) Audit wire-up over S1–S7: sign-in success/failure (`AuthController`), sign-out
   (`LogoutHandler`), and every create/update/status/link/import/evaluation/accept/reject/confirm/
   start/outcome/completion/demo operation in its service. **Tests:** representative events per
   feature area, refused requests leave no event, no password or hash in any event, import and
   demo summaries.
3. (M) Domain: cancellation fields and `Tour.cancel`, crew visibility, `OutcomeCorrection` with
   retained previous/new values, in-place quantity correction; class diagram and OCL. **Tests:**
   cancel guards and retained record, correction history and guards, no-change detection.
4. (S) Cancellation endpoint and response fields; crew visibility of cancelled tours. **Tests:**
   each open state, 409 for terminal tours, 400s, roles, frozen afterwards, crew visibility, audit.
5. (M) Correction endpoint (US-31). **Tests:** previous values retained, original recorder kept,
   rule order (400/404/409/422), inactive-category rule, agents 403, audit row naming the
   correction, in-progress and cancelled tours.
6. (M) History endpoint (US-32). **Tests:** only terminal tours, each filter, order, 400s, roles,
   retained detail, no reopen or delete.
7. (M) Audit review endpoint (US-39). **Tests:** order and paging, every filter, 400s, ADMIN only,
   405 for any write.
8. (M) Frontend: cancel and correct dialogs, corrections list, cancellation facts, history page,
   agent cancelled notice; contract sync.
9. (S) Frontend: admin audit log page; navigation.
10. (S) UML activity/use-case/sequence text, README; rendered diagrams; full test/build pass.
11. (S) Close-out: review, retrospective, statuses, D-049.

**Automated done checks**

- `./mvnw test` green including the audit, cancellation, correction and history rules.
- `npm run build` green; `npm run lint` clean outside `components/ui`.
- Correction of one demo stop appears both in the history (`correctionCount`) and in the audit log
  (`STOP_OUTCOME_CORRECTED` naming the correction id).
- A database created by the S7 build upgrades in place (additive columns and tables) and keeps
  working; a cancelled demo tour is not re-advanced by the demo loader after a restart.
- No password, password hash or demo password appears in any audit event.

**Review.** 2026-11-01.

- `./mvnw test`: **326 tests, 0 failures, 0 errors, 0 skipped** (271 from S1-S7 plus 55 new: audit
  service 7, audit trail 10, import/demo audit 2, tour domain 8, cancellation 7, correction 8,
  history 7, audit API 6). `npm run build` exits 0 (only the pre-existing >500 kB chunk warning);
  `npm run lint` shows the three pre-existing warnings in vendored `src/components/ui/` and nothing
  else.
- Done-check `s8-done-check.sh`: 173 `PASS` lines (counted with `grep -c '^PASS'` on the saved
  output), final line `FAILS=0` on the first run, no script change (no `s8-done-check.orig.sh`).
  Backend on 8081 with scratch H2 databases and a scratch routing cache, Vite on 5174. The owner's
  servers (8080, 5173), `backend/data/` and `backend/routing-cache/` were not touched. Phase A4 ran
  (it did not `SKIP`): a copy of the owner's database file started with the S8 jar, logged no demo
  failure and its audit log answered 200.
- Mermaid: the use-case, class, sequence and activity diagrams render with
  `@mermaid-js/mermaid-cli` 11 (session scratchpad, not a repo dependency): exit 0, SVG written, no
  "syntax error", re-run inside the done-check. Each changed diagram was rendered right after its
  edit.
- Observed on the fresh database: the start-up run wrote one `DATA_IMPORTED` and one `DEMO_LOADED`
  (4 tours), both with actor `system`, plus the demo tour steps by `system` (`TOUR_CONFIRMED` 3,
  `TOUR_COMPLETED` 1, six `STOP_OUTCOME_RECORDED`); the whole audit log held 81 events at the end
  of phase B1 and none contained a password, a hash or a demo password.

| Check | Observed |
|---|---|
| Static / UML | no traffic or routing engine in `optimization/`; GraphHopper only in `routing/`; `AuditEventRepository` extends Spring Data `Repository` with no delete or update method; `AuditEvent` is `@Immutable`; 38 audit actions; no S8 matcher in `SecurityConfig`; only `AuthController` and `AuditEventController` use `AuditService` in `web/`; class diagram names `OutcomeCorrection`, `cancelledFromStatus`, `CorrectionRetained`; activity diagram names `privileged correction`; README names the S8 endpoints; four diagrams render |
| S7 database upgraded (A1-A3) | S7 jar loads the demo (`DRAFT PLANNED IN_PROGRESS COMPLETED`); S8 jar starts on that database with `tours created=0` and no failure; the completed tour answers with `cancellable` false and `correctable` true; history lists it; cancelling the planned demo tour and correcting the not-collected stop both answer 200; the audit log holds the import and demo events by `system`, the planner sign-in, the cancellation and the correction; after a restart the tour stays `CANCELLED` and the correction is still there |
| Owner DB copy (A4) | a copy of `backend/data/wastems.mv.db` starts with the S8 jar without demo failure; audit search 200 |
| Start-up audit | one `DATA_IMPORTED` (entity `DATA_IMPORT`) and one `DEMO_LOADED` by `system`; 3 `TOUR_CONFIRMED`, 1 `TOUR_COMPLETED`, evaluation runs audited |
| Sign-in audit | success with entity `USER`; wrong password 401 and a `FAILURE` row `invalid credentials`; unknown user 401, `FAILURE` row without entity id; deactivated account 401 and `account deactivated`; sign-out 204 and one `SIGN_OUT` |
| No secrets | 81 events dumped over every page: no password, hash or `$2a$`; `USER_CREATED` details are `username auditee, role PLANNER`; dev and test passwords absent from the logs |
| Data-change audit | vehicle create/status, agent create, one link and one unlink (repeats add none), category create by admin, point status changes, user status change; a refused edit of a planned tour (409) leaves no event |
| Cancellation | agent 403; `{}` and blank reason 400 field `reason`; unknown tour 404; completed tour 409 with the documented message; planned demo tour cancelled (200, from `PLANNED`, pending stops stay `PENDING`); cancel again 409; frozen afterwards (start, edit, evaluate 409, delete 405); crew sees the cancelled planned tour (200) and never the cancelled draft (403); in-progress tour cancelled with its outcomes kept; three `TOUR_CANCELLED` events with from-status and reason |
| Correction (history + audit) | crew member 403; missing reason and `PENDING` 400 with the field; unchanged outcome 422; pending stop 409; planner corrects demo stop 4 to Collected with Mixed litter 18.00; previous status, text and recorder retained; admin may correct; crew sees it; `correctionCount` 1 in the history; one `STOP_OUTCOME_CORRECTED` naming the correction id |
| History | completed and cancelled only; filters by status, vehicle, agent (driver and collector), point and date range (inclusive); unknown agent empty; `status=PLANNED`, `from` after `to`, bad date and bad uuid 400; agent 403, admin 200, anonymous 401; the detail of a cancelled tour keeps cancellation facts |
| Audit search | planner and agent 403, anonymous 401; paging; newest first; `outcome=FAILURE` only sign-ins; 400s for action, outcome, size, page and time; `from`/`to` boundaries; actor filter case-insensitive; single event 200/404/400; PUT, PATCH, DELETE and POST 405 and the event unchanged |
| Restart (B2) | the demo creates nothing; the three cancelled tours stay `CANCELLED`; the correction and the audit rows persist |
| Frontend / bundle / proxy | `api.ts`, hooks, routes `/history` (PLANNER) and `/admin/audit` (ADMIN); lat/lng flip only in `geo.ts`; build 0; no lint findings outside `components/ui`; 22 required strings present in the bundle; through Vite 5174 the planner cancels with CSRF, history lists it, admin audit search finds it, both SPA routes serve |

- **US-30 met** (DRAFT, PLANNED and IN_PROGRESS tours move to CANCELLED with a reason; no re-cancel
  and no cancellation after completion; stops, outcomes and history are retained; it is not a
  delete).
- **US-31 met** (dedicated PLANNER/ADMIN endpoint; reason, actor and UTC time; the previous values
  are retained; an audit event names the correction; agents get 403).
- **US-32 met** (filters by date, status, vehicle, agent and point; the detail retains resources,
  order, outcomes, quantities, incidents, cancellation reason, timestamps and corrections; no reopen
  or delete).
- **US-39 met** (sign-in success and failure and every S1-S8 data change with actor, UTC time,
  action, entity, outcome and details; no secrets; ADMIN filters by time, actor, action, entity and
  outcome; no edit or delete), within D-040's limit for the UI pages: they were built and served but
  not viewed in a browser.
- **Deferred items closed:** FR-30 sign-in audit (S1); audit of S1-S7 changes, imports, evaluation
  runs, accept/reject/confirm, start, outcomes, completion and the demo load; US-03 audit
  administration; cancellation of drafts, planned and in-progress tours; planner corrections.
- **Deferred.** Dashboards, evaluation explorer, CSV and the traffic overlay (S9); metrics (S10).
- **Known gaps.**
  - No browser walkthrough (D-040).
  - Bootstrap seeding (dev accounts, map sample) is not audited by design.
  - Demo-created master data is summarized by one `DEMO_LOADED` event.
  - Refused requests are not audited; only failed sign-ins and failed evaluation runs are
    `FAILURE` rows.
  - The audit actor of demo tour steps is `system` (or the admin) while the outcome's recorder is
    the crew account.
- **Reviewer walkthrough.** Steps 8-11 of README §4.6.

**Retrospective.**

- *Went well.* All 326 tests and all 173 done-check assertions passed on the first run, with no
  script correction and no `fix(...)` commit. Recording the audit event inside the service
  transaction, after the change, kept refused requests out of the log without special cases, and
  updating kept categories in place avoided the Hibernate insert-before-delete conflict that the
  plan had flagged. Each diagram was rendered right after its edit.
- *Went badly.* The UI was again verified without a browser (build, lint, bundle strings and proxy
  checks). The done-check takes a long time because it starts the backend several times.
- *Process changes for S9.* Keep counting the done-check assertions from the script output; plan a
  browser walkthrough only if the owner can supply one; keep the audit hook in every new mutating
  service method (CSV import in S9).


### 3.12 Sprint S9 — Dashboards, evaluation explorer, CSV exchange, traffic overlay

**Sprint goal.** Planners see an operational dashboard and explore retained evaluation
comparisons; the fixed 60-scenario experiment of D-016/D-022 can be generated deterministically
and run from the backend (HTTP for small sets, a headless command for S10); evaluation scenarios
import from and results export to documented CSV files; a timestamped NYC DOT traffic layer is
displayed without touching optimizer costs.

**Entrance criteria.** S8 done (green baseline on `main` `3749362`: 326 backend tests, frontend
build, three lint warnings in vendored `components/ui`). Retained evaluation runs exist (S5/S6).
No decision is pending: D-016, D-021, D-022, D-024, D-028, D-029, D-038 and D-040 fix the
experiment matrix, the traffic boundary, demand/capacity, the CSV scope, the dashboard and audit
scope, the session model and the verification level.

**Scope notes.**

- **Experiment support (D-016, D-022).** A seeded generator samples real MN01 DSNY baskets
  without replacement from the committed basket snapshot (238 eligible) and draws synthetic demand
  per profile (`UNIFORM_LOW`, `UNIFORM_HIGH`, `HOTSPOT`); 3 profiles × seeds 1–5 × sizes
  10/25/50/100 = 60 canonical scenarios. Same seed ⇒ identical scenario and results (execution
  time excepted). Runs reuse the S5 retention and belong to an `EvaluationBatch` with its
  parameters, snapshot ids, road graph version and environment. Trigger: `POST
  /api/admin/experiments` (ADMIN, at most 180 runs) and `tools/run-experiment.sh OUT_DIR` for S10.
  A planner spike measured about 3 s for a 100-point scenario, so the canonical campaign takes
  about 1–2 minutes. S9 runs only a small hand-run set; the campaign is S10.
- **Explorer (US-36).** Run list with filters (kind, batch, profile, size, seed, status), run
  detail with reproducibility fields and per-method change vs input order (shown only for
  comparable feasible results), batch aggregates per size and profile. The hand-crafted demo
  runs (`TOUR_DRAFT`) are never part of a batch aggregate.
- **CSV (US-37, US-38, D-028).** Import of scenario points (existing collection points by id or
  source key, coordinates, category, kilograms, capacity, optional profile/seed) with strict,
  atomic validation and row errors; export of one row per run and method including failures,
  with every value needed to recompute changes; a batch's scenarios export in the import format
  (round trip). Schemas in `docs/evaluation-csv.md`.
- **Dashboard (US-35, D-029).** Resource counts, tours by status and stop progress for a planning
  period, with links to filtered views; explicitly not live tracking.
- **Traffic (US-34, D-021).** A planner spike found NYC Open Data `i4gi-tjb9` reachable without a
  key; a bounded snapshot (12 links touching the MN01 road-extract box, 2026-11-01) is committed
  with its fetch tool, like the basket and road extracts. An isolated `traffic/` package
  classifies links (observed, stale, no reading, invalid) and serves a display-only overlay with
  observation time, coverage note, attribution and the DOT disclaimer; a missing snapshot is
  shown as unavailable. It never enters routing or optimization.
- **Schema.** Additive (new table and nullable columns), plus a start-up conversion of three
  H2 `ENUM` columns of the S8 schema to `VARCHAR`, which the new enum values require.
- **UI (D-020, D-040).** Pages `/dashboard`, `/evaluations`, `/evaluations/:runId`; traffic toggle
  on the collection-point and tour maps; verified by build, lint, bundle strings and proxy checks.

**Task backlog**

1. (M) Schema groundwork: `EvaluationBatch`, scenario fields on runs and stops, new scenario kinds
   and audit actions, VARCHAR enum mapping and the start-up `SchemaUpgrade`. **Tests:** fresh
   columns are VARCHAR; ENUM conversion keeps data; point-count back-fill; batch counters.
2. (M) Seeded scenario generator `mn01-v1` and a non-saving evaluation entry point.
   **Tests:** golden samples and demand, determinism, nesting, sampling without replacement,
   profile ranges.
3. (L) Experiment service, ADMIN trigger, batch audit event. **Tests:** one retained run per
   scenario and method with provenance; **determinism** (same spec twice ⇒ identical runs);
   retained routing-failure runs; 409 without the MN01 import; validation; roles.
4. (M) Explorer API (US-36): paged run list, batches, batch aggregates, run detail change values.
   **Tests:** aggregate math, comparability rule, filters, paging, 400s, roles.
5. (M) CSV export (US-38) of results and of a batch's scenarios. **Tests:** header, failure rows,
   recalculation of changes, no operational columns.
6. (M) CSV import (US-37). **Tests:** validation matrix with row errors, atomicity, provenance,
   infeasible and routing-failure scenarios, **round trip** export → import ⇒ identical results.
7. (S) Headless experiment runner (`experiment` profile, `tools/run-experiment.sh`) for S10.
   **Tests:** output files; spec from properties.
8. (M) Operational dashboard API (US-35). **Tests:** summary math against fixtures and against
   the seeded demo data; period filter; roles.
9. (L) Traffic overlay backend (US-34): committed snapshot and fetch tool, isolated package,
   API. **Tests:** polyline decoding, classification (stale/no data/invalid/unavailable), pinned
   snapshot facts, package isolation, identical evaluation matrices before and after reading
   traffic.
10. (M) Frontend: dashboard page and URL-driven filters on the operational lists.
11. (L) Frontend: evaluation explorer, run page, CSV import/export, experiment launcher.
12. (S) Frontend: display-only traffic layer and panel on the maps.
13. (S) Documentation (`docs/evaluation-csv.md`, traffic provenance in
    `docs/geospatial-data.md`, README, use-case text); rendered diagrams; full test/build pass.
14. (S) Close-out: review, retrospective, statuses, D-050.

**Automated done checks**

- `./mvnw test` green including the generator determinism, experiment, CSV validation/round-trip,
  export, dashboard and traffic tests.
- `npm run build` green; `npm run lint` clean outside `components/ui`.
- A database created by the S8 build upgrades in place and accepts experiment batches and CSV
  imports.
- A small hand-run scenario set (8 scenarios: 2 profiles × 2 seeds × 10/25 points) runs through
  the API, matches an independent re-implementation of the generator and an independent
  matrix-based recomputation, repeats identically, round-trips through CSV, and the headless CLI
  reproduces the same results on another database.
- The demo dashboard matches the seeded demo data; the explorer lists the demo runs separately
  from the experiment batches.
- The traffic overlay shows the committed snapshot's timestamp, counts and texts, an
  "unavailable" state without a snapshot, and evaluation matrices are identical before and after
  reading it.

**Review.** 2026-11-02.

- **Tests.** `./mvnw test`: **406 tests, 0 failures, 0 errors, 0 skipped** (326 from S1-S8 plus 80 new:
  schema upgrade 3, batch 4, generator 11, experiment 10, explorer 11, export 8, import 9, CLI 3,
  dashboard 5, traffic 16). `npm run build` exits 0; `npm run lint` reports only the three
  pre-existing warnings in the vendored `src/components/ui/`.
- **Done-check.** `s9-done-check.sh` ran against the S9 jar on port 8081 with scratch H2 databases and a
  scratch routing cache, the headless CLI on a random port, and Vite on 5174. Final run: **162 `PASS`
  lines (counted from the output), `FAILS=0`**. The owner's servers on 8080 and 5173, `backend/data/`
  (only a copy was opened, phase A4 ran: a 3-point experiment on the copy answered 201 `COMPLETED`) and
  `backend/routing-cache/` were untouched. The hand-run experiment (8 runs) took 1.3 s over HTTP; the CLI
  run (2 scenarios, including JVM start and graph import) took 10 s. The four diagrams render with the
  Mermaid CLI (exit 0, SVG written, no syntax error). The canonical 60-scenario campaign was **not** run.
- **Script corrections** (original kept as `s9-done-check.orig.sh`, see D-050): two text greps matched
  pre-existing prose ("Traffic never enters (D-021)" in a comment, a note in the indicator table) and
  now look for code references; the A2 check expected three `SchemaUpgrade` log lines although
  Hibernate 6.6 had already altered the three ENUM columns (it now asserts the resulting `VARCHAR`
  types); phase C logged in before the start-up runner had seeded the accounts (it now waits for the
  seeder). The first two runs, with `FAILS=3` and `FAILS=2`, are in `s9-done-check.run1.out` and
  `s9-done-check.run2.out`.

| Check | Observed |
|---|---|
| Static and docs | optimization independence and GraphHopper confinement unchanged; traffic package isolated (no reference from `optimization/`, `routing/`, `service/`, `domain/`, `repository/`, `config/`; only `TrafficController` and its DTO in `web/`); traffic snapshot SHA-256 `2a1b9f1f...`; fetch tool unchanged; 40 audit actions; both `SecurityConfig` matchers; no dependency change against `main`; docs name every column; four diagrams render |
| S8 database upgraded (A1-A3) | S8 demo loaded; S9 jar: point count back-filled for the 4 old runs, demo creates nothing, old runs listed (demo draft 8 points), experiment 201 and CSV import 201 (new enum values stored), both audit actions present, the three columns are `VARCHAR`; restart: no conversion, both batches kept |
| Owner database copy (A4) | run on a copy: 3-point experiment 201 `COMPLETED` |
| Dashboard | vehicles 3/2/1, agents 5/4/1, points 449/448/1, tours one per state, stops 14 (8 pending, 4 collected, 2 not collected), `collectedKg` equal to the sum over the tours; today-only period 4 tours, tomorrow empty; inverted period 400 on `to`; roles 200/403/401 |
| Hand-run experiment and independent checks | 8 runs 201 `COMPLETED`; 15 aggregate rows in group order; independent Python port of `java.util.Random` reproduces the stops and demand of every run; every distance and travel time recomputed from the retained matrix and visit order; permutations; NN + 2-opt <= NN |
| Determinism | the same request again: identical stops, matrices, visit orders, distances and durations for 8 scenarios |
| Explorer API | kind, profile, size, seed and status filters; paging; 400s on `page`, `size`, `pointCount`, `kind`; run detail with scenario fields and change values; batch 404/400; agent 403, anonymous 401 |
| Results export | 24 rows, header, recalculation of every change within 0.001, duration identity, source and snapshot ids, no operational column; `kind=TOUR_DRAFT` export 12 rows; scenario export 140 rows in the import format |
| CSV round trip | the scenario export imported again: 8 runs, `EQUAL 24` rows per scenario key and method |
| CSV validation | three bad rows give 400 with exactly those rows (2 `category`, 3 `latitude`, 4 `demand_kg`); header error row 1 `demand_kg`; empty body 400 `file`; JSON content type 415; refused imports created no batch and no audit row |
| Failure and infeasible rows | Queens point `ROUTING_FAILED` / `POINT_NOT_ROUTABLE`; 5 kg capacity all methods infeasible; export rows with empty indicators; aggregates with 0 comparable runs |
| Audit | two `EVALUATION_EXPERIMENT_RUN` by `admin` (`SUCCESS`); two `EVALUATION_SCENARIOS_IMPORTED` (round trip `SUCCESS`, Queens batch `FAILURE`); `TOUR_EVALUATED` only for tour runs |
| Traffic overlay | snapshot id `nyc-dot-i4gi-tjb9:12:2a1b9f1faa7f`, observed `2026-11-01T20:08:05Z` (`15:08:05` NYC), 8 observed / 4 without reading, bands, geometry sources, truncated link with 12 coordinates, GeoJSON order inside NYC, the four texts; roles and `POST` 405; matrices identical before and after reading the overlay, tour revision unchanged |
| Traffic unavailable (C) | 200, `available: false`, "Traffic snapshot not found", no links; the rest of the app answers |
| CLI and cross-database determinism (D) | exit 0 in 10 s; `results.csv` 6 rows, `scenarios.csv` 35 rows, `batch.json` 2 runs `COMPLETED` by `system`, `run-info.txt` with `commit:` and `exit: 0`; rows `EQUAL` to the API batch of another database; second run into the same directory exits 64 |
| Restart (B2) | demo creates nothing; batches, runs and audit rows persist; an experiment run is still readable |
| Frontend, bundle, proxy | `api.ts` mirrors the S9 records; hooks call the new endpoints; three PLANNER routes; no traffic reference in the evaluation and tour hooks and components; build 0; lint clean outside `components/ui`; all 30 literal UI strings in the bundle; through Vite 5174: dashboard, batches, CSV import with CSRF 201, `text/csv` export, traffic overlay and the three SPA routes |

- **Acceptance.**
  - **US-34** met: official link geometry and attributes with observation time, age, coverage note,
    attribution and the DOT disclaimer; unsensed streets are not coloured; stale, no-reading, invalid and
    unavailable states are visible; display only, shown by identical matrices and the isolation test.
    Limits: D-040 (no browser walkthrough) and the overlay is a committed snapshot refreshed by a tool,
    not live.
  - **US-35** met: resource and tour summaries, stop progress for a selected period, links to filtered
    views and the explicit "not live tracking" text. Limit: D-040.
  - **US-36** met: batch and run selection, per-method distance, duration, execution time, coverage and
    checks, change only for comparable results, profile, seed and size, configuration, snapshots, road
    graph version and visit order; aggregates per batch exclude the hand-crafted demo. Limit: D-040.
  - **US-37** met: documented schema and example; header, type, range, kilogram, category, duplicate
    point/order and required-field validation with row errors; atomic; provenance; the three methods
    use the same frozen scenario; no operational import.
  - **US-38** met: one row per run and method including failures; all required columns; recalculation
    verified; no operational data.
  - Fixed-matrix experiment support done: generator, determinism, runner and CLI for S10; the 60
    canonical scenarios were **not** run in S9.
- **Deferred.** The canonical 60-scenario campaign, the metrics and the research answer (S10).
- **Known gaps.** No browser walkthrough (D-040). The traffic overlay is a stored snapshot (refresh by
  `tools/fetch-traffic-snapshot.py` and a commit), not live. Experiment batches run synchronously in the
  HTTP request (bounded to 180 runs). CSV import references existing collection points only. Capacity is
  a fixed assumption (10 000 kg) under which every canonical scenario is feasible.
- **Reviewer walkthrough.** README section 4.6 steps 12-15 and section 4.7.

**Retrospective.**

- *Went well.* 406 tests passed and the generator golden values (computed by the planner with a port of
  `java.util.Random`) matched on the first try; the independent Python re-implementation, the CSV round
  trip and the cross-database CLI comparison all agreed, so determinism is demonstrated, not assumed.
  Computing a batch in memory first and saving it in one transaction (import) or one transaction per run
  (experiment) avoided the rollback-only pitfall. Rendering every diagram right after its edit again
  cost nothing.
- *Went badly.* The plan stated that `ddl-auto: update` never changes a column type; Hibernate 6.6 does
  alter the ENUM columns on H2, so the planned `SchemaUpgrade` conversion is only a safety net and the
  matching done-check expectation was wrong. Two further done-check expectations (text greps over
  pre-existing prose, a start-up race) needed correcting, so the first two runs failed. The traffic service has two constructors (one for tests) and first failed to start until the public one carried `@Autowired`. The UI was again verified without a browser.
- *Process changes for S10.* Probe framework behaviour on a copy of the old database before relying on
  it in a plan; have done-check scripts wait for start-up runners, not only for the port; run the
  canonical campaign with `bash tools/run-experiment.sh metrics/experiment` on an otherwise idle machine
  and keep `run-info.txt` and `run.log`.

### 3.13 Sprint S10 — Metrics campaign and project closure

**Sprint goal.** Part 6 is *answered with measured data* (tooling executes, reports exist and are
committed), the HTML deck is ready, requirements are traceable to code and tests, and the
prototype and its documentation are coherent end to end for the reviewer.

**Entrance criteria.** S9 done (merged at `c2c7ef0`; 406 tests green, build and lint clean). The
deck format is fixed by D-041 (self-contained HTML); the presentation **date** is still open
(AGENTS.md §4) and does not block building the deck, which carries no date.

**Planning findings.** `tools/metrics.sh` had never run on the full codebase. It runs, but JDepend
2.9.1 cannot read Java 21 class files (183 `Unknown constant: 18` errors, 90 of 273 classes
analysed), PMD reports complexity only above the threshold, LOC mixes data files with code, and the
TypeScript side had no complexity measure. In the canonical matrix the three demand profiles of a
seed and size share the same points and the 10 000 kg capacity never binds, so the 60 scenarios
contain 20 distinct routing instances; paired statistics use those 20.

**Task backlog**

1. (M) Metrics tooling: JDepend fork `guru.nidi:jdepend` 2.9.5 as a plugin dependency of the
   `metrics` profile, a second PMD execution for the full cyclomatic-complexity distribution,
   TypeScript complexity with oxlint `eslint/complexity` (vendored `components/ui` excluded),
   per-language LOC, extended CKJM/JDepend summaries, new PMD and TypeScript summaries, a stale-class
   guard and `--publish` (copies the summaries to `docs/results/metrics/`).
2. (S) `tools/experiment-stats.py` (stdlib only): validity checks, descriptive statistics per size,
   profile and method, NN versus NN + 2-opt, execution time, paired comparisons with an exact
   Wilcoxon signed-rank test over the 20 instances, Markdown/CSV/JSON outputs and an SVG chart.
3. (S) `tools/capture-screenshots.mjs`, an optional tool using a puppeteer installed outside the
   repository (`PUPPETEER_DIR`); no project dependency (D-040).
4. (M) Run the fixed campaign on a clean tree: `bash tools/run-experiment.sh metrics/experiment
   --wastems.experiment.repetitions=3` (60 scenarios × 3 repetitions = 180 runs, 540 method rows),
   then `tools/experiment-stats.py` and `bash tools/metrics.sh --publish`; commit the small result
   files under `docs/results/` (never the H2 database or the graph cache).
5. (M) Write `docs/06-metrics.md` from those files only: metric tables with their commands,
   interpretation (hot spots, package coupling, `optimization/` independence), experiment design,
   results, statistics, the research question answered, threats to validity and limitations.
6. (M) Presentation assets: architecture diagram (Mermaid source + SVG), UML exports, twelve real
   screenshots of the running prototype.
7. (M) `presentation/index.html`: self-contained deck following the six parts, keyboard navigation,
   demo script, metrics table and key finding, Scrum backlog/sprints/retrospective highlights,
   limitations and future work.
8. (S) `docs/traceability.md`: FR → user story → use case → code → test.
9. (S) Final sweep: full regression, scripted reviewer walkthrough, README, `AGENTS.md` §3/§5, story
   statuses, D-051.

**Automated done checks**

- `bash tools/metrics.sh` exits 0 with no JDepend parse error, JDepend covering every class file,
  and summaries identical to the committed `docs/results/metrics/`.
- `docs/results/experiment/results.csv` has 540 rows (60 scenarios × 3 repetitions × 3 methods), all
  completed and feasible; `run-info.txt` shows exit 0 on a clean tree; an independent recomputation
  agrees with `docs/results/experiment-stats/`; a fresh partial re-run reproduces the distances.
- Every decimal number in `docs/06-metrics.md` and in the deck occurs in a committed results file
  or document; no placeholder text.
- The deck opens offline from `file://`, navigates by keyboard, loads every image and has no
  overflowing slide at 1280×720.
- `./mvnw test`, `npm run build`, `npm run lint` green; all diagrams render; the README §4.6
  walkthrough passes over HTTP on spare ports.

**Review.** *To be filled at sprint close.*

**Retrospective.** *To be filled at sprint close.*
