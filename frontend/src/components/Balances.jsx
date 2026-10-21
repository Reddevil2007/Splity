import { formatPaise } from '../money.js'

/**
 * Each person's net position drawn from a centre rule:
 * bars to the right are owed money, bars to the left owe money.
 */
export default function Balances({ balances }) {
  if (balances.length === 0) {
    return <p className="muted">Add people and expenses to see who owes whom.</p>
  }
  const max = Math.max(1, ...balances.map((b) => Math.abs(b.netPaise)))

  return (
    <div className="balances">
      <div className="balance-axis" aria-hidden="true">
        <span>owes</span>
        <span>is owed</span>
      </div>
      <ul className="balance-list">
        {balances.map((b) => {
          const width = `${(Math.abs(b.netPaise) / max) * 50}%`
          const state = b.netPaise > 0 ? 'credit' : b.netPaise < 0 ? 'debit' : 'even'
          const summary =
            state === 'credit' ? `is owed ${formatPaise(b.netPaise)}`
              : state === 'debit' ? `owes ${formatPaise(-b.netPaise)}`
                : 'is settled up'
          return (
            <li key={b.memberId} className={`balance-row is-${state}`}>
              <div className="balance-name">
                <strong>{b.memberName}</strong>
                <span className="balance-summary">{summary}</span>
              </div>
              <div className="balance-track" role="img" aria-label={`${b.memberName} ${summary}`}>
                <span className="balance-centre" />
                {state !== 'even' && <span className="balance-bar" style={{ width }} />}
              </div>
              <div className="balance-detail">
                paid {formatPaise(b.paidPaise)}, share {formatPaise(b.sharePaise)}
              </div>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
