import { useCallback, useEffect, useState } from 'react'
import { api } from '../api.js'
import { formatPaise } from '../money.js'
import MemberForm from './MemberForm.jsx'
import MemberEditor from './MemberEditor.jsx'
import SpendingChart from './SpendingChart.jsx'
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
  const [editingMemberId, setEditingMemberId] = useState(null)

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

  async function afterMemberEdit(changed) {
    setEditingMemberId(null)
    if (changed) await afterChange()
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
          <ul className="chips" aria-label="Members (click a name to edit)">
            {group.members.map((m) => (
              <li key={m.id}>
                <button type="button" className={`chip ${editingMemberId === m.id ? 'is-active' : ''}`}
                  title={`Edit ${m.name}`} onClick={() => setEditingMemberId(editingMemberId === m.id ? null : m.id)}>
                  {m.name}
                </button>
              </li>
            ))}
          </ul>
          {editingMemberId ? (
            <MemberEditor key={editingMemberId} groupId={groupId}
              member={group.members.find((m) => m.id === editingMemberId)} onDone={afterMemberEdit} />
          ) : (
            <MemberForm groupId={groupId} onAdded={afterChange} />
          )}
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
            <div className="expenses-side">
              <SpendingChart expenses={expenses} />
              <ExpenseList groupId={groupId} expenses={expenses} onChanged={afterChange} />
            </div>
          </div>
        )}
        {tab === 'balances' && <Balances balances={balances} />}
        {tab === 'settle' && (
          <SettleUp groupId={groupId} plan={plan} settlements={settlements} onChanged={afterChange} />
        )}
      </div>
    </section>
  )
}
