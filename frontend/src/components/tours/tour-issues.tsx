import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import type { Tour } from '@/types/api'

/** Structural validation of the draft (the server computes it; this only displays it). */
export function TourIssues({ tour }: { tour: Tour }) {
  if (tour.structurallyValid) {
    return (
      <Alert>
        <AlertTitle>Ready</AlertTitle>
        <AlertDescription>
          Draft is structurally complete. Optimization and confirmation arrive in later sprints.
        </AlertDescription>
      </Alert>
    )
  }
  return (
    <Alert variant="destructive">
      <AlertTitle>
        {tour.validationIssues.length} issue{tour.validationIssues.length === 1 ? '' : 's'} to resolve
      </AlertTitle>
      <AlertDescription>
        <ul className="list-disc pl-4">
          {tour.validationIssues.map((i, idx) => (
            <li key={`${i.code}-${idx}`}>{i.message}</li>
          ))}
        </ul>
      </AlertDescription>
    </Alert>
  )
}
