# Part 1 — Problem analysis

## 1. Project overview

Intelligent Waste Management System

The project is a management system for organizing municipal or organizational waste
collection. It covers five core areas: **collection points, vehicles, agents, tours, and
waste**. It also includes an intelligent module that optimizes collection tours and an
evaluation mechanism that measures the module's impact against a documented baseline.

This document defines the problem independently of the implementation technology.

## 2. Context

Waste collection requires coordination between geographically distributed collection
points, a limited fleet of vehicles, available agents, and different categories and
quantities of waste. Poorly planned tours can produce unnecessary travel, inefficient use
of vehicle capacity, delayed collections, and uneven workloads.

A conventional management system can store operational data and track collection work, but
it does not necessarily improve the quality of the planning decisions. This project
therefore combines:

1. an operational management system for the five core areas;
2. an intelligent tour-optimization module;
3. a reproducible comparison of baseline and optimized planning.

The intelligent module is intended to support a planner, not to conceal its decisions. Its
inputs, proposed tour order, constraints, and measured outcomes must remain visible.

## 3. Users and stakeholders

| User or stakeholder | Role in the system |
|---|---|
| **Administrator** | Manages user access and reference data, including waste categories. |
| **Collection manager / planner** | Manages collection points, vehicles, agents, and tours; assigns resources; runs the optimizer; and reviews impact indicators. |
| **Collection agent** | Consults assigned tours and collection points, records collection progress, and enters collected quantities or incidents. |
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
  name, a location, and the operational information required for planning, such as its
  status and collection demand.
- **FR-06 — Collection demand:** The system shall record the waste category and quantity or
  demand associated with a collection point so that this information can be used during
  tour planning.

### 4.3 Vehicles and agents

- **FR-07 — Vehicle management:** The collection manager shall be able to create, view,
  update, and change the availability of vehicles.
- **FR-08 — Vehicle capacity:** Each vehicle shall have a capacity that can be checked
  against the demand assigned to a tour.
- **FR-09 — Agent management:** The collection manager shall be able to create, view,
  update, and change the availability of agents.
- **FR-10 — Resource assignment:** The system shall allow an available vehicle and one or
  more available agents to be assigned to a tour, subject to the final prototype scope.

### 4.4 Tours and collection execution

- **FR-11 — Tour management:** The collection manager shall be able to create and consult a
  tour defined by its date, assigned resources, selected collection points, visit order,
  and status.
- **FR-12 — Tour consultation:** An agent shall be able to consult the details and ordered
  stops of an assigned tour.
- **FR-13 — Collection follow-up:** An authorized user shall be able to update tour and stop
  statuses and record the actual collection result, including collected quantities where
  applicable.
- **FR-14 — Operational history:** The system shall retain completed or cancelled tours and
  their collection results for later consultation and evaluation.

### 4.5 Intelligent tour optimization

- **FR-15 — Optimization input:** The optimizer shall use an explicit planning scenario
  containing the selected collection points, their locations and demands, the available
  vehicle capacities, and any other constraints supported by the prototype.
- **FR-16 — Optimized tour proposal:** The optimizer shall produce a valid ordered tour, or
  a set of tours where supported, without silently omitting selected points or violating
  documented constraints.
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
  method, constraints, and resulting indicators so that a reported result can be reproduced.

## 5. Non-functional requirements

- **NFR-01 — Security:** Passwords shall not be stored in plain text, and protected
  operations shall enforce both authentication and server-side authorization.
- **NFR-02 — Data integrity:** The system shall validate quantities, capacities, locations,
  resource availability, and tour relationships. Invalid or inconsistent operations shall
  be rejected with a useful error.
- **NFR-03 — Usability:** Core planning and collection workflows shall be accessible through
  clear forms and views, with readable statuses and validation messages.
- **NFR-04 — Performance observability:** The system shall measure optimization execution
  time and the size of each evaluation scenario. A response-time target will be fixed when
  the prototype scope and evaluation datasets are agreed.
- **NFR-05 — Reliability:** Optimization failure shall not corrupt existing operational
  data. An accepted tour and its evaluation results shall be stored consistently.
- **NFR-06 — Explainability:** Optimization results shall expose their inputs, tour order,
  constraints, and indicators; the intelligent module shall not be an unexplained black box
  from the planner's perspective.
- **NFR-07 — Fair evaluation:** Baseline and optimized solutions shall use identical demand,
  resource, location, and constraint data, and shall apply the same distance and duration
  formulas.
- **NFR-08 — Maintainability:** Operational management, optimization, and impact evaluation
  shall be separated into cohesive modules so that they can be tested and measured
  independently.
- **NFR-09 — Testability and reproducibility:** Demonstration data and evaluation scenarios
  shall be reproducible, and research results shall identify the commands, dataset,
  algorithm configuration, and execution environment used to obtain them.
- **NFR-10 — Portability:** A reviewer shall be able to install and run the prototype locally
  from documented instructions without undisclosed manual steps.
- **NFR-11 — Privacy:** The system shall store only agent and user information necessary for
  authentication, assignment, and operational follow-up.

## 6. Problem statement

A waste-collection organization must coordinate collection demand, vehicles, agents, and
tour execution while using its resources efficiently. A system limited to data management
would improve record keeping but would not determine whether intelligent planning provides
a measurable operational benefit.

The problem is therefore to design and implement a coherent waste-management prototype
that manages collection points, vehicles, agents, tours, and waste, integrates an
intelligent tour-optimization module, and evaluates that module under controlled and
reproducible conditions.

## 7. Research question

> **To what extent does integrating an intelligent tour-optimization module reduce total
> travel distance and estimated collection duration compared with a documented baseline,
> while satisfying the same vehicle-capacity and collection-service constraints?**

Supporting measures:

- optimization execution time;
- number and size of evaluated scenarios;
- percentage distance and duration change relative to the baseline;
- number of constraint violations, which must be zero for a valid compared solution;
- collection-point coverage, which must be identical for valid baseline and optimized
  scenarios.

No numerical improvement is claimed before the experiments are executed.

## 8. Scope clarification

The agreed functional areas are collection points, vehicles, agents, tours, waste, tour
optimization, and impact measurement. The following are **not assumed** to be part of the
prototype and require an explicit later decision if needed:

- connected fill-level sensors or other IoT hardware;
- real-time GPS tracking;
- external commercial mapping or routing services;
- a citizen-facing mobile application;
- billing or payment management.

The optimization algorithm, the baseline route construction, the depot model, the distance
calculation, the agent-assignment cardinality and the prototype scope are **resolved** and
recorded in `docs/decisions.md` (D-007 to D-016). The implementation technology is fixed by
D-004, D-013 and D-017: a Java 21 / Spring Boot 3 REST API with a React, Tailwind CSS and
shadcn/ui front end.
