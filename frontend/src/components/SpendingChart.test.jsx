import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import SpendingChart from './SpendingChart.jsx'

afterEach(cleanup)

const expense = (category, amountPaise) => ({ category, amountPaise })

describe('SpendingChart', () => {
  it('totals spending per category with percentages', () => {
    render(<SpendingChart expenses={[expense('STAY', 1200000), expense('TRAVEL', 240000), expense('TRAVEL', 150000),
      expense('FOOD', 360000)]} />)
    expect(screen.getByText('Stay')).toBeTruthy()
    expect(screen.getByText('₹3,900.00')).toBeTruthy() // travel: 2,400 + 1,500
    expect(screen.getByText('62%')).toBeTruthy() // stay: 12,000 of 19,500
  })

  it('explains itself when there is nothing to chart', () => {
    render(<SpendingChart expenses={[]} />)
    expect(screen.getByText(/appears here once you add an expense/)).toBeTruthy()
  })
})
