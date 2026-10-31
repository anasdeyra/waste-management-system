import type {
  Agent,
  CollectionPoint,
  DashboardSummary,
  LoginRequest,
  LoginResponse,
  OptimizationComparison,
  OptimizationRun,
  Tour,
  UserProfile,
  Vehicle,
  WasteCategory,
} from '@/types/api'

/**
 * Base path of the Spring Boot REST API.
 *
 * In development Vite proxies `/api` to `http://localhost:8080` (see
 * `vite.config.ts`), so the frontend can use a relative base path and avoid
 * CORS entirely.
 */
export const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** Key under which the bearer token is kept in `localStorage`. */
const TOKEN_STORAGE_KEY = 'wastems.auth.token'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function getToken(): string | null {
  return window.localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function setToken(token: string | null): void {
  if (token === null) {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY)
  } else {
    window.localStorage.setItem(TOKEN_STORAGE_KEY, token)
  }
}

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
  /** Send the bearer token. Defaults to `true`. */
  authenticated?: boolean
}

/**
 * Thin wrapper around `fetch` that adds the bearer token, serialises JSON
 * bodies and normalises backend errors into {@link ApiError}.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, authenticated = true, headers, ...rest } = options

  const requestHeaders = new Headers(headers)
  if (body !== undefined && !(body instanceof FormData)) {
    requestHeaders.set('Content-Type', 'application/json')
  }
  if (authenticated) {
    const token = getToken()
    if (token) {
      requestHeaders.set('Authorization', `Bearer ${token}`)
    }
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: requestHeaders,
    body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body),
  })

  if (response.status === 204) {
    return undefined as T
  }

  const isJson = response.headers.get('Content-Type')?.includes('application/json')
  const payload = isJson ? await response.json() : await response.text()

  if (!response.ok) {
    const message =
      typeof payload === 'string' && payload.length > 0
        ? payload
        : ((payload as { message?: string })?.message ?? `Request failed with status ${response.status}`)
    throw new ApiError(response.status, message)
  }

  return payload as T
}

export const api = {
  auth: {
    login: (credentials: LoginRequest) =>
      apiRequest<LoginResponse>('/auth/login', {
        method: 'POST',
        body: credentials,
        authenticated: false,
      }),
    logout: () => apiRequest<void>('/auth/logout', { method: 'POST' }),
    me: () => apiRequest<UserProfile>('/auth/me'),
  },

  dashboard: {
    summary: () => apiRequest<DashboardSummary>('/dashboard/summary'),
  },

  wasteCategories: {
    list: () => apiRequest<WasteCategory[]>('/waste-categories'),
  },

  collectionPoints: {
    list: () => apiRequest<CollectionPoint[]>('/collection-points'),
  },

  vehicles: {
    list: () => apiRequest<Vehicle[]>('/vehicles'),
  },

  agents: {
    list: () => apiRequest<Agent[]>('/agents'),
  },

  tours: {
    list: () => apiRequest<Tour[]>('/tours'),
    byId: (id: number) => apiRequest<Tour>(`/tours/${id}`),
  },

  optimization: {
    runs: () => apiRequest<OptimizationRun[]>('/optimization/runs'),
    compare: (scenarioName: string) =>
      apiRequest<OptimizationComparison>(
        `/optimization/comparison?scenario=${encodeURIComponent(scenarioName)}`,
      ),
  },
}
