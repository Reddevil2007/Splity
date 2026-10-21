import { describe, expect, it } from 'vitest'
import { dayColors, formatDay, guessCategory } from './categories.js'

describe('guessCategory', () => {
  it('matches the backend guesses', () => {
    expect(guessCategory('Cab to airport')).toBe('TRAVEL')
    expect(guessCategory('Hotel booking')).toBe('STAY')
    expect(guessCategory('Beach shack dinner')).toBe('FOOD')
    expect(guessCategory('Tequila')).toBe('DRINKS')
    expect(guessCategory('Parasailing')).toBe('ACTIVITIES')
    expect(guessCategory('Something else')).toBe('OTHER')
  })
})

describe('dayColors', () => {
  it('gives each distinct day one colour, newest day first', () => {
    const colors = dayColors(['2026-10-21', '2026-10-22', '2026-10-21'])
    expect(Object.keys(colors)).toEqual(['2026-10-22', '2026-10-21'])
    expect(colors['2026-10-22']).not.toBe(colors['2026-10-21'])
  })
})

describe('formatDay', () => {
  it('formats a calendar date without shifting it across time zones', () => {
    expect(formatDay('2026-10-22')).toContain('22 Oct')
  })
})
