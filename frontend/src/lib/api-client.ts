import type { ApiError, CsrfTokenResponse } from '@/types/api'

const API_BASE: string = import.meta.env.VITE_API_BASE_URL ?? ''

/** Error thrown for every non-2xx API response. */
export class ApiRequestError extends Error {
  status: number
  body: ApiError | null

  constructor(status: number, body: ApiError | null) {
    super(body?.message ?? `Request failed (${status})`)
    this.status = status
    this.body = body
  }
}

function readCookie(name: string): string | undefined {
  const prefix = `${name}=`
  const found = document.cookie.split('; ').find((c) => c.startsWith(prefix))
  return found ? decodeURIComponent(found.slice(prefix.length)) : undefined
}

async function ensureCsrfToken(): Promise<string> {
  const existing = readCookie('XSRF-TOKEN')
  if (existing) return existing
  const res = await fetch(`${API_BASE}/api/auth/csrf`, { credentials: 'same-origin' })
  const body = (await res.json()) as CsrfTokenResponse
  return body.token
}

async function toRequestError(res: Response): Promise<ApiRequestError> {
  let body: ApiError | null = null
  try {
    body = (await res.json()) as ApiError
  } catch {
    body = null
  }
  return new ApiRequestError(res.status, body)
}

interface FetchInit {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
}

/** Calls the backend with session cookie and CSRF header; JSON in, JSON out. */
export async function apiFetch<T>(path: string, init: FetchInit = {}): Promise<T> {
  const method = init.method ?? 'GET'
  const headers: Record<string, string> = {}
  if (method !== 'GET') headers['X-XSRF-TOKEN'] = await ensureCsrfToken()
  if (init.body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    credentials: 'same-origin',
    body: init.body !== undefined ? JSON.stringify(init.body) : undefined,
  })

  if (!res.ok) throw await toRequestError(res)
  if (res.status === 204) return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** POSTs raw CSV text (Content-Type text/csv); errors carry body.rowErrors when the file is invalid. */
export async function apiPostCsv<T>(path: string, csv: string): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { 'X-XSRF-TOKEN': await ensureCsrfToken(), 'Content-Type': 'text/csv' },
    credentials: 'same-origin',
    body: csv,
  })
  if (!res.ok) throw await toRequestError(res)
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** Downloads a file through the session; the name comes from Content-Disposition or the fallback. */
export async function apiDownload(path: string, fallbackName: string): Promise<void> {
  const res = await fetch(`${API_BASE}${path}`, { credentials: 'same-origin' })
  if (!res.ok) throw await toRequestError(res)
  const disposition = res.headers.get('Content-Disposition') ?? ''
  const match = /filename="([^"]+)"/.exec(disposition)
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = match ? match[1] : fallbackName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
