/**
 * Typed API contract. Each interface mirrors a backend Java record (D-018) and must stay in
 * sync with it.
 */

/** com.ase.wastems.domain.AppRole */
export type AppRole = 'ADMIN' | 'PLANNER' | 'AGENT'

/** web.dto.CurrentUserResponse */
export interface CurrentUser {
  id: string
  username: string
  role: AppRole
  agentId?: string
}

/** web.dto.LoginRequest */
export interface LoginRequest {
  username: string
  password: string
}

/** web.dto.CsrfTokenResponse */
export interface CsrfTokenResponse {
  headerName: string
  token: string
}

/** web.dto.UserResponse */
export interface UserSummary {
  id: string
  username: string
  role: AppRole
  active: boolean
  linkedAgentId?: string
  linkedAgentName?: string
}

/** web.dto.CreateUserRequest */
export interface CreateUserRequest {
  username: string
  password: string
  role: AppRole
  agentId?: string
}

/** web.dto.ActiveStatusRequest */
export interface ActiveStatusRequest {
  active: boolean
}

/** web.dto.WasteCategoryResponse (description is omitted when null) */
export interface WasteCategory {
  id: string
  name: string
  description?: string
  active: boolean
}

/** web.dto.WasteCategoryRequest */
export interface WasteCategoryRequest {
  name: string
  description?: string
}

/** web.dto.FieldErrorDetail */
export interface FieldErrorDetail {
  field: string
  message: string
}

/** web.dto.ApiError */
export interface ApiError {
  status: number
  error: string
  message: string
  fieldErrors?: FieldErrorDetail[]
  issues?: TourIssue[]
  rowErrors?: CsvRowError[]
}

/** web.dto.CsvRowErrorResponse (row 1 is the CSV header) */
export interface CsvRowError {
  row: number
  column?: string
  message: string
}

/** domain.CollectionPointStatus */
export type CollectionPointStatus = 'ACTIVE' | 'INACTIVE'

/** domain.DataOrigin */
export type DataOrigin = 'IMPORTED_REAL' | 'SYNTHETIC' | 'USER_ENTERED'

/** web.dto.DemandEntryResponse */
export interface DemandEntry {
  id: string
  categoryId: string
  categoryName: string
  categoryActive: boolean
  quantityKg: number
  origin: DataOrigin
}

/** web.dto.CollectionPointResponse (source fields are omitted when null) */
export interface CollectionPoint {
  id: string
  name: string
  longitude: number
  latitude: number
  status: CollectionPointStatus
  origin: DataOrigin
  sourceDataset?: string
  sourceIdentifier?: string
  sourceOwnershipType?: string
  optimizationEligible: boolean
  demands: DemandEntry[]
  totalDemandKg: number
}

/** web.dto.DepotResponse */
export interface Depot {
  id: string
  name: string
  longitude: number
  latitude: number
  origin: DataOrigin
  sourceDataset?: string
  sourceIdentifier?: string
}

/** web.dto.DemandEntryRequest */
export interface DemandEntryRequest {
  categoryId: string
  quantityKg: number
}

/** web.dto.CreateCollectionPointRequest */
export interface CreateCollectionPointRequest {
  name: string
  longitude: number
  latitude: number
  demands: DemandEntryRequest[]
}

/** web.dto.UpdateCollectionPointRequest */
export interface UpdateCollectionPointRequest {
  name: string
  longitude: number
  latitude: number
}

/** web.dto.CollectionPointStatusRequest */
export interface CollectionPointStatusRequest {
  status: CollectionPointStatus
}

/** domain.ResourceStatus */
export type ResourceStatus = 'AVAILABLE' | 'UNAVAILABLE'

/** web.dto.VehicleResponse */
export interface Vehicle {
  id: string
  registration: string
  capacityKg: number
  status: ResourceStatus
  origin: DataOrigin
}

/** web.dto.VehicleRequest */
export interface VehicleRequest {
  registration: string
  capacityKg: number
}

/** web.dto.LinkedUserResponse */
export interface LinkedUser {
  id: string
  username: string
  active: boolean
}

/** web.dto.AgentResponse (linkedUser is omitted when not linked) */
export interface Agent {
  id: string
  displayName: string
  status: ResourceStatus
  origin: DataOrigin
  linkedUser?: LinkedUser
}

