import { useEffect, useState } from 'react'
import QRCode from 'qrcode'
import { api } from '../api.js'
import { formatPaise } from '../money.js'
import { dayColors, formatDay } from '../categories.js'

function UpiQr({ link }) {
  const [src, setSrc] = useState(null)
  useEffect(() => {
    let active = true
    QRCode.toDataURL(link, { margin: 1, width: 168, color: { dark: '#1B2A4A', light: '#FFFFFF' } })
      .then((url) => { if (active) setSrc(url) })
      .catch(() => { if (active) setSrc(null) })
    return () => { active = false }
  }, [link])
  return src ? <img className="qr" src={src} width="168" height="168" alt="UPI payment QR code" /> : null
}

function Transfer({ t, groupId, onChanged }) {
  const [showQr, setShowQr] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  async function markPaid() {
    setSaving(true)
    try {
      await api.recordSettlement(groupId, {
        fromMemberId: t.fromMemberId,
        toMemberId: t.toMemberId,
        amountPaise: t.amountPaise,
      })
      await onChanged()
    } catch (e) {
      setError(e.message)
      setSaving(false)
    }
  }

  return (
    <li className="transfer">
      <div className="transfer-line">
        <span><strong>{t.fromName}</strong> pays <strong>{t.toName}</strong></span>
        <span className="figure transfer-amount">{formatPaise(t.amountPaise)}</span>
      </div>
      <div className="transfer-actions">
        {t.upiLink ? (
          <>
            <a className="btn btn-pay" href={t.upiLink}>Pay with UPI</a>
            <button type="button" className="link-btn" onClick={() => setShowQr(!showQr)}>
              {showQr ? 'Hide QR code' : 'Show QR code'}
            </button>
          </>
        ) : (
          <span className="muted">{t.toName} has no UPI ID saved, so pay by cash or bank transfer.</span>
        )}
        <button type="button" className="btn btn-quiet" onClick={markPaid} disabled={saving}>
          {saving ? 'Recording…' : 'Mark as paid'}
        </button>
      </div>
      {showQr && t.upiLink && (
        <div className="qr-wrap">
          <UpiQr link={t.upiLink} />
          <p className="muted">Scan with Google Pay, PhonePe or Paytm. The money goes straight to {t.toName}.</p>
        </div>
      )}
      {error && <p className="field-error" role="alert">{error}</p>}
    </li>
  )
}

/** A recorded payment: change the date it was paid, or undo it if the money never arrived. */
function PaymentRow({ s, groupId, color, onChanged }) {
  const [editing, setEditing] = useState(false)
  const [paidOn, setPaidOn] = useState(s.paidOn)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  async function run(action) {
    setBusy(true)
    try {
      await action()
      await onChanged()
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  function undo() {
    if (!window.confirm(`Undo "${s.fromName} paid ${s.toName} ${formatPaise(s.amountPaise)}"? They will owe it again.`)) return
    run(() => api.deleteSettlement(groupId, s.id))
  }

  function saveDate(event) {
    event.preventDefault()
    run(() => api.updateSettlement(groupId, s.id, { paidOn }))
  }

  return (
    <li className="payment-row" style={{ '--day': color }}>
      <span className="day-pill">{formatDay(s.paidOn)}</span>
      <span>{s.fromName} paid {s.toName}</span>
      <span className="figure">{formatPaise(s.amountPaise)}</span>
      {editing ? (
        <form className="inline-date" onSubmit={saveDate}>
          <input type="date" aria-label="Date paid" value={paidOn} onChange={(e) => setPaidOn(e.target.value)} required />
          <button type="submit" className="btn" disabled={busy}>Save</button>
          <button type="button" className="link-btn" onClick={() => { setEditing(false); setPaidOn(s.paidOn) }}>Cancel</button>
        </form>
      ) : (
        <span className="row-actions">
          <button type="button" className="link-btn" onClick={() => setEditing(true)}>Change date</button>
          <button type="button" className="link-btn danger" onClick={undo} disabled={busy}>Undo</button>
        </span>
      )}
      {error && <p className="field-error" role="alert">{error}</p>}
    </li>
  )
}

export default function SettleUp({ groupId, plan, settlements, onChanged }) {
  const colors = dayColors(settlements.map((s) => s.paidOn))
  return (
    <div className="settle">
      {plan.length === 0 ? (
        <div className="settled-note">
          <p><strong>Everyone is settled up.</strong> New expenses will show the payments needed here.</p>
        </div>
      ) : (
        <>
          <p className="plan-intro">
            {plan.length} {plan.length === 1 ? 'payment clears' : 'payments clear'} every balance in this group.
          </p>
          <ul className="transfers">
            {plan.map((t) => (
              <Transfer key={`${t.fromMemberId}-${t.toMemberId}`} t={t} groupId={groupId} onChanged={onChanged} />
            ))}
          </ul>
        </>
      )}

      {settlements.length > 0 && (
        <div className="history">
          <h2 className="card-title">Payments recorded</h2>
          <p className="muted history-note">Marked a payment that didn't actually go through? Undo it and they'll owe the amount again.</p>
          <ul>
            {settlements.map((s) => (
              <PaymentRow key={s.id} s={s} groupId={groupId} color={colors[s.paidOn]} onChanged={onChanged} />
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
