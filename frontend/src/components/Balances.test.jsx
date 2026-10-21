import { describe, expect, it, afterEach } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import Balances from './Balances.jsx'

afterEach(cleanup)

describe('Balances', () => {
  it('describes who owes and who is owed', () => {
    render(
      <Balances
        balances={[
          { memberId: 1, memberName: 'Asha', paidPaise: 30000, sharePaise: 10000, netPaise: 20000 },
          { memberId: 2, memberName: 'Bilal', paidPaise: 0, sharePaise: 10000, netPaise: -10000 },
          { memberId: 3, memberName: 'Chitra', paidPaise: 0, sharePaise: 0, netPaise: 0 },
        ]}
      />,
    )
    expect(screen.getByText('is owed ₹200.00')).toBeTruthy()
    expect(screen.getByText('owes ₹100.00')).toBeTruthy()
    expect(screen.getByText('is settled up')).toBeTruthy()
  })

  it('shows guidance when there is nothing to show', () => {
    render(<Balances balances={[]} />)
    expect(screen.getByText(/Add people and expenses/)).toBeTruthy()
  })
})
