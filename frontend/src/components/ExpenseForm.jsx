import { useState } from 'react'
import { api } from '../api.js'
import { parseRupees } from '../money.js'

const SPLIT_TYPES = [
  { id: 'EQUAL', label: 'Equally', hint: null },
  { id: 'EXACT', label: 'Exact amounts', hint: '₹' },
  { id: 'PERCENT', label: 'Percentages', hint: '%' },
  { id: 'SHARES', label: 'Shares', hint: 'shares' },
]

export default function ExpenseForm({ groupId, members, onAdded }) {
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [payerId, setPayerId] = useState(members[0]?.id ?? '')
  const [splitType, setSplitType] = useState('EQUAL')
  const [included, setIncluded] = useState(() => Object.fromEntries(members.map((m) => [m.id, true])))
  const [values, setValues] = useState({})
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  if (members.length === 0) {
    return (
      <div className="card empty">
        <p>Add the people in this group first. Every expense needs someone who paid and someone who shares it.</p>
      </div>
    )
  }

  const hint = SPLIT_TYPES.find((t) => t.id === splitType).hint

  function participantValue(memberId) {
    const raw = values[memberId] ?? ''
    if (splitType === 'EQUAL') return null
    if (splitType === 'EXACT') return parseRupees(raw)
    const n = Number(raw)
    return raw === '' || Number.isNaN(n) ? null : n
  }

  async function submit(event) {
    event.preventDefault()
    const amountPaise = parseRupees(amount)
    if (amountPaise === null || amountPaise <= 0) {
      setError('Enter the amount in rupees, for example 450 or 450.50')
      return
    }
    const chosen = members.filter((m) => included[m.id])
    if (chosen.length === 0) {
      setError('Tick at least one person who shares this expense')
      return
    }
    const participants = chosen.map((m) => ({ memberId: m.id, value: participantValue(m.id) }))
    if (splitType !== 'EQUAL' && participants.some((p) => p.value === null)) {
      setError(`Fill in a value for everyone ticked (${hint})`)
      return
    }

    setSaving(true)
    try {
      await api.addExpense(groupId, {
        description,
        amountPaise,
        payerId: Number(payerId),
        splitType,
        participants,
      })
      setDescription('')
      setAmount('')
      setValues({})
      setError(null)
      await onAdded()
    } catch (e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="card expense-form" onSubmit={submit}>
      <h2 className="card-title">Add an expense</h2>

      <label htmlFor="exp-desc">What was it for?</label>
      <input id="exp-desc" value={description} onChange={(e) => setDescription(e.target.value)}
        placeholder="Dinner, rent, cab…" required maxLength={120} />

      <div className="two-col">
        <div>
          <label htmlFor="exp-amount">Amount (₹)</label>
          <input id="exp-amount" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)}
            placeholder="0.00" required />
        </div>
        <div>
          <label htmlFor="exp-payer">Paid by</label>
          <select id="exp-payer" value={payerId} onChange={(e) => setPayerId(e.target.value)}>
            {members.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
          </select>
        </div>
      </div>

      <fieldset className="split-type">
        <legend>Split</legend>
        {SPLIT_TYPES.map((t) => (
          <label key={t.id} className={`seg ${splitType === t.id ? 'is-active' : ''}`}>
            <input type="radio" name="splitType" value={t.id} checked={splitType === t.id}
              onChange={() => setSplitType(t.id)} />
            {t.label}
          </label>
        ))}
      </fieldset>

      <ul className="participants">
        {members.map((m) => (
          <li key={m.id}>
            <label className="check">
              <input type="checkbox" checked={!!included[m.id]}
                onChange={(e) => setIncluded({ ...included, [m.id]: e.target.checked })} />
              {m.name}
            </label>
            {splitType !== 'EQUAL' && included[m.id] && (
              <span className="value-field">
                <input aria-label={`${m.name} ${hint}`} inputMode="decimal" value={values[m.id] ?? ''}
                  onChange={(e) => setValues({ ...values, [m.id]: e.target.value })} />
                <span className="unit">{hint}</span>
              </span>
            )}
          </li>
        ))}
      </ul>

      {error && <p className="field-error" role="alert">{error}</p>}
      <button type="submit" className="btn btn-wide" disabled={saving}>
        {saving ? 'Saving…' : 'Save expense'}
      </button>
    </form>
  )
}
