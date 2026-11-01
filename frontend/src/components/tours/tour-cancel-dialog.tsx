import { useState } from 'react'
import { toast } from 'sonner'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useCancelTour } from '@/hooks/use-tours'
import type { Tour } from '@/types/api'

function CancelForm({ tour, onDone }: { tour: Tour; onDone: () => void }) {
  const cancel = useCancelTour()
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)
  const submit = () => {
    setError(null)
    cancel.mutate(
      { id: tour.id, body: { reason: reason.trim() } },
      {
        onSuccess: () => {
          toast.success('Tour cancelled.')
          onDone()
        },
        onError: (e) => setError(e.message),
      },
    )
  }
  return (
    <>
      <div className="flex flex-col gap-1">
        <Label htmlFor="cancel-reason">Cancellation reason</Label>
        <Textarea
          id="cancel-reason"
          value={reason}
          maxLength={500}
          onChange={(e) => setReason(e.target.value)}
        />
      </div>
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <DialogFooter>
        <Button variant="outline" onClick={onDone}>
          Keep tour
        </Button>
        <Button
          variant="destructive"
          disabled={reason.trim() === '' || cancel.isPending}
          onClick={submit}
        >
          Confirm cancellation
        </Button>
      </DialogFooter>
    </>
  )
}

/** Cancels a draft, planned or in-progress tour with a reason (US-30); the record is kept. */
export function TourCancelDialog({ tour }: { tour: Tour }) {
  const [open, setOpen] = useState(false)
  return (
    <>
      <Button variant="destructive" onClick={() => setOpen(true)}>
        Cancel tour
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel this tour?</DialogTitle>
            <DialogDescription>
              Cancelling keeps the tour, its stops and every recorded outcome. It cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <CancelForm tour={tour} onDone={() => setOpen(false)} />
        </DialogContent>
      </Dialog>
    </>
  )
}
