# Part 1 — Problem analysis

## 1. Project overview

Intelligent Waste Management System

The project is a **map-centric management system** for organizing municipal or organizational
waste collection. It covers five core areas: **collection points, vehicles, agents, tours, and
waste**. It also includes an intelligent module that optimizes collection tours and an
evaluation mechanism that measures the module's impact against a documented baseline.

The worked example is **New York City**, initially **Lower Manhattan (DSNY district MN01)**.
Real DSNY basket and garage coordinates anchor the demonstration, tours follow a real road
network, and interactive maps support planning, execution, and comparison. Synthetic demand
and operational records remain distinguishable from the real location data.

This document defines the problem independently of the implementation technology.

## 2. Context

Waste collection requires coordination between geographically distributed collection
points, a limited fleet of vehicles, available agents, and different categories and
quantities of waste. Poorly planned tours can produce unnecessary travel, inefficient use
of vehicle capacity, delayed collections, and uneven workloads.

A conventional management system can store operational data and track collection work, but
it does not necessarily improve the quality of the planning decisions. Straight-line routes
also conceal practical street effects such as one-way restrictions and longer paths around
physical barriers. This project therefore combines:

1. an operational management system for the five core areas;
2. real geolocation data and road-following tours in a bounded, real-city case study;
3. interactive maps for operational display and user interaction;
4. an intelligent tour-optimization module; and
5. a reproducible comparison of baseline and optimized planning on the same road graph.

The intelligent module is intended to support a planner, not to conceal its decisions. Its
inputs, proposed tour order, constraints, and measured outcomes must remain visible.

## 3. Users and stakeholders

| User or stakeholder | Role in the system |
|---|---|
| **Administrator** | Creates and activates/deactivates user accounts, assigns roles, manages waste categories, and reviews the audit log. Inherits all planner capabilities. |
| **Collection manager / planner** | Manages collection points, vehicles, agents, and tours; selects stops and edits locations on a map; assigns resources; runs the optimizer; reviews route geometry and impact indicators; and makes audited corrections to execution results. |
| **Collection agent** | Consults only assigned tours, ordered stops, and road routes on a map. The assigned driver or either assigned collector may record collection progress, collected quantities, and incidents. |
| **Waste-collection organization** | Owns the operational process and benefits from improved resource use and service quality. |
| **Course reviewer** | Runs the prototype with demonstration data and verifies the comparison between baseline and optimized tours. |

The collection manager and administrator may be represented by separate roles even if one
person performs both roles during a demonstration.

## 4. Functional requirements

### 4.1 Access and administration

- **FR-01 — Authentication:** The system shall allow a registered user to authenticate and
  shall protect management operations from unauthenticated access.
- **FR-02 — Authorization:** The system shall restrict operations according to the user's
  role, particularly administration, planning, and collection execution.
- **FR-03 — Waste categories:** The administrator shall be able to manage waste categories
  used to classify collection demand and collected waste.

### 4.2 Collection points

- **FR-04 — Collection-point management:** The collection manager shall be able to create,
  view, update, and deactivate collection points.
- **FR-05 — Collection-point data:** Each collection point shall have an identifier, a
  name, WGS84 longitude/latitude coordinates, and the operational information required for
  planning, such as its status and collection demand. Imported points shall also retain their
  source dataset, source identifier, and source ownership/type where available.
- **FR-06 — Collection demand:** The system shall record one or more demand entries for a
  collection point, each with a waste category and a quantity in kilograms. A tour may contain
  mixed categories, and the sum of all selected demand entries shall be checked against vehicle
  capacity; separate vehicle compartments are not modeled.

### 4.3 Vehicles and agents

- **FR-07 — Vehicle management:** The collection manager shall be able to create, view,
  update, and change the availability of vehicles.
- **FR-08 — Vehicle capacity:** Each vehicle shall have a capacity that can be checked
  against the demand assigned to a tour.
- **FR-09 — Agent management:** The collection manager shall be able to create, view,
  update, and change the availability of agents. An agent may have an optional one-to-one link
  to an active `AGENT` user account for authenticated execution access.
- **FR-10 — Resource assignment:** The system shall allow one available vehicle, one
  available driver, and zero to two available collectors to be assigned to a tour, with
  distinct driver and collector roles.

### 4.4 Tours and collection execution

- **FR-11 — Tour management:** The collection manager shall be able to create and consult a
  tour defined by its date, assigned resources, selected collection points, visit order,
  and status. The lifecycle shall be `DRAFT → PLANNED → IN_PROGRESS → COMPLETED`, with
  `CANCELLED` permitted before completion.
