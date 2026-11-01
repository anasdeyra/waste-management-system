import { Boxes } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

/**
 * Placeholder of the WasteMS single-page application.
 *
 * <p>The application shell, the routing and the pages of the five functional areas are
 * built in the feature sprints described in `docs/05-scrum.md`.
 */
export default function App() {
  return (
    <main className="flex min-h-svh items-center justify-center bg-muted/30 p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <div className="mb-2 flex size-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Boxes className="size-5" />
          </div>
          <CardTitle>WasteMS</CardTitle>
          <CardDescription>
            Intelligent Waste Management System — React + Tailwind CSS + shadcn/ui, talking to the
            Spring Boot API on port 8080.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <Button disabled>Action</Button>
          <p className="text-sm text-muted-foreground">
            Framework scaffold only. The domain features are implemented in the upcoming sprints.
          </p>
        </CardContent>
      </Card>
    </main>
  )
}
