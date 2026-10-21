import { api } from '../api.js'
import { formatPaise } from '../money.js'

const SPLIT_LABEL = { EQUAL: 'split equally', EXACT: 'exact amounts', PERCENT: 'by percentage', SHARES: 'by shares' }

const dateFormat = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short' })

export default function ExpenseList({ groupId, expenses, onDeleted }) {
  async function remove(expense) {
    if (!window.confirm(`Delete "${expense.description}"? Balances will be recalculated.`)) return
    await api.deleteExpense(groupId, expense.id)
    await onDeleted()
  }

  if (expenses.length === 0) {
    return (
      <div className="ledger empty">
        <p>No expenses yet. Add the first one and the balances fill in automatically.</p>
      </div>
    )
  }

  return (
    <ol className="ledger" aria-label="Expenses, newest first">
      {expenses.map((e) => (
        <li key={e.id} className="ledger-row">
          <div className="ledger-date">{dateFormat.format(new Date(e.createdAt))}</div>
          <div className="ledger-body">
            <div className="ledger-line">
              <span className="ledger-desc">{e.description}</span>
              <span className="figure">{formatPaise(e.amountPaise)}</span>
            </div>
            <div className="ledger-sub">
              {e.payerName} paid, {SPLIT_LABEL[e.splitType]}:{' '}
              {e.splits.map((s) => `${s.memberName} ${formatPaise(s.sharePaise)}`).join(', ')}
            </div>
          </div>
          <button type="button" className="icon-btn" onClick={() => remove(e)} aria-label={`Delete ${e.description}`}>
            ×
          </button>
        </li>
      ))}
    </ol>
  )
}
