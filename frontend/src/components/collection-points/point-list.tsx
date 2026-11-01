import { useEffect, useRef } from 'react'

import { originLabel } from '@/components/maps/geo'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import type { CollectionPoint } from '@/types/api'

/** Selectable list of collection points; selection is shared with the map. */
export function PointList({
  points,
  selectedId,
  onSelect,
}: {
  points: CollectionPoint[]
  selectedId: string | null
  onSelect: (p: CollectionPoint) => void
}) {
  const selectedRef = useRef<HTMLLIElement | null>(null)

  useEffect(() => {
    selectedRef.current?.scrollIntoView({ block: 'nearest' })
  }, [selectedId])

  return (
    <ul role="listbox" aria-label="Collection points" className="max-h-[480px] overflow-y-auto rounded-md border bg-background">
      {points.map((p) => {
        const selected = p.id === selectedId
        return (
          <li
            key={p.id}
            ref={selected ? selectedRef : undefined}
            role="option"
            aria-selected={selected}
            tabIndex={0}
            onClick={() => onSelect(p)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault()
                onSelect(p)
              }
            }}
            className={cn(
              'flex cursor-pointer flex-col gap-1 border-b p-3 text-sm last:border-b-0 hover:bg-muted focus-visible:outline-2 focus-visible:outline-ring',
              selected && 'bg-primary/10',
            )}
          >
            <div className="flex items-center justify-between gap-2">
              <span className="font-medium">{p.name}</span>
              <Badge variant={p.status === 'ACTIVE' ? 'default' : 'secondary'}>
                {p.status === 'ACTIVE' ? 'Active' : 'Inactive'}
              </Badge>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <span>{originLabel(p.origin)}</span>
              <span>{p.totalDemandKg} kg</span>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
