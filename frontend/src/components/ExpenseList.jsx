import { useState } from 'react'
import { api } from '../api.js'
import { formatPaise } from '../money.js'
import { CATEGORIES, category, dayColors, formatDay } from '../categories.js'

const SPLIT_LABEL = { EQUAL: 'split equally', EXACT: 'exact amounts', PERCENT: 'by percentage', SHARES: 'by shares' }

function ExpenseEditor({ groupId, expense, onDone }) {
  const [description, setDescription] = useState(expense.description)
  const [spentOn, setSpentOn] = useState(expense.spentOn)
  const [cat, setCat] = useState(expense.category)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  async function save(event) {
    event.preventDefault()
    setSaving(true)
    try {
      await api.updateExpense(groupId, expense.id, { description, spentOn, category: cat })
      await onDone(true)
    } catch (e) {
      setError(e.message)
      setSaving(false)
    }
  }

  return (
    <form className="expense-editor" onSubmit={save}>
      <label>
        Description
        <input value={description} onChange={(e) => setDescription(e.target.value)} required maxLength={120} />
      </label>
      <div className="two-col">
        <label>
          Date
          <input type="date" value={spentOn} onChange={(e) => setSpentOn(e.target.value)} required />
        </label>
        <label>
          Category
          <select value={cat} onChange={(e) => setCat(e.target.value)}>
            {CATEGORIES.map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
          </select>
        </label>
      </div>
      <div className="form-actions">
        <button type="submit" className="btn" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button>
        <button type="button" className="link-btn" onClick={() => onDone(false)}>Cancel</button>
      </div>
      {error && <p className="field-error" role="alert">{error}</p>}
    </form>
  )
}

/** Expenses grouped by day, newest first. Each day has its own colour on the margin rule. */
export default function ExpenseList({ groupId, expenses, onChanged }) {
  const [editingId, setEditingId] = useState(null)

  async function remove(expense) {
    if (!window.confirm(`Delete "${expense.description}"? Balances will be recalculated.`)) return
    await api.deleteExpense(groupId, expense.id)
    await onChanged()
  }

  async function finishEdit(changed) {
    setEditingId(null)
    if (changed) await onChanged()
  }

  if (expenses.length === 0) {
    return (
      <div className="ledger empty">
        <p>No expenses yet. Add the first one and the balances fill in automatically.</p>
      </div>
    )
  }

  const colors = dayColors(expenses.map((e) => e.spentOn))
  const days = Object.keys(colors)

  return (
    <div className="ledger" aria-label="Expenses by day, newest first">
      {days.map((day) => {
        const dayExpenses = expenses.filter((e) => e.spentOn === day)
        const dayTotal = dayExpenses.reduce((sum, e) => sum + e.amountPaise, 0)
        return (
          <section key={day} className="ledger-day" style={{ '--day': colors[day] }}>
            <header className="ledger-day-head">
              <span className="day-pill">{formatDay(day)}</span>
              <span className="muted figure">{formatPaise(dayTotal)}</span>
            </header>
            <ol>
              {dayExpenses.map((e) => {
                const cat = category(e.category)
                return (
                  <li key={e.id} className="ledger-row">
                    {editingId === e.id ? (
                      <ExpenseEditor groupId={groupId} expense={e} onDone={finishEdit} />
                    ) : (
                      <>
                        <div className="ledger-body">
                          <div className="ledger-line">
                            <span className="ledger-desc">{e.description}</span>
                            <span className="figure">{formatPaise(e.amountPaise)}</span>
                          </div>
                          <div className="ledger-sub">
                            <span className="cat-tag" style={{ '--cat': cat.color }}>{cat.label}</span>
                            {e.payerName} paid, {SPLIT_LABEL[e.splitType]}:{' '}
                            {e.splits.map((s) => `${s.memberName} ${formatPaise(s.sharePaise)}`).join(', ')}
                          </div>
                        </div>
                        <div className="row-actions">
                          <button type="button" className="icon-btn" onClick={() => setEditingId(e.id)}
                            aria-label={`Edit ${e.description}`} title="Edit date, category or name">✎</button>
                          <button type="button" className="icon-btn icon-danger" onClick={() => remove(e)}
                            aria-label={`Delete ${e.description}`} title="Delete">×</button>
                        </div>
                      </>
                    )}
                  </li>
                )
              })}
            </ol>
          </section>
        )
      })}
    </div>
  )
}
