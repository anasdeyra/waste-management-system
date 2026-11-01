# Activity diagram — Tour planning and execution lifecycle

## 1. Purpose

This activity covers UC08–UC19: draft preparation, three-method evaluation, planner review,
confirmation, assigned-crew execution, completion, and cancellation. It implements the lifecycle
chosen in D-025 and the terminal stop outcomes in D-031.

The main path shows an accepted optimized proposal. Rejecting the proposal does not block the
planner from confirming an otherwise valid draft with its current input order.

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
    FEASIBLE -- No --> KEEP
    FEASIBLE -- Yes --> ACCEPT{Planner accepts proposal?}
    ACCEPT -- Yes --> APPLY[Apply selected proposal order and road-leg reference to DRAFT]
    ACCEPT -- No --> KEEP[Keep current DRAFT order; retain evaluation]
    APPLY --> CONFIRM{All confirmation validations pass?}
    KEEP --> CONFIRM
    CONFIRM -- No --> FIX
    CONFIRM -- Yes --> PLANNED[Confirm tour as PLANNED and lock ordinary structural edits]

    PLANNED --> START_TOUR{Assigned crew starts and resources remain valid?}
    START_TOUR -- No, wait --> PLANNED
    START_TOUR -- Yes --> ACTIVE[Set tour IN_PROGRESS]
    ACTIVE --> NEXT{PENDING stop remaining?}
    NEXT -- Yes --> OUTCOME{Stop collected?}
    OUTCOME -- Yes --> COLLECTED[Set COLLECTED; record category quantities, actor and UTC time]
    OUTCOME -- No --> NOT_COLLECTED[Set NOT_COLLECTED; record incident or reason, actor and UTC time]
    COLLECTED --> NEXT
    NOT_COLLECTED --> NEXT
    NEXT -- No, all outcomes terminal --> COMPLETE[Assigned crew completes tour]
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
| `DRAFT → PLANNED` | Revalidate stops/order, active points, road route, one available vehicle, one available driver, zero to two distinct available collectors, and total mixed-category demand within capacity. |
| `PLANNED → IN_PROGRESS` | Only an assigned crew member may start the tour; resource availability is checked again. “No, wait” leaves it `PLANNED` and does not silently change resources. |
| `PENDING → COLLECTED` | The assigned driver or either collector records non-negative actual quantities by waste category, actor, and UTC time. |
| `PENDING → NOT_COLLECTED` | The assigned driver or either collector records a non-blank incident/reason, actor, and UTC time. |
| `IN_PROGRESS → COMPLETED` | Every stop must be `COLLECTED` or `NOT_COLLECTED`; the full route, resources, outcomes, and results are retained. |
| `DRAFT/PLANNED/IN_PROGRESS → CANCELLED` | A planner supplies a reason. Existing stop results are retained, and cancellation is not deletion. |
| Terminal states | `COMPLETED` and `CANCELLED` have no ordinary execution transition. A privileged result correction uses the separate audited operation in US-31. |

There is intentionally no automatic `PLANNED → DRAFT` transition and no optimizer-driven start,
completion, or rerouting. If a confirmed tour can no longer be executed, the planner uses the
documented cancellation path rather than bypassing lifecycle validation.