/** web.dto.AgentRequest */
export interface AgentRequest {
  displayName: string
}

/** web.dto.ResourceStatusRequest */
export interface ResourceStatusRequest {
  status: ResourceStatus
}

/** web.dto.LinkUserAccountRequest */
export interface LinkUserAccountRequest {
  userId: string
}

/** service.ImportTrigger */
export type ImportTrigger = 'STARTUP' | 'MANUAL'

/** service.DatasetImportResult (dsnyEligibleRowCount is omitted for depots) */
export interface DatasetImportResult {
  sourceDataset: string
  sourceFile: string
  rowCount: number
  dsnyEligibleRowCount?: number
  created: number
  updated: number
  existing: number
  skipped: number
  sha256: string
  snapshotId: string
}

/** web.dto.SourceSnapshotResponse */
export interface SourceSnapshot {
  sourceDataset: string
  sourceFile: string
  rowCount: number
  sha256: string
  snapshotId: string
  importedAt: string
}

/** web.dto.ImportReportResponse */
export interface ImportReport {
  trigger: ImportTrigger
  startedAt: string
  finishedAt: string
  baskets: DatasetImportResult
  depots: DatasetImportResult
  syntheticDemandsCreated: number
  syntheticDemandCategory: string
  warnings: string[]
  snapshots: SourceSnapshot[]
}

// ---------------------------------------------------------------------------
// Tours (S4)
// ---------------------------------------------------------------------------

/** domain.TourStatus */
export type TourStatus = 'DRAFT' | 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

/** domain.TourStopStatus */
export type TourStopStatus = 'PENDING' | 'COLLECTED' | 'NOT_COLLECTED'

/** domain.TourIssueCode */
export type TourIssueCode =
  | 'NO_STOPS'
  | 'NO_VEHICLE'
  | 'NO_DRIVER'
  | 'VEHICLE_UNAVAILABLE'
  | 'DRIVER_UNAVAILABLE'
  | 'COLLECTOR_UNAVAILABLE'
  | 'INACTIVE_STOP'
  | 'INELIGIBLE_STOP'
  | 'OVER_CAPACITY'
  | 'PLANNING_DATE_PAST'
  | 'ROUTE_MISSING'
  | 'ROUTE_OUTDATED'
  | 'STOP_PENDING'

/** web.dto.StopDemandResponse */
export interface StopDemand {
  id: string
  categoryId: string
  categoryName: string
  plannedQuantityKg: number
}

/** web.dto.CollectedQuantityResponse */
export interface CollectedQuantity {
  id: string
  categoryId: string
  categoryName: string
  actualQuantityKg: number
}

/** web.dto.CorrectionQuantityResponse */
export interface CorrectionQuantity {
  categoryId: string
  categoryName: string
  quantityKg: number
}

/** web.dto.OutcomeCorrectionResponse (null fields are omitted) */
export interface OutcomeCorrection {
  id: string
  correctedAt: string
  correctedBy: string
  reason: string
  previousStatus: TourStopStatus
  previousIncidentReason?: string
  previousRecordedBy?: string
  previousRecordedAt?: string
  previousQuantities: CorrectionQuantity[]
  newStatus: TourStopStatus
  newIncidentReason?: string
  newQuantities: CorrectionQuantity[]
}

/** web.dto.TourStopResponse (source fields are omitted when null) */
export interface TourStop {
  id: string
  visitOrder: number
  status: TourStopStatus
  collectionPointId: string
  name: string
  longitude: number
  latitude: number
  pointStatus: CollectionPointStatus
  optimizationEligible: boolean
  sourceDataset?: string
  sourceIdentifier?: string
  sourceOwnershipType?: string
  plannedDemands: StopDemand[]
  plannedTotalKg: number
  incidentReason?: string
  outcomeRecordedAt?: string
  outcomeRecordedBy?: string
  actualQuantities: CollectedQuantity[]
  actualTotalKg?: number
  corrections: OutcomeCorrection[]
}

/** web.dto.TourCapacityResponse (vehicle fields are omitted when no vehicle is assigned) */
export interface TourCapacity {
  totalPlannedDemandKg: number
  vehicleCapacityKg?: number
  remainingKg?: number
  overCapacity: boolean
}

