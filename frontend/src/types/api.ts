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
}

/** web.dto.CreateUserRequest */
export interface CreateUserRequest {
  username: string
  password: string
  role: AppRole
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
