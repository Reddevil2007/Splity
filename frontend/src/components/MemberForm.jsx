import { useState } from 'react'
import { api } from '../api.js'
import QrUpload from './QrUpload.jsx'

const EMPTY = { name: '', email: '', upiId: '' }

export default function MemberForm({ groupId, onAdded }) {
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  function update(field) {
    return (e) => setForm({ ...form, [field]: e.target.value })
  }

  async function submit(event) {
    event.preventDefault()
    setSaving(true)
    try {
      await api.addMember(groupId, form)
      setForm(EMPTY)
      setError(null)
      setOpen(false)
      await onAdded()
    } catch (e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  if (!open) {
    return (
      <button type="button" className="link-btn" onClick={() => setOpen(true)}>
        Add a person
      </button>
    )
  }

  return (
    <form className="member-form" onSubmit={submit}>
      <input aria-label="Name" placeholder="Name" value={form.name} onChange={update('name')} required maxLength={60} />
      <input aria-label="Email" type="email" placeholder="Email (for reminders)" value={form.email} onChange={update('email')} />
      <input aria-label="UPI ID" placeholder="UPI ID, e.g. name@okaxis" value={form.upiId} onChange={update('upiId')} />
      <QrUpload onRead={({ upiId, name }) => setForm((f) => ({ ...f, upiId, name: f.name || name }))} />
      <div className="form-actions">
        <button type="submit" className="btn" disabled={saving}>{saving ? 'Adding…' : 'Add person'}</button>
        <button type="button" className="link-btn" onClick={() => setOpen(false)}>Cancel</button>
      </div>
      {error && <p className="field-error" role="alert">{error}</p>}
    </form>
  )
}
