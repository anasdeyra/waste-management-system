import { useRef, useState } from 'react'
import { toast } from 'sonner'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useImportScenarioCsv } from '@/hooks/use-evaluation-explorer'
import { ApiRequestError } from '@/lib/api-client'

/** Uploads a scenario CSV; an invalid file imports nothing and lists its row errors. */
export function CsvImportCard({ onImported }: { onImported: (batchId: string) => void }) {
  const [file, setFile] = useState<File | null>(null)
  const [label, setLabel] = useState('')
  const input = useRef<HTMLInputElement>(null)
  const importCsv = useImportScenarioCsv()
  const error = importCsv.error instanceof ApiRequestError ? importCsv.error : null

  const submit = async () => {
    if (!file) return
    const csv = await file.text()
    importCsv.mutate(
      { csv, fileName: file.name, label: label.trim() || undefined },
      {
        onSuccess: (detail) => {
          toast.success('Scenarios imported')
          setFile(null)
          setLabel('')
          if (input.current) input.current.value = ''
          onImported(detail.batch.id)
        },
      },
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Import scenario CSV</CardTitle>
        <CardDescription>Columns and rules: docs/evaluation-csv.md. An invalid file imports nothing.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <div className="flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1">
            <Label htmlFor="csv-file">Scenario file</Label>
            <Input
              id="csv-file"
              ref={input}
              type="file"
              accept=".csv,text/csv"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="csv-label">Label (optional)</Label>
            <Input id="csv-label" value={label} onChange={(e) => setLabel(e.target.value)} maxLength={120} />
          </div>
          <Button onClick={submit} disabled={!file || importCsv.isPending}>
            {importCsv.isPending ? 'Importing…' : 'Import'}
          </Button>
        </div>
        {error && (
          <Alert variant="destructive">
            <AlertTitle>{error.message}</AlertTitle>
            <AlertDescription>
              {error.body?.rowErrors ? (
                <div className="flex flex-col gap-2">
                  <p>Nothing was imported. Fix these rows and try again.</p>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Row</TableHead>
                        <TableHead>Column</TableHead>
                        <TableHead>Message</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {error.body.rowErrors.map((e, i) => (
                        <TableRow key={i}>
                          <TableCell>{e.row}</TableCell>
                          <TableCell>{e.column ?? '—'}</TableCell>
                          <TableCell>{e.message}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              ) : (
                (error.body?.fieldErrors ?? []).map((f) => `${f.field}: ${f.message}`).join('; ')
              )}
            </AlertDescription>
          </Alert>
        )}
      </CardContent>
    </Card>
  )
}
