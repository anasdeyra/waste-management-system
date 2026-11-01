import { RefreshCw } from 'lucide-react'
import { toast } from 'sonner'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useImportReport, useRunImport } from '@/hooks/use-data-import'
import type { DatasetImportResult } from '@/types/api'

function local(iso: string): string {
  return new Date(iso).toLocaleString()
}

function ResultRow({ label, r }: { label: string; r: DatasetImportResult }) {
  return (
    <TableRow>
      <TableCell className="font-medium">{label}</TableCell>
      <TableCell>{r.rowCount}</TableCell>
      <TableCell>{r.dsnyEligibleRowCount ?? '—'}</TableCell>
      <TableCell>{r.created}</TableCell>
      <TableCell>{r.existing}</TableCell>
      <TableCell>{r.updated}</TableCell>
      <TableCell>{r.skipped}</TableCell>
    </TableRow>
  )
}

/** ADMIN page: source snapshot identity and the last MN01 import run. */
export default function DataImportPage() {
  const report = useImportReport()
  const run = useRunImport()

  const rerun = () =>
    run.mutate(undefined, {
      onSuccess: (r) =>
        toast.success(
          `Import done: baskets created ${r.baskets.created}, existing ${r.baskets.existing}; depots created ${r.depots.created}, existing ${r.depots.existing}`,
        ),
      onError: (err) => toast.error(err.message),
    })

  const rerunButton = (
    <Button onClick={rerun} disabled={run.isPending}>
      <RefreshCw />
      Re-run import
    </Button>
  )

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">Data import</h1>
        {report.data && rerunButton}
      </div>
      <p className="text-sm text-muted-foreground">
        Locations are real NYC Open Data records (D-019); demand values are synthetic. Only{' '}
        <code>DSNY …</code> basket types are eligible for optimization.
      </p>

      {report.isPending && <Skeleton className="h-40 w-full" />}
      {report.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load the import report</AlertTitle>
          <AlertDescription>{report.error.message}</AlertDescription>
        </Alert>
      )}
      {report.data === null && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>No import has run since the backend started</EmptyTitle>
            <EmptyDescription>Run the import to load the MN01 snapshots.</EmptyDescription>
          </EmptyHeader>
          {rerunButton}
        </Empty>
      )}
      {report.data && (
        <>
          <Card>
            <CardHeader>
              <CardTitle>MN01 source snapshots</CardTitle>
              <CardDescription>Provenance of the included source files.</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Dataset</TableHead>
                    <TableHead>File</TableHead>
                    <TableHead>Rows</TableHead>
                    <TableHead>Snapshot ID</TableHead>
                    <TableHead>Imported at</TableHead>
                    <TableHead>SHA-256</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {report.data.snapshots.map((s) => (
                    <TableRow key={s.sourceDataset}>
                      <TableCell className="font-medium">{s.sourceDataset}</TableCell>
                      <TableCell>{s.sourceFile}</TableCell>
                      <TableCell>{s.rowCount}</TableCell>
                      <TableCell>{s.snapshotId}</TableCell>
                      <TableCell>{local(s.importedAt)}</TableCell>
                      <TableCell>
                        <code className="break-all text-xs">{s.sha256}</code>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Last import</CardTitle>
              <CardDescription>
                Trigger {report.data.trigger}; started {local(report.data.startedAt)}, finished{' '}
                {local(report.data.finishedAt)}.
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Dataset</TableHead>
                    <TableHead>Rows</TableHead>
                    <TableHead>DSNY rows</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead>Existing</TableHead>
                    <TableHead>Updated</TableHead>
                    <TableHead>Skipped</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <ResultRow label="Baskets" r={report.data.baskets} />
                  <ResultRow label="Depots" r={report.data.depots} />
                </TableBody>
              </Table>
              <p className="text-sm">
                Synthetic demand entries created: {report.data.syntheticDemandsCreated} (category{' '}
                {report.data.syntheticDemandCategory}).
              </p>
              <div className="text-sm">
                <h2 className="font-medium">Warnings</h2>
                {report.data.warnings.length === 0 ? (
                  <p className="text-muted-foreground">No warnings</p>
                ) : (
                  <ul className="list-disc pl-5">
                    {report.data.warnings.map((w, i) => (
                      <li key={i}>{w}</li>
                    ))}
                  </ul>
                )}
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  )
}
