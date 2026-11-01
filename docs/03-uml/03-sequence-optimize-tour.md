# Sequence diagram — Evaluate, review, and accept a tour proposal

## 1. Scenario

This is the core intelligent-planning scenario for UC10 and UC11 (US-21–US-24). A planner has
already created a `DRAFT`, selected its stops/input order, and assigned a vehicle and crew. The
planner requests a three-method comparison, inspects feasible results, and may explicitly apply
the NN + 2-opt proposal to that same draft.

The diagram distinguishes:

- the **operational draft**, which must not be changed merely by running an evaluation;
- the **frozen planning scenario**, shared identically by all three methods;
- **retained evaluation results**, including failures and rejected proposals; and
- **planner acceptance**, which is a separate, authorized operation.

GraphHopper and the optimization module are participants in this behavioral view even though they
are intentionally excluded from the operational-domain class diagram (D-033).

## 2. Diagram

```mermaid
sequenceDiagram
    autonumber
    actor Planner as Collection manager / Planner
    participant UI as React SPA
    participant API as Optimization REST API
    participant Tours as TourService
    participant Eval as EvaluationService
    participant Router as DirectedCostMatrixProvider
    participant GH as Local GraphHopper
    participant Optimizer as TourOptimizationModule
    participant Store as Transactional persistence

    Planner->>UI: Request evaluation for draft tour
    UI->>API: POST /api/tours/{tourId}/evaluations
    Note over API: Require authenticated ADMIN or PLANNER
    API->>Tours: loadDraftAndBuildScenario(tourId)
    Tours->>Store: Read draft, depot, stops, demands, vehicle and crew
    Store-->>Tours: Operational planning input
    Tours->>Tours: Freeze scenario and validate points, order, capacity and resources

    alt Invalid scenario
        Tours-->>API: Validation errors with useful constraint details
        API-->>UI: 422 Unprocessable Content
        UI-->>Planner: Show errors, draft remains unchanged
    else Valid scenario
        Tours->>Eval: evaluate(frozenScenario)
        Eval->>Router: buildMatrix(points, depot, roadGraphVersion)
        Router->>GH: Route every directed leg on the versioned graph
        GH-->>Router: Directed distances, base times and road geometry
        Router-->>Eval: Directed cost matrices, leg geometry and snapshot IDs

        alt Directed matrix or required leg unavailable
            Eval->>Store: Retain failed scenario/run and diagnostic without changing the draft
            Store-->>Eval: evaluationId
            Eval-->>API: Routing/infeasibility explanation, no accepted operational change
            API-->>UI: Failure result without a valid proposal
            UI-->>Planner: Show missing routing input or infeasible scenario
        else Complete directed scenario available
            Note over Eval,Optimizer: Evaluate deterministically in method order, no parallel-runtime assumption
            Eval->>Eval: Evaluate the frozen input/selection-order baseline
            Eval->>Optimizer: plan(NEAREST_NEIGHBOR, scenario, matrices)
            Optimizer-->>Eval: Capacity-aware nearest-neighbor candidate
            Eval->>Optimizer: plan(NN_PLUS_2_OPT, scenario, matrices)
            Note over Optimizer: Recalculate all internal leg costs for every reversed segment
            Optimizer-->>Eval: Locally improved ordered candidate

            Eval->>Eval: Validate coverage/capacity and calculate distance, duration and runtime
            Eval->>Store: Atomically retain scenario, methods, configurations, snapshots and results
            Store-->>Eval: evaluationId
            Eval-->>API: Three-method comparison and per-method feasibility
            API-->>UI: 200 OK with evaluationId and comparison
            UI-->>Planner: Display route layers, visit orders, indicators and any method errors

            alt Planner accepts a feasible NN + 2-opt proposal
                Planner->>UI: Accept proposal for this draft
                UI->>API: POST /api/tours/{tourId}/proposal-acceptances {evaluationId}
                Note over API: Require the same planner authorization and an editable DRAFT
                API->>Tours: acceptOptimizedProposal(tourId, evaluationId)
                Tours->>Store: Lock/read draft and retained evaluation
                Tours->>Tours: Verify proposal identity, feasibility and scenario/draft match
                Tours->>Store: Save optimized stop order, road-leg reference and audit event atomically
                Store-->>Tours: Updated DRAFT
                Tours-->>API: Updated tour, still not PLANNED or started
                API-->>UI: 200 OK with accepted draft order
                UI-->>Planner: Confirm acceptance and retain all compared results
            else Planner rejects/discards or no feasible optimized proposal
                Planner->>UI: Reject proposal or correct the draft inputs
                UI-->>Planner: Keep current draft order unchanged, retained evaluation remains available
            end
        end
    end
```

## 3. Main postconditions and alternatives

### Successful evaluation

- The scenario and all method outcomes are retained with source/road snapshots and algorithm
  configuration, even when the planner later rejects the proposal.
- Every feasible candidate starts/ends at the depot and covers exactly the same selected points.
  Capacity and coverage are revalidated after improvement.
- Distance, estimated duration, and execution time are calculated under common rules. No global
  optimality or numerical improvement is assumed.

### Accepted proposal

- Acceptance applies only the identified feasible NN + 2-opt order and road-route reference to the
  matching editable draft.
- The tour remains `DRAFT`; US-25/UC12 must separately validate and confirm it as `PLANNED`.
- Updating the operational draft and writing its audit event occur in one transaction.

### Failure or rejection

- Invalid inputs, unavailable directed legs, or optimization failures never produce a partially
  accepted tour and never corrupt existing operational data.
- Rejecting/discarding a proposal leaves the current draft order unchanged while retaining the
  evaluation for traceability.
- NYC DOT traffic is absent from this sequence by design: it is a display overlay, not a cost
  input or automatic rerouting trigger.
