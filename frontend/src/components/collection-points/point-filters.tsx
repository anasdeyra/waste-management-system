import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import type { PointFilters } from '@/hooks/use-collection-points'
import type { CollectionPointStatus, WasteCategory } from '@/types/api'

const ALL = 'ALL'

/** Status and category filters of the collection point list (default: active points). */
export function PointFiltersBar({
  filters,
  categories,
  onChange,
}: {
  filters: PointFilters
  categories: WasteCategory[]
  onChange: (f: PointFilters) => void
}) {
  return (
    <div className="flex flex-wrap gap-3">
      <div className="flex flex-col gap-1">
        <Label htmlFor="filter-status">Status</Label>
        <Select
          value={filters.status ?? ALL}
          onValueChange={(v) =>
            onChange({ ...filters, status: v === ALL ? undefined : (v as CollectionPointStatus) })
          }
        >
          <SelectTrigger id="filter-status" className="w-36">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All</SelectItem>
            <SelectItem value="ACTIVE">Active</SelectItem>
            <SelectItem value="INACTIVE">Inactive</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="filter-category">Waste category</Label>
        <Select
          value={filters.categoryId ?? ALL}
          onValueChange={(v) => onChange({ ...filters, categoryId: v === ALL ? undefined : v })}
        >
          <SelectTrigger id="filter-category" className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All categories</SelectItem>
            {categories.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.name}
                {c.active ? '' : ' (inactive)'}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  )
}
