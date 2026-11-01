import type { AuditAction, AuditEntityType, AuditOutcome } from '@/types/api'

/** Every audited action, in the order of the catalogue (D-049). */
export const AUDIT_ACTIONS: AuditAction[] = [
  'SIGN_IN',
  'SIGN_OUT',
  'USER_CREATED',
  'USER_STATUS_CHANGED',
  'CATEGORY_CREATED',
  'CATEGORY_UPDATED',
  'CATEGORY_STATUS_CHANGED',
  'POINT_CREATED',
  'POINT_UPDATED',
  'POINT_STATUS_CHANGED',
  'DEMAND_ADDED',
  'DEMAND_UPDATED',
  'DEMAND_REMOVED',
  'VEHICLE_CREATED',
  'VEHICLE_UPDATED',
  'VEHICLE_STATUS_CHANGED',
  'AGENT_CREATED',
  'AGENT_UPDATED',
  'AGENT_STATUS_CHANGED',
  'AGENT_ACCOUNT_LINKED',
  'AGENT_ACCOUNT_UNLINKED',
  'DATA_IMPORTED',
  'TOUR_CREATED',
  'TOUR_UPDATED',
  'TOUR_STOP_ADDED',
  'TOUR_STOP_REMOVED',
  'TOUR_STOPS_REORDERED',
  'TOUR_ASSIGNED',
  'TOUR_EVALUATED',
  'PROPOSAL_ACCEPTED',
  'PROPOSAL_REJECTED',
  'TOUR_CONFIRMED',
  'TOUR_STARTED',
  'STOP_OUTCOME_RECORDED',
  'TOUR_COMPLETED',
  'TOUR_CANCELLED',
  'STOP_OUTCOME_CORRECTED',
  'DEMO_LOADED',
]

export const AUDIT_ENTITY_TYPES: AuditEntityType[] = [
  'USER',
  'WASTE_CATEGORY',
  'COLLECTION_POINT',
  'VEHICLE',
  'AGENT',
  'TOUR',
  'DATA_IMPORT',
  'DEMO_DATA',
]

export const AUDIT_OUTCOMES: AuditOutcome[] = ['SUCCESS', 'FAILURE']

export const AUDIT_ACTION_LABELS: Record<AuditAction, string> = {
  SIGN_IN: 'Sign-in',
  SIGN_OUT: 'Sign-out',
  USER_CREATED: 'User created',
  USER_STATUS_CHANGED: 'User status changed',
  CATEGORY_CREATED: 'Category created',
  CATEGORY_UPDATED: 'Category updated',
  CATEGORY_STATUS_CHANGED: 'Category status changed',
  POINT_CREATED: 'Collection point created',
  POINT_UPDATED: 'Collection point updated',
  POINT_STATUS_CHANGED: 'Collection point status changed',
  DEMAND_ADDED: 'Demand added',
  DEMAND_UPDATED: 'Demand updated',
  DEMAND_REMOVED: 'Demand removed',
  VEHICLE_CREATED: 'Vehicle created',
  VEHICLE_UPDATED: 'Vehicle updated',
  VEHICLE_STATUS_CHANGED: 'Vehicle status changed',
  AGENT_CREATED: 'Agent created',
  AGENT_UPDATED: 'Agent updated',
  AGENT_STATUS_CHANGED: 'Agent status changed',
  AGENT_ACCOUNT_LINKED: 'Agent account linked',
  AGENT_ACCOUNT_UNLINKED: 'Agent account unlinked',
  DATA_IMPORTED: 'MN01 data imported',
  TOUR_CREATED: 'Tour created',
  TOUR_UPDATED: 'Tour updated',
  TOUR_STOP_ADDED: 'Tour stop added',
  TOUR_STOP_REMOVED: 'Tour stop removed',
  TOUR_STOPS_REORDERED: 'Tour stops reordered',
  TOUR_ASSIGNED: 'Tour assigned',
  TOUR_EVALUATED: 'Tour evaluated',
  PROPOSAL_ACCEPTED: 'Proposal accepted',
  PROPOSAL_REJECTED: 'Proposal rejected',
  TOUR_CONFIRMED: 'Tour confirmed',
  TOUR_STARTED: 'Tour started',
  STOP_OUTCOME_RECORDED: 'Outcome recorded',
  TOUR_COMPLETED: 'Tour completed',
  TOUR_CANCELLED: 'Tour cancelled',
  STOP_OUTCOME_CORRECTED: 'Outcome corrected',
  DEMO_LOADED: 'Demonstration loaded',
}

/** Converts a datetime-local value (entered in local time) to a UTC ISO instant. */
export function toUtcIso(localDateTime: string): string | undefined {
  if (!localDateTime) return undefined
  const d = new Date(localDateTime)
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString()
}
