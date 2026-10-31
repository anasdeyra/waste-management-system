import { Navigate, Route, Routes } from 'react-router-dom'

import { AppShell } from '@/components/layout/app-shell'
import { useAuth } from '@/hooks/use-auth'
import { AgentsPage } from '@/pages/agents'
import { CollectionPointsPage } from '@/pages/collection-points'
import { DashboardPage } from '@/pages/dashboard'
import { LoginPage } from '@/pages/login'
import { NotFoundPage } from '@/pages/not-found'
import { OptimizationPage } from '@/pages/optimization'
import { ToursPage } from '@/pages/tours'
import { VehiclesPage } from '@/pages/vehicles'
import { WasteCategoriesPage } from '@/pages/waste-categories'

export default function App() {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <Routes>
      <Route path="/login" element={<Navigate to="/" replace />} />
      <Route element={<AppShell />}>
        <Route index element={<DashboardPage />} />
        <Route path="collection-points" element={<CollectionPointsPage />} />
        <Route path="vehicles" element={<VehiclesPage />} />
        <Route path="agents" element={<AgentsPage />} />
        <Route path="tours" element={<ToursPage />} />
        <Route path="waste-categories" element={<WasteCategoriesPage />} />
        <Route path="optimization" element={<OptimizationPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
