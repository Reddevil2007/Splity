import { useCallback, useEffect, useState } from 'react'
import { api } from './api.js'
import { formatPaise } from './money.js'
import GroupView from './components/GroupView.jsx'

export default function App() {
  const [groups, setGroups] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [newName, setNewName] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  const loadGroups = useCallback(async () => {
    try {
      const list = await api.listGroups()
      setGroups(list)
      setError(null)
      return list
    } catch (e) {
      setError(e.message)
      return []
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadGroups().then((list) => {
      if (list.length > 0) setSelectedId((current) => current ?? list[0].id)
    })
  }, [loadGroups])

  async function createGroup(event) {
    event.preventDefault()
    if (!newName.trim()) return
    try {
      const created = await api.createGroup(newName.trim())
      setNewName('')
      await loadGroups()
      setSelectedId(created.id)
    } catch (e) {
      setError(e.message)
    }
  }

  async function createDemo() {
    try {
      const created = await api.createDemoGroup()
      await loadGroups()
      setSelectedId(created.id)
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="shell">
      <header className="masthead">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          <span className="brand-name">SplitCloud</span>
        </div>
        <p className="tagline">Shared expenses, settled in the fewest payments.</p>
      </header>

      <div className="layout">
        <aside className="sidebar" aria-label="Your groups">
          <h2 className="sidebar-title">Groups</h2>

          {loading && <p className="muted">Loading groups…</p>}

          {!loading && groups.length === 0 && !error && (
            <div className="empty">
              <p>No groups yet. Create one below, or load the sample trip to see how it works.</p>
              <button type="button" className="btn btn-quiet" onClick={createDemo}>
                Load sample trip
              </button>
            </div>
          )}

          <ul className="group-list">
            {groups.map((g) => (
              <li key={g.id}>
                <button
                  type="button"
                  className={`group-item ${g.id === selectedId ? 'is-active' : ''}`}
                  onClick={() => setSelectedId(g.id)}
                  aria-current={g.id === selectedId ? 'true' : undefined}
                >
                  <span className="group-item-name">{g.name}</span>
                  <span className="group-item-meta">
                    {g.memberCount} {g.memberCount === 1 ? 'person' : 'people'}, {formatPaise(g.totalSpentPaise)}
                  </span>
                </button>
              </li>
            ))}
          </ul>

          <form className="new-group" onSubmit={createGroup}>
            <label htmlFor="new-group-name">New group</label>
            <div className="inline-field">
              <input
                id="new-group-name"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="Flat 4B, Goa trip…"
                maxLength={80}
              />
              <button type="submit" className="btn">Create</button>
            </div>
          </form>
          {groups.length > 0 && (
            <button type="button" className="link-btn" onClick={createDemo}>
              Add another sample trip
            </button>
          )}
        </aside>

        <main className="main">
          {error && (
            <div className="banner" role="alert">
              {error}
              <button type="button" className="link-btn" onClick={() => { setError(null); loadGroups() }}>
                Retry
              </button>
            </div>
          )}
          {selectedId ? (
            <GroupView key={selectedId} groupId={selectedId} onChanged={loadGroups} />
          ) : (
            !loading && <p className="muted center-note">Pick a group, or create one to start recording expenses.</p>
          )}
        </main>
      </div>
    </div>
  )
}
