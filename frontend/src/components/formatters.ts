export function formatCurrency(value: number | undefined | null): string {
  const n = value ?? 0
  return '\u20B9' + n.toLocaleString('en-IN', { maximumFractionDigits: 2 })
}

export function formatDate(iso: string | undefined): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}
