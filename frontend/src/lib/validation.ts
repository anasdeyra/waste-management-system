/** True when the text is a positive number with at most two decimals (mirrors the backend). */
export function isValidKg(text: string): boolean {
  const n = Number(text)
  return text.trim() !== '' && Number.isFinite(n) && n > 0 && /^\d{1,8}(\.\d{1,2})?$/.test(text.trim())
}

