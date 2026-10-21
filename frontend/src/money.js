// All money moves between the browser and the API as integer paise.
// These helpers are the only place rupees and paise are converted.

const rupeeFormat = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** 123456 -> "₹1,234.56" */
export function formatPaise(paise) {
  return rupeeFormat.format(paise / 100)
}

/** "1234.5" -> 123450; returns null for anything that is not a valid amount */
export function parseRupees(text) {
  const value = String(text ?? '').trim().replace(/,/g, '')
  if (!/^\d+(\.\d{1,2})?$/.test(value)) return null
  const [whole, fraction = ''] = value.split('.')
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
}
