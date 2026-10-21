import { describe, expect, it } from 'vitest'
import { formatPaise, parseRupees } from './money.js'

describe('parseRupees', () => {
  it('converts rupee text to integer paise', () => {
    expect(parseRupees('450')).toBe(45000)
    expect(parseRupees('450.5')).toBe(45050)
    expect(parseRupees('0.07')).toBe(7)
    expect(parseRupees('1,200.00')).toBe(120000)
  })

  it('rejects anything that is not an amount', () => {
    expect(parseRupees('')).toBeNull()
    expect(parseRupees('abc')).toBeNull()
    expect(parseRupees('-5')).toBeNull()
    expect(parseRupees('1.234')).toBeNull()
  })
})

describe('formatPaise', () => {
  it('formats with the Indian grouping and two decimals', () => {
    expect(formatPaise(12345678)).toBe('₹1,23,456.78')
    expect(formatPaise(0)).toBe('₹0.00')
  })
})