- **FR-12 — Tour consultation:** An agent shall be able to consult the details and ordered
  stops of an assigned tour, but not tours assigned to other crews.
- **FR-13 — Collection follow-up:** The assigned driver or either assigned collector shall be
  able to update tour and stop statuses and record actual collected quantities and incidents.
  Each stop shall move from `PENDING` to `COLLECTED` or `NOT_COLLECTED`; a collected stop shall
  record quantities by waste category, a not-collected stop shall require a reason, and tour
  completion shall require a terminal outcome for every stop. Planners and administrators shall
  be able to consult results and correct them through audited operations.
- **FR-14 — Operational history:** The system shall retain completed or cancelled tours and
  their collection results for later consultation and evaluation.

### 4.5 Intelligent tour optimization

- **FR-15 — Optimization input:** The optimizer shall use an explicit planning scenario
  containing the selected collection points, their real coordinates and demands, the depot,
  available vehicle capacities, a versioned directed road-network cost matrix, and any other
  constraints supported by the prototype.
- **FR-16 — Optimized tour proposal:** Each optimizer run shall produce one valid ordered
  single-vehicle tour without silently omitting selected points or violating documented
  constraints. Its operational representation shall include road-following leg geometry
  rather than straight lines between stops.
- **FR-17 — Planner control:** The collection manager shall be able to review the proposed
  solution before accepting it as an operational tour.
- **FR-18 — Optimization information:** The system shall display the proposed visit order,
  the calculated indicators, and any rejected or infeasible scenario explanation.

### 4.6 Impact measurement

- **FR-19 — Baseline comparison:** The system shall evaluate an optimized solution and a
  documented baseline using the same scenario and calculation rules.
- **FR-20 — Impact indicators:** The system shall calculate and display, at minimum, total
  distance and estimated duration for each compared solution. Optimization execution time
  and constraint-compliance indicators shall also be recorded for the research evaluation.
- **FR-21 — Result traceability:** Each comparison shall retain the scenario, planning
  method, constraints, source/road-graph snapshots, and resulting indicators so that a
  reported result can be reproduced.

### 4.7 Maps and real-city data

- **FR-22 — Operational map:** The system shall display the depot and collection points on an
  interactive map with pan/zoom, filtering, layer controls, and a linked details view.
- **FR-23 — Map-based editing and selection:** An authorized planner shall be able to set or
  correct a collection point's coordinates by clicking the map and confirming a form, and
  shall be able to include or remove stops from a tour through map interactions.
- **FR-24 — Tour visualization:** The system shall display ordered stops and road-following
  tour geometry, distinguish pending and completed stops, and allow baseline and optimized
  routes to be compared as distinct layers.
- **FR-25 — Traffic overlay:** The system shall be able to display official NYC DOT traffic
  link geometry and attributes with their observation timestamp and coverage limitations.
  Traffic status shall not be represented as available on unsensed streets.
- **FR-26 — Real-city seed data:** The demonstration shall include reproducible MN01 DSNY
  basket and MN01G garage extracts. Real locations shall be clearly distinguished from
  synthetic demands, vehicles, agents, and collection results.

### 4.8 User administration and supporting features

- **FR-27 — User account management:** The administrator shall be able to create user accounts,
  assign the `ADMIN`, `PLANNER`, or `AGENT` role, and activate or deactivate accounts. Public
  self-registration shall not be provided.
- **FR-28 — Dashboards:** The system shall provide operational resource and tour summaries and
  retained baseline/nearest-neighbor/optimized distance and duration comparisons.
- **FR-29 — Evaluation CSV exchange:** The system shall import evaluation-scenario points from
  a documented CSV schema with validation errors, and shall export retained evaluation results
  for all three planning methods. General master-data and operational-tour CSV exchange is not
  included.
- **FR-30 — Audit log:** The system shall record sign-in and data-change events with actor, UTC
  timestamp, action, entity type and identifier, outcome, and concise details. The administrator
  shall be able to consult and filter the log; field-level before/after diffs are not required.

## 5. Non-functional requirements

- **NFR-01 — Security:** Passwords shall not be stored in plain text, and protected
  operations shall enforce both authentication and server-side authorization.
- **NFR-02 — Data integrity:** The system shall validate quantities, capacities, WGS84
  coordinate ranges, resource availability, and tour relationships. Invalid or inconsistent
  operations shall be rejected with a useful error, and coordinate order shall be explicit at
  API boundaries.