/** web.dto.TourIssueResponse */
export interface TourIssue {
  code: TourIssueCode
  message: string
}

/** web.dto.TourResponse (vehicle, driver and route are omitted when absent) */
export interface Tour {
  id: string
  planningDate: string
  status: TourStatus
  origin: DataOrigin
  revision: number
  createdAt: string
  updatedAt: string
  depot: Depot
  vehicle?: Vehicle
  driver?: Agent
  collectors: Agent[]
  stops: TourStop[]
  capacity: TourCapacity
  structurallyValid: boolean
  validationIssues: TourIssue[]
  route?: TourRoute
  confirmationIssues: TourIssue[]
  readyToConfirm: boolean
  startedAt?: string
  startedBy?: string
  completedAt?: string
  completedBy?: string
  startIssues: TourIssue[]
  readyToStart: boolean
  readyToComplete: boolean
  cancellationReason?: string
  cancelledAt?: string
  cancelledBy?: string
  cancelledFromStatus?: TourStatus
  cancellable: boolean
  correctable: boolean
}

/** web.dto.CancelTourRequest */
export interface CancelTourRequest {
  reason: string
}

/** web.dto.CollectedQuantityRequest */
export interface CollectedQuantityRequest {
  categoryId: string
  actualQuantityKg: number
}

/** web.dto.OutcomeCorrectionRequest */
export interface OutcomeCorrectionRequest {
  status: 'COLLECTED' | 'NOT_COLLECTED'
  quantities?: CollectedQuantityRequest[]
  incidentReason?: string
  reason: string
}

/** web.dto.StopOutcomeRequest */
export interface StopOutcomeRequest {
  status: Exclude<TourStopStatus, 'PENDING'>
  quantities?: CollectedQuantityRequest[]
  incidentReason?: string
}

/** web.dto.TourSummaryResponse */
export interface TourSummary {
  id: string
  planningDate: string
  status: TourStatus
  origin: DataOrigin
  depotId: string
  depotName: string
  stopCount: number
  terminalStopCount: number
  totalPlannedDemandKg: number
  vehicleId?: string
  vehicleRegistration?: string
  vehicleCapacityKg?: number
  driverId?: string
  driverName?: string
  collectorCount: number
  overCapacity: boolean
  structurallyValid: boolean
  issueCount: number
  updatedAt: string
  routeMethod?: PlanningMethod
  completedAt?: string
  cancelledAt?: string
  cancellationReason?: string
  correctionCount: number
}

/** web.dto.TourRequest */
export interface TourRequest {
  planningDate: string
  depotId: string
}

/** web.dto.AddTourStopRequest */
export interface AddTourStopRequest {
  collectionPointId: string
}

/** web.dto.ReorderTourStopsRequest */
export interface ReorderTourStopsRequest {
  stopIds: string[]
}

/** web.dto.TourAssignmentRequest */
export interface TourAssignmentRequest {
  vehicleId?: string
  driverId?: string
  collectorIds: string[]
}

// --- Evaluation runs (S5; review UI S6) ---------------------------------

/** optimization.PlanningMethod */
export type PlanningMethod = 'INPUT_ORDER' | 'SWEEP' | 'NEAREST_NEIGHBOR' | 'NN_PLUS_2_OPT'

/** domain.EvaluationStatus */
export type EvaluationStatus = 'COMPLETED' | 'ROUTING_FAILED'

/** domain.ScenarioKind */
export type ScenarioKind = 'TOUR_DRAFT' | 'EXPERIMENT' | 'CSV_IMPORT'

/** Properties of one road leg feature (node 0 is the depot) */
export interface RoadLegProperties {
  sequence: number
  fromIndex: number
  toIndex: number
  distanceM: number
  travelTimeS: number
}

/** One directed road leg as GeoJSON ([longitude, latitude] coordinates) */
export interface RoadLegFeature {
  type: 'Feature'
  geometry: { type: 'LineString'; coordinates: [number, number][] }
  properties: RoadLegProperties
}

/** Road-following legs of one method's route */
export interface RoadLegCollection {
  type: 'FeatureCollection'
  features: RoadLegFeature[]
}

/** domain.ProposalDecision */
export type ProposalDecision = 'ACCEPTED' | 'REJECTED'

