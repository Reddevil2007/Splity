import { useRef, useState } from 'react'
import { readUpiFromImage } from '../upiQr.js'

/** "Upload payment QR": reads the UPI ID out of a QR image chosen from the device. */
export default function QrUpload({ onRead }) {
  const inputRef = useRef(null)
  const [status, setStatus] = useState(null)
  const [error, setError] = useState(null)

  async function handleFile(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setError(null)
    setStatus('Reading QR…')
    try {
      const upi = await readUpiFromImage(file)
      onRead(upi)
      setStatus(`UPI ID read from QR: ${upi.upiId}`)
    } catch (e) {
      setStatus(null)
      setError(e.message)
    }
  }

  return (
    <div className="qr-upload">
      <input ref={inputRef} type="file" accept="image/*" hidden onChange={handleFile} />
      <button type="button" className="link-btn" onClick={() => inputRef.current?.click()}>
        Upload payment QR instead
      </button>
      {status && <span className="qr-status">{status}</span>}
      {error && <p className="field-error" role="alert">{error}</p>}
    </div>
  )
}
