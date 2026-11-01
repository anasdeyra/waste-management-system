import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'

import App from './App'
import { Toaster } from '@/components/ui/sonner'
import { currentUserQueryKey } from '@/hooks/use-auth'
import { ApiRequestError } from '@/lib/api-client'
import './index.css'

function dropSessionOn401(err: unknown) {
  if (err instanceof ApiRequestError && err.status === 401) {
    queryClient.setQueryData(currentUserQueryKey, null)
  }
}

const queryClient: QueryClient = new QueryClient({
  queryCache: new QueryCache({ onError: dropSessionOn401 }),
  mutationCache: new MutationCache({
    onError: (err, _vars, _ctx, mutation) => {
      if (mutation.options.mutationKey?.[0] !== 'login') dropSessionOn401(err)
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        if (error instanceof ApiRequestError && [401, 403, 404].includes(error.status)) return false
        return failureCount < 1
      },
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
        <Toaster richColors position="top-right" />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
