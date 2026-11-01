import { Route, Routes } from 'react-router-dom'

import { AppShell } from '@/components/shared/app-shell'
import { RequireAuth, RequireRole } from '@/components/shared/require-auth'
import CategoriesPage from '@/pages/waste-categories-page'
import HomePage from '@/pages/home-page'
import LoginPage from '@/pages/login-page'
import NotFoundPage from '@/pages/not-found-page'
import UsersAdminPage from '@/pages/users-admin-page'

/** Application routes. Role gates here are UX only; the backend enforces access. */
export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<HomePage />} />
        <Route
          path="waste-categories"
          element={
            <RequireRole role="PLANNER">
              <CategoriesPage />
            </RequireRole>
          }
        />
        <Route
          path="admin/users"
          element={
            <RequireRole role="ADMIN">
              <UsersAdminPage />
            </RequireRole>
          }
        />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
