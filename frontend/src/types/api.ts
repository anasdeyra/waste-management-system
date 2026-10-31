/**
 * Typed representation of the Spring Boot REST contract.
 *
 * These DTOs mirror the backend records (`backend/src/main/java/com/ase/wastems`).
 * Keeping them in one place makes the API boundary explicit and lets both
 * TypeScript and Java evolve against the same contract.
 */

/** Roles recognised by the backend (see `Role` enum). */
export type Role = 'ADMIN' | 'MANAGER' | 'AGENT'

export type UserProfile = {
  id: number
  username: string
  fullName: string
  roles: Role[]
}

export type LoginRequest = {
  username: string
  password: string
}

export type LoginResponse = {
  token: string
  user: UserProfile
}

/** Lifecycle of a collection point. */
export type CollectionPointStatus = 'ACTIVE' | 'INACTIVE' | 'FULL'

export type CollectionPoint = {
  id: number
  name: string
  /** Free-text address of the point. */
  address: string
  /** Synthetic planning coordinate, x axis, in kilometres. */
  x: number
  /** Synthetic planning coordinate, y axis, in kilometres. */
  y: number
  capacityKg: number
  currentLoadKg: number
  status: CollectionPointStatus
  wasteCategoryId: number
  wasteCategoryName: string
}

export type VehicleStatus = 'AVAILABLE' | 'IN_SERVICE' | 'MAINTENANCE'

export type Vehicle = {
  id: number
  plate: string
  description: string
  capacityKg: number
  status: VehicleStatus
}

export type AgentRole = 'DRIVER' | 'COLLECTOR'

export type AgentStatus = 'AVAILABLE' | 'ASSIGNED' | 'UNAVAILABLE'

export type Agent = {
  id: number
  fullName: string
  phone: string
  role: AgentRole
  status: AgentStatus
}

export type WasteCategory = {
  id: number
  name: string
  description: string
  recyclable: boolean
}

export type TourStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export type TourStop = {
  id: number
  sequence: number
  collectionPointId: number
  collectionPointName: string
  /** Expected kilograms to collect at this stop. */
  expectedKg: number
  /** Actually collected kilograms, null until the stop is executed. */
  collectedKg: number | null
  status: 'PENDING' | 'DONE' | 'SKIPPED'
}

export type Tour = {
  id: number
  plannedDate: string
  status: TourStatus
  vehicleId: number
  vehiclePlate: string
  driverId: number
  driverName: string
  collectorIds: number[]
  collectorNames: string[]
  totalDistanceKm: number
  estimatedDurationMinutes: number
  totalLoadKg: number
  stops: TourStop[]
}

/** Planning method used to produce a tour (see decision D-008). */
export type PlanningMethod = 'BASELINE' | 'NEAREST_NEIGHBOR' | 'OPTIMIZED'

export type OptimizationRun = {
  id: number
  createdAt: string
  scenarioName: string
  pointCount: number
  method: PlanningMethod
  totalDistanceKm: number
  estimatedDurationMinutes: number
  executionTimeMillis: number
  constraintViolations: number
}

export type OptimizationComparison = {
  scenarioName: string
  pointCount: number
  results: OptimizationRun[]
  baselineDistanceKm: number
  optimizedDistanceKm: number
  distanceImprovementPercent: number
  baselineDurationMinutes: number
  optimizedDurationMinutes: number
  durationImprovementPercent: number
}

export type DashboardSummary = {
  collectionPoints: number
  activeCollectionPoints: number
  vehicles: number
  availableVehicles: number
  agents: number
  availableAgents: number
  toursPlannedToday: number
  toursInProgress: number
  /** Total kilograms collected across completed tours. */
  collectedKg: number
}
