/** Metres as kilometres with two decimals, e.g. 7470.433 -> "7.47 km". */
export function formatDistanceKm(m: number | undefined): string {
  return m === undefined ? '-' : `${(m / 1000).toFixed(2)} km`
}

/** Seconds as "1 h 36 min" (rounded to the minute); under an hour "45 min". */
export function formatDuration(s: number | undefined): string {
  if (s === undefined) return '-'
  const totalMinutes = Math.round(s / 60)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return hours > 0 ? `${hours} h ${minutes} min` : `${minutes} min`
}

/** Milliseconds with three decimals, e.g. "0.065 ms". */
export function formatExecutionMs(ms: number): string {
  return `${ms.toFixed(3)} ms`
}

/** Relative change in percent, or null when either value is missing or the baseline is 0. */
export function percentChange(
  value: number | undefined,
  baseline: number | undefined,
): number | null {
  if (value === undefined || baseline === undefined || baseline === 0) return null
  return ((value - baseline) / baseline) * 100
}

/** "-36.3%" / "+2.1%" / "0.0%" with one decimal (ASCII minus); null -> '-'. */
export function formatPercent(p: number | null): string {
  if (p === null) return '-'
  const text = Math.abs(p).toFixed(1)
  if (text === '0.0') return '0.0%'
  return `${p < 0 ? '-' : '+'}${text}%`
}

/** ISO instant as a local date-time string (toLocaleString). */
export function formatInstant(iso: string): string {
  return new Date(iso).toLocaleString()
}
