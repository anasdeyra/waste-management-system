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

Sprint Backlogs, sprint reviews, and retrospectives will be added after the Product Backlog is
validated and sprint selection begins; no completed sprint or measured outcome is claimed here.
