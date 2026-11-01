# Activity diagram — Tour planning and execution lifecycle

## 1. Purpose

This activity covers UC08–UC19: draft preparation, three-method evaluation, planner review,
confirmation, assigned-crew execution, completion, and cancellation. It implements the lifecycle
chosen in D-025 and the terminal stop outcomes in D-031. Execution (start, stop outcomes,
completion) is implemented in S7 for the assigned crew only; planners follow it read-only.

The main path shows an accepted optimized proposal. Keeping the input order (rejecting
the proposal) attaches the input-order road route of the same run, so an otherwise valid
draft can still be confirmed in its current order.

## 2. Diagram

```mermaid
flowchart TD
    START([Start]) --> CREATE[Planner creates DRAFT with date and depot]
    CREATE --> PREPARE[Select and arrange active points; assign vehicle and crew]
    PREPARE --> VALIDATE{Draft inputs structurally valid?}
    VALIDATE -- No --> FIX[Display validation errors and edit DRAFT]
    FIX --> PREPARE
    VALIDATE -- Yes --> EVALUATE[Evaluate input order, nearest neighbor and NN + 2-opt on one frozen scenario]

    EVALUATE --> ROUTABLE{Complete directed routing scenario available?}
    ROUTABLE -- No --> EXPLAIN[Retain/report routing or infeasibility details without changing the DRAFT]
    EXPLAIN --> FIX
    ROUTABLE -- Yes --> COMPARE[Display road-following candidates, orders, indicators and constraint checks]

    COMPARE --> FEASIBLE{Feasible NN + 2-opt proposal available?}
    FEASIBLE -- No --> FIX
    FEASIBLE -- Yes --> ACCEPT{Planner accepts proposal?}
    ACCEPT -- Yes --> APPLY[Apply selected proposal order and road-leg reference to DRAFT]
    ACCEPT -- No --> KEEP[Keep current DRAFT order; attach input-order road route; retain evaluation]
    APPLY --> CONFIRM{All confirmation validations pass?}
    KEEP --> CONFIRM
    CONFIRM -- No --> FIX
    CONFIRM -- Yes --> PLANNED[Confirm tour as PLANNED and lock ordinary structural edits]

    PLANNED --> START_TOUR{Assigned crew starts; resources, points and route still valid?}
    START_TOUR -- No, wait --> PLANNED
    START_TOUR -- Yes --> ACTIVE[Set tour IN_PROGRESS]
    ACTIVE --> NEXT{PENDING stop remaining?}
    NEXT -- Yes --> OUTCOME{Stop collected?}
    OUTCOME -- Yes --> COLLECTED[Set COLLECTED; record category quantities, actor and UTC time]
    OUTCOME -- No --> NOT_COLLECTED[Set NOT_COLLECTED; record incident or reason, actor and UTC time]
    COLLECTED --> NEXT
    NOT_COLLECTED --> NEXT
    NEXT -- No, all outcomes terminal --> COMPLETE[Assigned crew completes tour; refused with STOP_PENDING while a stop is pending]
    COMPLETE --> COMPLETED[(COMPLETED: retain operational history)]
    COMPLETED --> END([End])

    CREATE -. cancel with reason .-> CANCELLED[(CANCELLED: retain draft/history)]
    PREPARE -. cancel with reason .-> CANCELLED
    PLANNED -. cancel with reason .-> CANCELLED
    ACTIVE -. cancel with reason and partial results .-> CANCELLED
    CANCELLED --> END
```

## 3. Guards and state rules

| Transition or state | Rule |
|---|---|
| `DRAFT → PLANNED` | Revalidate stops/order, active and eligible points, one available vehicle, one available driver, zero to two distinct available collectors, total mixed-category demand within capacity, a planning date not in the past, and a road route from a retained run of the current draft whose stop and depot coordinates are unchanged (`ROUTE_MISSING`, `ROUTE_OUTDATED`). Any failure returns the blocking issues and the tour stays `DRAFT`. |
| `DRAFT` route reference | Set only by accepting the NN + 2-opt proposal or keeping the input order of a completed, current run. Cleared by any change of stops, order or depot; a stale run (other revision) is refused. A failed or infeasible run cannot provide a route, so the planner corrects the draft and evaluates again. |
| `PLANNED → IN_PROGRESS` | Only an assigned crew member (driver or collector with a linked, active `AGENT` account) may start, and only a `PLANNED` tour. The start rechecks the structural issues (vehicle, driver and collectors available, points active and eligible, capacity) and the road route (`ROUTE_MISSING`, `ROUTE_OUTDATED`); any failure returns the blocking issues (for example `INACTIVE_STOP`, `VEHICLE_UNAVAILABLE`) and the tour stays `PLANNED` (“No, wait”). The planning date is not checked. Nothing is rechecked after the start. |
| `PENDING → COLLECTED` | The assigned driver or either collector records non-negative actual quantities by waste category, actor, and UTC time. |
| `PENDING → NOT_COLLECTED` | The assigned driver or either collector records a non-blank incident/reason, actor, and UTC time. |
| `IN_PROGRESS → COMPLETED` | Every stop must be `COLLECTED` or `NOT_COLLECTED`; the full route, resources, outcomes, and results are retained; otherwise the completion is refused with one `STOP_PENDING` issue per pending stop. |
| `DRAFT/PLANNED/IN_PROGRESS → CANCELLED` | A planner supplies a reason. Existing stop results are retained, and cancellation is not deletion. |
| Terminal states | `COMPLETED` and `CANCELLED` have no ordinary execution transition. A privileged result correction uses the separate audited operation in US-31. |

There is intentionally no automatic `PLANNED → DRAFT` transition and no optimizer-driven start,
completion, or rerouting. If a confirmed tour can no longer be executed, the planner uses the
documented cancellation path rather than bypassing lifecycle validation.
