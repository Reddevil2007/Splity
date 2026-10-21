import { useEffect, useState } from 'react'
import QRCode from 'qrcode'
import { api } from '../api.js'
import { formatPaise } from '../money.js'

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

function Transfer({ t, groupId, onRecorded }) {
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
      await onRecorded()
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

const dateFormat = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })

export default function SettleUp({ groupId, plan, settlements, onRecorded }) {
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
              <Transfer key={`${t.fromMemberId}-${t.toMemberId}`} t={t} groupId={groupId} onRecorded={onRecorded} />
            ))}
          </ul>
        </>
      )}

      {settlements.length > 0 && (
        <div className="history">
          <h2 className="card-title">Payments recorded</h2>
          <ul>
            {settlements.map((s) => (
              <li key={s.id}>
                <span>{s.fromName} paid {s.toName}</span>
                <span className="figure">{formatPaise(s.amountPaise)}</span>
                <span className="muted">{dateFormat.format(new Date(s.createdAt))}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
