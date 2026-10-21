import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import { formatPaise } from '../money.js'
import MemberForm from './MemberForm.jsx'
import ExpenseForm from './ExpenseForm.jsx'
import ExpenseList from './ExpenseList.jsx'
import Balances from './Balances.jsx'
import SettleUp from './SettleUp.jsx'

const TABS = [
  { id: 'expenses', label: 'Expenses' },
  { id: 'balances', label: 'Balances' },
  { id: 'settle', label: 'Settle up' },
]

export default function GroupView({ groupId, onChanged }) {
  const [group, setGroup] = useState(null)
  const [expenses, setExpenses] = useState([])
  const [balances, setBalances] = useState([])
  const [plan, setPlan] = useState([])
  const [settlements, setSettlements] = useState([])
  const [tab, setTab] = useState('expenses')
  const [error, setError] = useState(null)

  const reload = useCallback(async () => {
    try {
      const [g, e, b, p, s] = await Promise.all([
        api.getGroup(groupId),
        api.listExpenses(groupId),
        api.balances(groupId),
        api.settlementPlan(groupId),
        api.listSettlements(groupId),
      ])
      setGroup(g)
      setExpenses(e)
      setBalances(b)
      setPlan(p)
      setSettlements(s)
      setError(null)
    } catch (err) {
      setError(err.message)
    }
  }, [groupId])

  useEffect(() => {
    reload()
  }, [reload])

  async function afterChange() {
    await reload()
    onChanged()
  }

  if (error && !group) return <div className="banner" role="alert">{error}</div>
  if (!group) return <p className="muted">Loading group…</p>

  return (
    <section className="group" aria-labelledby="group-title">
      <div className="group-head">
        <div>
          <h1 id="group-title" className="group-title">{group.name}</h1>
          <p className="group-total">
            <span className="figure">{formatPaise(group.totalSpentPaise)}</span> spent across{' '}
            {expenses.length} {expenses.length === 1 ? 'expense' : 'expenses'}
          </p>
        </div>
        <div className="people">
          <ul className="chips" aria-label="Members">
            {group.members.map((m) => (
              <li key={m.id} className="chip" title={m.upiId ?? 'No UPI ID'}>{m.name}</li>
            ))}
          </ul>
          <MemberForm groupId={groupId} onAdded={afterChange} />
        </div>
      </div>

      {error && <div className="banner" role="alert">{error}</div>}

      <div className="tabs" role="tablist" aria-label="Group sections">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            id={`tab-${t.id}`}
            aria-selected={tab === t.id}
            aria-controls={`panel-${t.id}`}
            className={`tab ${tab === t.id ? 'is-active' : ''}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
            {t.id === 'settle' && plan.length > 0 && <span className="tab-count">{plan.length}</span>}
          </button>
        ))}
      </div>

      <div role="tabpanel" id={`panel-${tab}`} aria-labelledby={`tab-${tab}`} className="panel">
        {tab === 'expenses' && (
          <div className="expenses-layout">
            {/* key resets the form when people are added, so new members start ticked */}
            <ExpenseForm key={group.members.map((m) => m.id).join(',')} groupId={groupId}
              members={group.members} onAdded={afterChange} />
            <ExpenseList groupId={groupId} expenses={expenses} onDeleted={afterChange} />
          </div>
        )}
        {tab === 'balances' && <Balances balances={balances} />}
        {tab === 'settle' && (
          <SettleUp groupId={groupId} plan={plan} settlements={settlements} onRecorded={afterChange} />
        )}
      </div>
    </section>
  )
}