/** web.dto.ProposalDecisionRequest */
export interface ProposalDecisionRequest {
  evaluationRunId: string
}

/** web.dto.TourRouteResponse: the road route attached to a tour */
export interface TourRoute {
  evaluationRunId: string
  method: PlanningMethod
  roadGraphVersion: string
  distanceM: number
  durationS: number
  legs: RoadLegCollection
}

/** web.dto.EvaluationDepotResponse */
export interface EvaluationDepot {
  name: string
  longitude: number
  latitude: number
  sourceIdentifier?: string
}

/** web.dto.EvaluationStopResponse */
export interface EvaluationStop {
  inputIndex: number
  collectionPointId: string
  sourceDataset?: string
  sourceIdentifier?: string
  name: string
  longitude: number
  latitude: number
  demandKg: number
  demandBreakdown?: string
}

/** web.dto.MethodResultResponse */
export interface MethodResult {
  method: PlanningMethod
  feasible: boolean
  infeasibilityReason?: string
  visitOrder: number[]
  collectionPointOrder: string[]
  distanceM?: number
  travelTimeS?: number
  durationS?: number
  executionTimeMs: number
  coverageOk: boolean
  capacityOk: boolean
  algorithmConfiguration: string
  distanceChangePct?: number
  durationChangePct?: number
  /** Only for NEAREST_NEIGHBOR and NN_PLUS_2_OPT; absent when the run has no feasible sweep (D-052). */
  distanceChangeVsSweepPct?: number
  durationChangeVsSweepPct?: number
  legs?: RoadLegCollection
}

/** web.dto.EvaluationRunResponse */
export interface EvaluationRun {
  id: string
  scenarioKind: ScenarioKind
  tourId?: string
  tourRevision?: number
  batchId?: string
  scenarioKey?: string
  demandProfile?: string
  seed?: number
  repetition?: number
  status: EvaluationStatus
  failureCode?: string
  failureMessage?: string
  createdAt: string
  createdBy: string
  roadGraphVersion: string
  routingEngine: string
  sourceSnapshotIds: string[]
  depot: EvaluationDepot
  vehicleCapacityKg: number
  serviceTimeSeconds: number
  pointCount: number
  totalDemandKg: number
  stops: EvaluationStop[]
  results: MethodResult[]
  decision?: ProposalDecision
  decidedBy?: string
  decidedAt?: string
}

/** web.dto.MethodSummaryResponse */
export interface MethodSummary {
  method: PlanningMethod
  feasible: boolean
  distanceM?: number
  durationS?: number
}

/** web.dto.EvaluationRunSummaryResponse */
export interface EvaluationRunSummary {
  id: string
  tourId?: string
  tourRevision?: number
  status: EvaluationStatus
  failureCode?: string
  createdAt: string
  createdBy: string
  roadGraphVersion: string
  pointCount: number
  results: MethodSummary[]
  decision?: ProposalDecision
  decidedBy?: string
  decidedAt?: string
}

/** web.dto.EvaluationMatrixResponse */
export interface EvaluationMatrix {
  runId: string
  roadGraphVersion: string
  nodeCount: number
  nodeLabels: string[]
  distanceM: number[][]
  travelTimeS: number[][]
}

/** web.dto.DemoLoadReportResponse.DemoCountResponse */
export interface DemoCount {
  created: number
  existing: number
}

/** web.dto.DemoLoadReportResponse.DemoTourStateResponse */
export interface DemoTourState {
  key: string
  tourId: string
  targetStatus: TourStatus
  status: TourStatus
}

/** web.dto.DemoLoadReportResponse (US-33) */
export interface DemoLoadReport {
  trigger: 'STARTUP' | 'MANUAL'
  loadedAt: string
  categories: DemoCount
  demandEntries: DemoCount
  vehicles: DemoCount
  accounts: DemoCount
  agents: DemoCount
  tours: DemoCount
  tourStates: DemoTourState[]
  warnings: string[]
}