- **NFR-03 — Usability and accessibility:** Core planning and collection workflows shall be
  accessible through clear maps, forms, and views, with readable statuses and validation
  messages. Essential information and actions shall not be available only through a map;
  synchronized lists/forms shall provide an alternative.
- **NFR-04 — Performance observability:** The system shall measure optimization execution
  time and the size of each evaluation scenario. No unmeasured response-time target is
  claimed; any later target must be recorded as an explicit decision.
- **NFR-05 — Reliability:** Optimization failure shall not corrupt existing operational
  data. An accepted tour and its evaluation results shall be stored consistently.
- **NFR-06 — Explainability:** Optimization results shall expose their inputs, tour order,
  constraints, and indicators; the intelligent module shall not be an unexplained black box
  from the planner's perspective.
- **NFR-07 — Fair evaluation:** Baseline and optimized solutions shall use identical demand,
  resource, location, constraint, directed road-network, and base travel-time data, and shall
  apply the same indicator formulas. A traffic refresh shall not change one method's input
  independently of another.
- **NFR-08 — Maintainability:** Operational management, optimization, and impact evaluation
  shall be separated into cohesive modules so that they can be tested and measured
  independently.
- **NFR-09 — Testability and reproducibility:** Demonstration data and evaluation scenarios
  shall be reproducible, and research results shall identify the commands, source and road
  snapshots, dataset, algorithm configuration, routing version, and execution environment
  used to obtain them.
- **NFR-10 — Portability:** A reviewer shall be able to install and run the prototype locally
  from documented instructions without undisclosed manual steps. Demonstration source data
  and the routing graph shall be available locally; unavailable live tiles or traffic feeds
  shall not corrupt saved operational data.
- **NFR-11 — Privacy:** The system shall store only agent and user information necessary for
  authentication, assignment, and operational follow-up.
- **NFR-12 — Provenance and attribution:** Real, imported, and synthetic values shall be
  distinguishable. Map/data attribution and the NYC DOT disclaimer shall remain visible or
  readily accessible, and timestamps shall accompany traffic observations.

## 6. Problem statement

A waste-collection organization must coordinate collection demand, vehicles, agents, and
tour execution while using its resources efficiently. A system limited to data management
would improve record keeping but would not determine whether intelligent planning provides
a measurable operational benefit.

The problem is therefore to design and implement a coherent, map-centric waste-management
prototype for a real Lower Manhattan case study that manages collection points, vehicles,
agents, tours, and waste, computes road-following tours from real coordinates, integrates an
intelligent tour-optimization module, and evaluates that module under controlled and
reproducible conditions without presenting synthetic operations or partial traffic coverage
as measured municipal reality.

## 7. Research question

> **To what extent does integrating an intelligent tour-optimization module reduce total
> road-network travel distance and estimated collection duration compared with a documented
> baseline, while satisfying the same vehicle-capacity and collection-service constraints?**

Supporting measures:

- optimization execution time;
- number and size of evaluated scenarios;
- percentage distance and duration change relative to the baseline;
- number of constraint violations, which must be zero for a valid compared solution;
- collection-point coverage, which must be identical for valid baseline and optimized
  scenarios.

No numerical improvement is claimed before the experiments are executed.

## 8. Scope clarification

The agreed functional areas are collection points, vehicles, agents, tours, waste, real road
routing, interactive maps, tour optimization, and impact measurement, supported by user
administration, operational/evaluation dashboards, evaluation-only CSV exchange, and an audit
log (D-023–D-031). The NYC/MN01 case study, data sources, and map workflows are resolved in
D-019–D-022 and detailed in `docs/geospatial-data.md`. The following are **not** part of the
current prototype:

- connected fill-level sensors or other IoT hardware;
- real-time GPS tracking;
- traffic-aware optimization or automatic dynamic rerouting;
- external commercial mapping or routing services;
- a citizen-facing mobile application;
- public user self-registration;
- vehicle compartments or category-specific vehicle capacities;
- general master-data/operational-tour CSV exchange and field-level audit diffs; and
- billing or payment management.

The optimization algorithm, baseline route construction, depot model, directed road-network
calculation, agent-assignment cardinality, and prototype scope are **resolved** and recorded in
`docs/decisions.md` (D-007 to D-016 and D-019 to D-022). The implementation technology is fixed
by D-004, D-013, D-017, D-020, and D-021: a Java 21 / Spring Boot 3 REST API, local GraphHopper
routing, and a React, Tailwind CSS, shadcn/ui, and Leaflet front end.
