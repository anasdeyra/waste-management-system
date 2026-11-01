import { Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'

import { originLabel } from '@/components/maps/geo'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useAddDemand, useRemoveDemand, useUpdateDemand } from '@/hooks/use-collection-points'
import { isValidKg } from '@/lib/validation'
import type { CollectionPoint, DemandEntry, WasteCategory } from '@/types/api'

function DemandRow({
  point,
  entry,
  categories,
  isOnly,
}: {
  point: CollectionPoint
  entry: DemandEntry
  categories: WasteCategory[]
  isOnly: boolean
}) {
  const update = useUpdateDemand()
  const remove = useRemoveDemand()
  const [categoryId, setCategoryId] = useState(entry.categoryId)
  const [kg, setKg] = useState(String(entry.quantityKg))
  const taken = new Set(point.demands.filter((d) => d.id !== entry.id).map((d) => d.categoryId))
  // The current category stays selectable even if inactive; new assignments must be active.
  const options = categories.filter((c) => c.id === entry.categoryId || (c.active && !taken.has(c.id)))
  const dirty = categoryId !== entry.categoryId || Number(kg) !== entry.quantityKg
  const valid = isValidKg(kg)

  return (
    <TableRow>
      <TableCell>
        <Select value={categoryId} onValueChange={setCategoryId}>
          <SelectTrigger className="w-44" aria-label="Waste category">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {options.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.name}
                {c.active ? '' : ' (inactive)'}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </TableCell>
      <TableCell>
        <div className="flex items-center gap-1">
          <Input
            aria-label="Quantity in kilograms"
            inputMode="decimal"
            className="w-24"
            value={kg}
            aria-invalid={!valid}
            onChange={(e) => setKg(e.target.value)}
          />
          <span className="text-sm text-muted-foreground">kg</span>
        </div>
      </TableCell>
      <TableCell>{originLabel(entry.origin)}</TableCell>
      <TableCell className="space-x-2 text-right">
        <Button
          size="sm"
          variant="outline"
          disabled={!dirty || !valid || update.isPending}
          onClick={() =>
            update.mutate(
              { pointId: point.id, demandId: entry.id, body: { categoryId, quantityKg: Number(kg) } },
              {
                onSuccess: () => toast.success('Demand entry saved'),
                onError: (e) => toast.error(e.message),
              },
            )
          }
        >
          Save
        </Button>
        <Button
          size="sm"
          variant="outline"
          aria-label={`Remove ${entry.categoryName}`}
          disabled={isOnly || remove.isPending}
          title={isOnly ? 'A point must keep at least one demand entry' : undefined}
          onClick={() =>
            remove.mutate(
              { pointId: point.id, demandId: entry.id },
              {
                onSuccess: () => toast.success('Demand entry removed'),
                onError: (e) => toast.error(e.message),
              },
            )
          }
        >
          <Trash2 />
        </Button>
      </TableCell>
    </TableRow>
  )
}

/** Inline editing of a point's demand entries (add, change quantity/category, remove). */
export function DemandEditor({
  point,
  categories,
}: {
  point: CollectionPoint
  categories: WasteCategory[]
}) {
  const add = useAddDemand()
  const used = new Set(point.demands.map((d) => d.categoryId))
  const available = categories.filter((c) => c.active && !used.has(c.id))
  const [newCategory, setNewCategory] = useState('')
  const [newKg, setNewKg] = useState('')
  const canAdd = newCategory !== '' && isValidKg(newKg)

  return (
    <div className="flex flex-col gap-3">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Category</TableHead>
            <TableHead>Quantity</TableHead>
            <TableHead>Origin</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {point.demands.map((d) => (
            <DemandRow
              key={`${d.id}:${d.categoryId}:${d.quantityKg}`}
              point={point}
              entry={d}
              categories={categories}
              isOnly={point.demands.length <= 1}
            />
          ))}
        </TableBody>
      </Table>
      {available.length > 0 && (
        <div className="flex flex-wrap items-end gap-2">
          <Select value={newCategory} onValueChange={setNewCategory}>
            <SelectTrigger className="w-44" aria-label="New demand category">
              <SelectValue placeholder="Category" />
            </SelectTrigger>
            <SelectContent>
              {available.map((c) => (
                <SelectItem key={c.id} value={c.id}>
                  {c.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <div className="flex items-center gap-1">
            <Input
              aria-label="New demand quantity in kilograms"
              inputMode="decimal"
              className="w-24"
              placeholder="0.00"
              value={newKg}
              onChange={(e) => setNewKg(e.target.value)}
            />
            <span className="text-sm text-muted-foreground">kg</span>
          </div>
          <Button
            size="sm"
            disabled={!canAdd || add.isPending}
            onClick={() =>
              add.mutate(
                { pointId: point.id, body: { categoryId: newCategory, quantityKg: Number(newKg) } },
                {
                  onSuccess: () => {
                    toast.success('Demand entry added')
                    setNewCategory('')
                    setNewKg('')
                  },
                  onError: (e) => toast.error(e.message),
                },
              )
            }
          >
            <Plus />
            Add demand
          </Button>
        </div>
      )}
    </div>
  )
}