/** domain.AuditAction (the catalogue is in D-049) */
export type AuditAction =
  | 'SIGN_IN'
  | 'SIGN_OUT'
  | 'USER_CREATED'
  | 'USER_STATUS_CHANGED'
  | 'CATEGORY_CREATED'
  | 'CATEGORY_UPDATED'
  | 'CATEGORY_STATUS_CHANGED'
  | 'POINT_CREATED'
  | 'POINT_UPDATED'
  | 'POINT_STATUS_CHANGED'
  | 'DEMAND_ADDED'
  | 'DEMAND_UPDATED'
  | 'DEMAND_REMOVED'
  | 'VEHICLE_CREATED'
  | 'VEHICLE_UPDATED'
  | 'VEHICLE_STATUS_CHANGED'
  | 'AGENT_CREATED'
  | 'AGENT_UPDATED'
  | 'AGENT_STATUS_CHANGED'
  | 'AGENT_ACCOUNT_LINKED'
  | 'AGENT_ACCOUNT_UNLINKED'
  | 'DATA_IMPORTED'
  | 'TOUR_CREATED'
  | 'TOUR_UPDATED'
  | 'TOUR_STOP_ADDED'
  | 'TOUR_STOP_REMOVED'
  | 'TOUR_STOPS_REORDERED'
  | 'TOUR_ASSIGNED'
  | 'TOUR_EVALUATED'
  | 'PROPOSAL_ACCEPTED'
  | 'PROPOSAL_REJECTED'
  | 'TOUR_CONFIRMED'
  | 'TOUR_STARTED'
  | 'STOP_OUTCOME_RECORDED'
  | 'TOUR_COMPLETED'
  | 'TOUR_CANCELLED'
  | 'STOP_OUTCOME_CORRECTED'
  | 'DEMO_LOADED'
  | 'EVALUATION_EXPERIMENT_RUN'
  | 'EVALUATION_SCENARIOS_IMPORTED'

/** domain.AuditEntityType */
export type AuditEntityType =
  | 'USER'
  | 'WASTE_CATEGORY'
  | 'COLLECTION_POINT'
  | 'VEHICLE'
  | 'AGENT'
  | 'TOUR'
  | 'DATA_IMPORT'
  | 'DEMO_DATA'
  | 'EVALUATION_BATCH'

/** domain.AuditOutcome */
export type AuditOutcome = 'SUCCESS' | 'FAILURE'

/** web.dto.AuditEventResponse (null fields are omitted) */
export interface AuditEvent {
  id: string
  occurredAt: string
  actor: string
  action: AuditAction
  entityType?: AuditEntityType
  entityId?: string
  outcome: AuditOutcome
  details?: string
}

