# Sequence diagram — Evaluate, review, and accept a tour proposal

## 1. Scenario

This is the core intelligent-planning scenario for UC10 and UC11 (US-21–US-24). A planner has
already created a `DRAFT`, selected its stops/input order, and assigned a vehicle and crew. The
planner requests a three-method comparison, inspects feasible results, and either applies the
NN + 2-opt proposal to that same draft or keeps the input order. Both decisions are refused when
the draft changed since the run.

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
        API->>Eval: evaluate(frozenScenario)
        Eval->>Router: buildMatrix(points, depot, roadGraphVersion)
        Router->>GH: Route every directed leg on the versioned graph
        GH-->>Router: Directed distances, base times and road geometry
        Router-->>Eval: Directed cost matrices, leg geometry and snapshot IDs

        alt Directed matrix or required leg unavailable
            Eval->>Store: Retain failed scenario/run and diagnostic without changing the draft
            Store-->>Eval: evaluationId
            Eval-->>API: Routing/infeasibility explanation, no accepted operational change
            API-->>UI: 201 Created with status ROUTING_FAILED and no method results
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
            API-->>UI: 201 Created (Location /api/evaluations/{evaluationId}) with the comparison
            UI-->>Planner: Display route layers, visit orders, indicators and any method errors

            alt Planner accepts a feasible NN + 2-opt proposal
                Planner->>UI: Accept proposal for this draft
                UI->>API: POST /api/tours/{tourId}/proposal-acceptances {evaluationRunId}
                Note over API: Require the same planner authorization and an editable DRAFT
                API->>Tours: acceptOptimizedProposal(tourId, evaluationRunId)
                Tours->>Store: Lock/read draft and retained evaluation
                Tours->>Tours: Verify run identity, revision/stop match and NN + 2-opt feasibility
                alt Draft changed since the run (stale proposal)
                    Tours-->>API: Conflict, nothing changed
                    API-->>UI: 409 Conflict (evaluate the tour again)
                    UI-->>Planner: Show that the proposal is stale
                else Proposal matches the current draft
                    Tours->>Store: Save proposal stop order, road-route reference and ACCEPTED decision atomically
                    Store-->>Tours: Updated DRAFT (revision + 1)
                    Tours-->>API: Updated tour, still not PLANNED or started
                    API-->>UI: 200 OK with accepted draft order and road route
                    UI-->>Planner: Confirm acceptance and retain all compared results
                end
            else Planner keeps the input order (rejects the proposal)
                Planner->>UI: Keep input order
                UI->>API: POST /api/tours/{tourId}/proposal-rejections {evaluationRunId}
                API->>Tours: rejectProposal(tourId, evaluationRunId)
                Tours->>Store: Keep stop order, attach input-order road route, record REJECTED decision
                Store-->>Tours: Updated DRAFT (revision + 1, same order)
                API-->>UI: 200 OK, order unchanged, all three results retained
                UI-->>Planner: Show the kept input order and its road route
            else No feasible proposal or planner leaves the run undecided
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
- Updating the operational draft and recording the decision on the retained run occur in one
  transaction. The same transaction writes the `PROPOSAL_ACCEPTED` audit event (S8, US-39).
- A proposal whose run revision or stop list differs from the current draft is stale. It is
  refused with 409 and changes nothing.

### Failure or rejection

- If the road snapshot itself cannot be loaded (missing or checksum mismatch), the API answers 503 and retains nothing.
- Invalid inputs, unavailable directed legs, or optimization failures never produce a partially
  accepted tour and never corrupt existing operational data.
- Keeping the input order (rejecting the proposal) leaves the draft order unchanged, attaches
  the input-order road route of the same run (needed for confirmation), and retains all three
  results. Leaving the run undecided changes nothing.
- NYC DOT traffic is absent from this sequence by design: it is a display overlay, not a cost
  input or automatic rerouting trigger.

## 4. Confirmation (UC12, US-25)

`POST /api/tours/{tourId}/confirmation` revalidates the structural issues, the planning date
and the road route (`ROUTE_MISSING`/`ROUTE_OUTDATED`). It answers 422 with the list of
blocking issues, or 200 with the tour `PLANNED`. After that, every structural edit,
evaluation and proposal decision answers 409.
