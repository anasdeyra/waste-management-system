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
