import { Route, Routes } from 'react-router-dom'

import { AppShell } from '@/components/shared/app-shell'
import { RequireAuth, RequireRole } from '@/components/shared/require-auth'
import AgentsPage from '@/pages/agents-page'
import CollectionPointsPage from '@/pages/collection-points-page'
import CategoriesPage from '@/pages/waste-categories-page'
import DataImportPage from '@/pages/data-import-page'
import HomePage from '@/pages/home-page'
import LoginPage from '@/pages/login-page'
import MyTourPage from '@/pages/my-tour-page'
import MyToursPage from '@/pages/my-tours-page'
import NotFoundPage from '@/pages/not-found-page'
import TourPlannerPage from '@/pages/tour-planner-page'
import ToursPage from '@/pages/tours-page'
import UsersAdminPage from '@/pages/users-admin-page'
import VehiclesPage from '@/pages/vehicles-page'

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
          path="my-tours"
          element={
            <RequireRole role="AGENT">
              <MyToursPage />
            </RequireRole>
          }
        />
        <Route
          path="my-tours/:id"
          element={
            <RequireRole role="AGENT">
              <MyTourPage />
            </RequireRole>
          }
        />
        <Route
          path="collection-points"
          element={
            <RequireRole role="PLANNER">
              <CollectionPointsPage />
            </RequireRole>
          }
        />
        <Route
          path="tours"
          element={
            <RequireRole role="PLANNER">
              <ToursPage />
            </RequireRole>
          }
        />
        <Route
          path="tours/:id"
          element={
            <RequireRole role="PLANNER">
              <TourPlannerPage />
            </RequireRole>
          }
        />
        <Route
          path="waste-categories"
          element={
            <RequireRole role="PLANNER">
              <CategoriesPage />
            </RequireRole>
          }
        />
        <Route
          path="vehicles"
          element={
            <RequireRole role="PLANNER">
              <VehiclesPage />
            </RequireRole>
          }
        />
        <Route
          path="agents"
          element={
            <RequireRole role="PLANNER">
              <AgentsPage />
            </RequireRole>
          }
        />
        <Route
          path="admin/data-import"
          element={
            <RequireRole role="ADMIN">
              <DataImportPage />
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
