/** Returns the value when it is one of the allowed strings, else undefined. */
export function enumParam<T extends string>(value: string | null, allowed: readonly T[]): T | undefined {
  return value !== null && (allowed as readonly string[]).includes(value) ? (value as T) : undefined
}

/** Accepts YYYY-MM-DD only. */
export function dateParam(value: string | null): string | undefined {
  return value !== null && /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : undefined
}