/** web.dto.AuditEventPageResponse */
export interface AuditEventPage {
  items: AuditEvent[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** service.DemandProfile */
export type DemandProfile = 'UNIFORM_LOW' | 'UNIFORM_HIGH' | 'HOTSPOT'

/** domain.BatchStatus */
export type BatchStatus = 'RUNNING' | 'COMPLETED' | 'ABORTED'

/** web.dto.ExperimentRequest */
export interface ExperimentRequest {
  profiles?: DemandProfile[]
  seeds?: number[]
  sizes?: number[]
  repetitions?: number
  label?: string
}

/** web.dto.EvaluationBatchResponse */
export interface EvaluationBatch {
  id: string
  kind: ScenarioKind
  label: string
  status: BatchStatus
  canonical: boolean
  parameters?: string
  demandProfiles: string[]
  seeds: number[]
  sizes: number[]
  repetitions?: number
  vehicleCapacityKg?: number
  sourceSnapshotIds: string[]
  roadGraphVersion?: string
  routingEngine?: string
  environment?: string
  fileName?: string
  fileSha256?: string
  fileRowCount?: number
  scenarioCount: number
  runCount: number
  completedRunCount: number
  failedRunCount: number
  createdAt: string
  createdBy: string
  finishedAt?: string
  failureMessage?: string
}

/** web.dto.EvaluationMethodBriefResponse */
export interface EvaluationMethodBrief {
  method: PlanningMethod
  feasible: boolean
  distanceM?: number
  durationS?: number
  executionTimeMs: number
  coverageOk: boolean
  capacityOk: boolean
  distanceChangePct?: number
  durationChangePct?: number
  distanceChangeVsSweepPct?: number
  durationChangeVsSweepPct?: number
}

/** web.dto.EvaluationRunListItemResponse */
export interface EvaluationRunListItem {
  id: string
  scenarioKind: ScenarioKind
  batchId?: string
  scenarioKey?: string
  demandProfile?: string
  seed?: number
  repetition?: number
  tourId?: string
  pointCount: number
  status: EvaluationStatus
  failureCode?: string
  createdAt: string
  createdBy: string
  roadGraphVersion: string
  totalDemandKg: number
  vehicleCapacityKg: number
  results: EvaluationMethodBrief[]
}

/** web.dto.EvaluationRunPageResponse */
export interface EvaluationRunPage {
  items: EvaluationRunListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** web.dto.EvaluationAggregateResponse */
export interface EvaluationAggregate {
  group: 'ALL' | 'SIZE' | 'PROFILE'
  value: string
  method: PlanningMethod
  runs: number
  comparableRuns: number
  excludedRuns: number
  meanDistanceM?: number
  meanDurationS?: number
  meanDistanceChangePct?: number
  minDistanceChangePct?: number
  maxDistanceChangePct?: number
  meanDurationChangePct?: number
  minDurationChangePct?: number
  maxDurationChangePct?: number
  meanExecutionTimeMs?: number
  comparableVsSweepRuns: number
  meanDistanceChangeVsSweepPct?: number
  minDistanceChangeVsSweepPct?: number
  maxDistanceChangeVsSweepPct?: number
  meanDurationChangeVsSweepPct?: number
}

/** web.dto.EvaluationBatchDetailResponse */
export interface EvaluationBatchDetail {
  batch: EvaluationBatch
  aggregates: EvaluationAggregate[]
  scenarioKeys: string[]
}

/** web.dto.OperationsDashboardResponse.ResourceCountsResponse */
export interface ResourceCounts {
  total: number
  available: number
  unavailable: number
}

/** web.dto.OperationsDashboardResponse.PointCountsResponse */
export interface PointCounts {
  total: number
  active: number
  inactive: number
}

/** web.dto.OperationsDashboardResponse.TourCountsResponse */
export interface TourCounts {
  total: number
  draft: number
  planned: number
  inProgress: number
  completed: number
  cancelled: number
}

/** web.dto.OperationsDashboardResponse.StopCountsResponse */
export interface StopCounts {
  total: number
  pending: number
  collected: number
  notCollected: number
}

/** web.dto.OperationsDashboardResponse: recorded data, not live tracking */
export interface OperationsDashboard {
  from?: string
  to?: string
  generatedAt: string
  vehicles: ResourceCounts
  agents: ResourceCounts
  collectionPoints: PointCounts
  tours: TourCounts
  stops: StopCounts
  collectedKg: number
}

/** traffic.TrafficLinkStatus */
export type TrafficLinkStatus = 'OBSERVED' | 'STALE' | 'NO_DATA'

/** traffic.SpeedBand */
export type SpeedBand = 'BELOW_10_MPH' | 'FROM_10_TO_25_MPH' | 'FROM_25_MPH'

/** traffic.GeometrySource */
export type GeometrySource = 'ENCODED_POLYLINE' | 'LINK_POINTS'

/** web.dto.TrafficOverlayResponse.TrafficCountsResponse */
export interface TrafficCounts {
  total: number
  observed: number
  stale: number
  noData: number
  invalid: number
}

/** web.dto.TrafficOverlayResponse.TrafficLinkResponse (display only) */
export interface TrafficLink {
  linkId: string
  name?: string
  borough?: string
  owner?: string
  status: TrafficLinkStatus
  speedMph?: number
  travelTimeS?: number
  speedBand?: SpeedBand
  dataAsOf: string
  dataAsOfLocal: string
  geometrySource: GeometrySource
  geometryTruncated: boolean
  geometry: { type: 'LineString'; coordinates: [number, number][] }
}

/** web.dto.TrafficOverlayResponse: stored NYC DOT snapshot, never a cost input */
export interface TrafficOverlay {
  available: boolean
  unavailableReason?: string
  snapshotId?: string
  source?: string
  sourceUrl?: string
  retrievedAt?: string
  observedAt?: string
  observedAtLocal?: string
  timeZone?: string
  ageMinutes?: number
  staleAfterMinutes: number
  counts: TrafficCounts
  coverageNote: string
  displayOnlyNote: string
  attribution: string
  disclaimer: string
  links: TrafficLink[]
}
