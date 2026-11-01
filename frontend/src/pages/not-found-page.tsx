import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'

/** Shown for unknown routes. */
export default function NotFoundPage() {
  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-2xl font-semibold">Page not found</h1>
      <Button asChild>
        <Link to="/">Back to home</Link>
      </Button>
    </main>
  )
}
