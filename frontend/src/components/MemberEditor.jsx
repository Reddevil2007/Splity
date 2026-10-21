import { useState } from 'react'
import { api } from '../api.js'
import QrUpload from './QrUpload.jsx'

/** Edit a person's name and contact details, or remove someone added by mistake. */
export default function MemberEditor({ groupId, member, onDone }) {
  const [form, setForm] = useState({
    name: member.name,
    email: member.email ?? '',
    upiId: member.upiId ?? '',
  })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  function update(field) {
    return (e) => setForm({ ...form, [field]: e.target.value })
  }

  async function save(event) {
    event.preventDefault()
    setBusy(true)
    try {
      await api.updateMember(groupId, member.id, form)
      await onDone(true)
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  async function remove() {
    if (!window.confirm(`Remove ${member.name} from this group?`)) return
    setBusy(true)
    try {
      await api.deleteMember(groupId, member.id)
      await onDone(true)
    } catch (e) {
      // 409: the person is part of expenses or payments, and the message says what to do
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <form className="member-form member-editor" onSubmit={save}>
      <p className="editor-title">Edit {member.name}</p>
      <input aria-label="Name" placeholder="Name" value={form.name} onChange={update('name')} required maxLength={60} />
      <input aria-label="Email" type="email" placeholder="Email" value={form.email} onChange={update('email')} />
      <input aria-label="UPI ID" placeholder="UPI ID, e.g. name@okaxis" value={form.upiId} onChange={update('upiId')} />
      <QrUpload onRead={({ upiId, name }) => setForm((f) => ({ ...f, upiId, name: f.name || name }))} />
      <div className="form-actions">
        <button type="submit" className="btn" disabled={busy}>Save</button>
        <button type="button" className="link-btn" onClick={() => onDone(false)}>Cancel</button>
        <button type="button" className="link-btn danger" onClick={remove} disabled={busy}>Remove person</button>
      </div>
      {error && <p className="field-error" role="alert">{error}</p>}
    </form>
  )
}
