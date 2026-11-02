import type { LegendItem } from './legend-items'

/** Compact legend card drawn over the bottom-left corner of a BaseMap. */
export function MapLegend({ items }: { items: LegendItem[] }) {
  if (items.length === 0) return null
  return (
    <ul className="wm-map-card pointer-events-auto absolute bottom-6 left-2 z-[1000] flex max-w-[calc(100%-9rem)] flex-wrap gap-x-3 gap-y-1 px-2.5 py-1.5 text-[11px] text-foreground/80">
      {items.map((item) => (
        <li key={item.label} className="flex items-center gap-1.5">
          {item.swatch}
          {item.label}
        </li>
      ))}
    </ul>
  )
}
