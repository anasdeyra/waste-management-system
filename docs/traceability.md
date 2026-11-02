# Requirement traceability (FR → user story → use case → code → test)

This matrix links every functional requirement of `docs/01-analysis.md` §4 to the user stories that
realise it (`docs/05-scrum.md`), the use cases that describe it (`docs/03-uml/01-use-cases.md`), the main
code that implements it and the automated tests that check it. It is the Part 1 traceability rule of
`AGENTS.md` made concrete.

How to read it. Paths are relative to `backend/src/main/java/com/ase/wastems/` for backend code,
`backend/src/test/java/com/ase/wastems/` for tests and `frontend/src/` for the user interface. The
user stories of a requirement are derived from the **Traceability** lines of `docs/05-scrum.md`, the use
cases from the table of `docs/03-uml/01-use-cases.md`; the code and test files were chosen by opening the
controllers, services and test classes that call them. Frontend behaviour is verified by build, lint and
HTTP checks only (D-040), so frontend-only requirements have no automated UI test; they are listed under
Gaps.

| FR | Requirement | User stories | Use cases | Backend (main) | Frontend | Tests |
|---|---|---|---|---|---|---|
| FR-01 | Authenticate and protect operations | US-01, US-02, US-03, US-04, US-05, US-16, US-26, US-28, US-31, US-39 | UC01, UC25 | `web/AuthController.java`, `config/SecurityConfig.java`, `security/ActiveAccountFilter.java` | `pages/login-page.tsx`, `components/shared/require-auth.tsx` | `web/AuthControllerTest.java`, `web/CsrfCookieFlowTest.java` |
| FR-02 | Role-based authorization | US-03, US-05, US-06, US-07, US-08, US-10, US-11, US-12, US-13, US-14, US-15, US-16, US-17, US-18, US-20, US-22, US-25, US-26, US-27, US-28, US-29, US-30, US-31, US-37 | UC01, UC13, UC15 | `config/SecurityConfig.java`, `security/JsonAccessDeniedHandler.java` | `components/shared/require-auth.tsx` | `config/SecurityConfigTest.java`, `web/TourAssignedAccessControllerTest.java` |
| FR-03 | Manage waste categories | US-06, US-07, US-09, US-10, US-19, US-23, US-26, US-28, US-32, US-35 | UC03 | `service/WasteCategoryService.java`, `web/WasteCategoryController.java` | `pages/waste-categories-page.tsx` | `web/WasteCategoryControllerTest.java`, `service/WasteCategoryServiceTest.java` |
| FR-04 | Manage collection points | US-08, US-09, US-10, US-12, US-21, US-36 | UC04 | `service/CollectionPointService.java`, `web/CollectionPointController.java` | `pages/collection-points-page.tsx`, `components/collection-points/point-form.tsx` | `web/CollectionPointControllerTest.java`, `service/CollectionPointServiceTest.java` |
| FR-05 | Collection-point data and provenance | US-08, US-09, US-10, US-22, US-24, US-29, US-30 | UC04 | `domain/CollectionPoint.java` | `components/collection-points/point-details.tsx` | `repository/CollectionPointRepositoryTest.java`, `web/CollectionPointControllerTest.java` |
| FR-06 | Demand per point in kilograms | US-06, US-07, US-08, US-09, US-11, US-20, US-22, US-23, US-24, US-36 | UC04, UC09 | `domain/DemandEntry.java`, `service/CollectionPointService.java` | `components/collection-points/demand-editor.tsx` | `service/CollectionPointServiceTest.java` |
| FR-07 | Manage vehicles | US-11, US-13, US-14, US-19, US-23, US-34, US-35, US-36, US-37, US-38 | UC06 | `service/VehicleService.java`, `web/VehicleController.java` | `pages/vehicles-page.tsx`, `components/fleet/vehicle-form-dialog.tsx` | `web/VehicleControllerTest.java`, `repository/VehicleRepositoryTest.java` |
| FR-08 | Vehicle capacity validation | US-11, US-13, US-20, US-25 | UC06, UC09, UC12 | `domain/Vehicle.java`, `service/TourService.java` | `components/tours/capacity-indicator.tsx` | `web/VehicleControllerTest.java`, `web/TourControllerTest.java` |
| FR-09 | Manage agents and account links | US-04, US-10, US-11, US-15, US-16, US-17, US-21, US-24, US-32, US-33, US-35, US-36, US-37, US-38 | UC02, UC07 | `service/AgentService.java`, `web/AgentController.java` | `pages/agents-page.tsx`, `components/agents/agent-form-dialog.tsx` | `web/AgentControllerTest.java`, `web/AgentAccountLinkControllerTest.java` |
| FR-10 | Assign vehicle, driver and collectors | US-14, US-17, US-20, US-25, US-27, US-33, US-34 | UC09, UC12, UC14 | `service/TourService.java`, `domain/Tour.java` | `components/tours/tour-assignment-panel.tsx` | `service/FleetAvailabilityServiceTest.java`, `web/TourControllerTest.java` |
| FR-11 | Manage tours and their lifecycle | US-01, US-04, US-14, US-15, US-16, US-17, US-18, US-19, US-20, US-25, US-27, US-29, US-30, US-35, US-39 | UC08, UC09, UC12, UC14, UC16, UC17 | `domain/Tour.java`, `web/TourController.java`, `service/TourService.java` | `pages/tours-page.tsx`, `pages/tour-planner-page.tsx` | `web/TourControllerTest.java`, `domain/TourTest.java`, `web/TourConfirmationControllerTest.java` |
| FR-12 | Agents consult assigned tours | US-03, US-08, US-09, US-23, US-26, US-32, US-33, US-34, US-37 | UC13 | `web/TourExecutionController.java` | `pages/my-tours-page.tsx`, `pages/my-tour-page.tsx` | `web/TourAssignedAccessControllerTest.java` |
| FR-13 | Record stop outcomes and completion | US-27, US-28, US-29, US-31, US-35 | UC14, UC15, UC16, UC18 | `service/TourExecutionService.java` | `components/execution/tour-execution-panel.tsx`, `components/execution/stop-outcome-form.tsx` | `web/TourStopOutcomeControllerTest.java`, `web/TourStartControllerTest.java`, `web/TourCompletionControllerTest.java` |
| FR-14 | Operational history and corrections | US-07, US-12, US-29, US-30, US-31, US-32 | UC16, UC17, UC18, UC19 | `service/TourHistoryQuery.java`, `service/TourService.java` | `pages/history-page.tsx`, `components/tours/stop-correction-dialog.tsx` | `web/TourHistoryControllerTest.java`, `web/TourCancellationControllerTest.java`, `web/StopOutcomeCorrectionControllerTest.java` |
| FR-15 | Frozen scenario as optimizer input | US-11, US-19, US-21, US-37 | UC08, UC10, UC23 | `service/EvaluationService.java`, `optimization/PlanningScenario.java` | `components/tours/evaluation-panel.tsx` | `service/EvaluationServiceTest.java`, `optimization/TourOptimizationModuleTest.java` |
| FR-16 | Optimized tour proposal | US-21, US-22, US-24 | UC10, UC11 | `optimization/TourOptimizationModule.java`, `optimization/NearestNeighborConstruction.java`, `optimization/TwoOptImprovement.java` | `components/tours/proposal-actions.tsx` | `optimization/NearestNeighborConstructionTest.java`, `optimization/TwoOptImprovementTest.java` |
| FR-17 | Planner accepts or rejects proposal | US-24, US-25 | UC11, UC12 | `domain/ProposalDecision.java`, `service/TourService.java` | `components/tours/proposal-actions.tsx`, `components/tours/tour-confirm-panel.tsx` | `web/TourReviewControllerTest.java`, `web/TourConfirmationControllerTest.java` |
| FR-18 | Expose optimization inputs and results | US-22, US-23 | UC10 | `domain/MethodResult.java`, `web/EvaluationController.java` | `components/tours/evaluation-indicator-table.tsx` | `web/EvaluationControllerTest.java` |
| FR-19 | Compare against the input-order baseline and the sweep baseline | US-19, US-21, US-23, US-40 | UC10 | `optimization/InputOrderBaseline.java`, `service/EvaluationComparison.java`, `optimization/SweepBaseline.java` | `components/tours/evaluation-comparison.tsx` | `service/EvaluationComparisonTest.java`, `optimization/SweepBaselineTest.java` |
| FR-20 | Distance, duration and time indicators | US-21, US-23, US-36, US-38, US-40 | UC10, UC22, UC24 | `service/EvaluationAggregates.java`, `service/EvaluationExportService.java` | `components/evaluations/batch-summary.tsx` | `service/EvaluationAggregatesTest.java`, `web/EvaluationExplorerControllerTest.java`, `web/EvaluationExportControllerTest.java` |
| FR-21 | Retain results with provenance | US-21, US-24, US-36, US-38, US-40 | UC10, UC11, UC22, UC24 | `domain/EvaluationRun.java`, `domain/EvaluationBatch.java`, `config/SchemaUpgrade.java` | `pages/evaluation-run-page.tsx` | `domain/EvaluationBatchTest.java`, `service/ExperimentServiceTest.java`, `config/SchemaUpgradeTest.java` |
| FR-22 | Operational map with filters | US-09, US-12 | UC05 | `web/CollectionPointController.java` | `pages/collection-points-page.tsx`, `components/maps/base-map.tsx`, `components/maps/points-map-layer.tsx` | none (D-040: build + HTTP checks only) |
| FR-23 | Map-based editing and selection | US-10, US-19 | UC04, UC08 | `web/CollectionPointController.java` | `components/maps/picked-location-layer.tsx`, `components/collection-points/point-form.tsx`, `components/tours/tour-candidate-list.tsx` | `web/CollectionPointControllerTest.java` (server-side coordinate validation); map interaction: none (D-040) |
| FR-24 | Road routes on maps | US-23, US-26, US-28 | UC10, UC13, UC15 | `routing/GraphHopperRoadRouter.java` | `components/maps/route-layers.tsx`, `components/maps/tour-stops-layer.tsx`, `components/maps/execution-stops-layer.tsx` | `routing/GraphHopperRoadRouterTest.java` |
| FR-25 | NYC traffic overlay, display only | US-34 | UC21 | `traffic/TrafficOverlayService.java`, `web/TrafficController.java` | `components/maps/traffic-layer.tsx`, `components/maps/traffic-panel.tsx` | `web/TrafficControllerTest.java`, `traffic/TrafficIsolationTest.java` |
| FR-26 | Real MN01 seed data and demo | US-33 | UC20 | `service/Mn01ImportService.java`, `service/DemoDataService.java` | `pages/data-import-page.tsx` | `service/Mn01ImportServiceTest.java`, `service/DemoDataServiceTest.java` |
| FR-27 | User account management | US-01, US-03, US-04, US-05, US-16 | UC02 | `service/UserAdminService.java`, `web/UserAdminController.java` | `pages/users-admin-page.tsx` | `web/UserAdminControllerTest.java` |
| FR-28 | Operational and evaluation dashboards | US-35, US-36 | UC22 | `service/DashboardService.java`, `web/DashboardController.java` | `pages/dashboard-page.tsx` | `web/DashboardControllerTest.java` |
| FR-29 | Evaluation CSV import and export | US-37, US-38 | UC23, UC24 | `service/ScenarioCsvImportService.java`, `service/EvaluationExportService.java` | `components/evaluations/csv-import-card.tsx` | `web/ScenarioCsvImportControllerTest.java`, `web/EvaluationExportControllerTest.java` |
| FR-30 | Audit log | US-03, US-31, US-39 | UC18, UC25 | `service/AuditService.java`, `web/AuditEventController.java` | `pages/audit-log-page.tsx` | `web/AuditEventControllerTest.java`, `service/AuditServiceTest.java` |

## Coverage checks

- All 39 user stories (US-01 to US-39) and all 25 use cases (UC01 to UC25) appear in the table; a
  script check in the S10 done-check verifies each identifier and that every file path above exists.
- Every requirement has at least one backend or frontend file and at least one automated test, or an
  explicit statement that there is none.

## Gaps

- **FR-22 (operational map with filters):** purely visual map behaviour; verified by build, lint and
  HTTP checks, with no automated browser test (D-040).
- **FR-23 (map-based editing and selection):** the server-side coordinate validation is tested; picking
  a location on the map, and the synchronized selection, are not covered by an automated test (D-040).
- **FR-24 (road routes on maps):** the routing and the retained geometry are tested; the rendering of
  the route layers in the browser is not (D-040).
- **All frontend requirements:** the frontend has no unit or end-to-end tests; the type contract in
  `frontend/src/types/api.ts` and `npm run build` are the automated checks.
