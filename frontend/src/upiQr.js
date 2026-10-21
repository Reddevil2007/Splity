import jsQR from 'jsqr'

/**
 * Reads a UPI payment QR in the browser. Only the UPI ID (and the name printed
 * in the QR) is kept; the image itself never leaves the device.
 */

/** 'upi://pay?pa=name@bank&pn=Name...' -> { upiId, name }, or null if it isn't a UPI payment link */
export function parseUpi(text) {
  if (typeof text !== 'string' || !text.toLowerCase().startsWith('upi://')) return null
  const query = text.slice(text.indexOf('?') + 1)
  const params = new URLSearchParams(query)
  const upiId = (params.get('pa') ?? '').trim()
  if (!upiId.includes('@')) return null
  return { upiId, name: (params.get('pn') ?? '').trim() }
}

function loadImage(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => { URL.revokeObjectURL(url); resolve(img) }
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('That file could not be opened as an image.')) }
    img.src = url
  })
}

/** Decodes the QR code in an image file and returns its UPI details. Throws with a readable message. */
export async function readUpiFromImage(file) {
  const img = await loadImage(file)
  const scale = Math.min(1, 1200 / Math.max(img.width, img.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(img.width * scale)
  canvas.height = Math.round(img.height * scale)
  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
  const pixels = ctx.getImageData(0, 0, canvas.width, canvas.height)

  const code = jsQR(pixels.data, pixels.width, pixels.height, { inversionAttempts: 'attemptBoth' })
  if (!code) throw new Error('No QR code found in that image. Try a clearer screenshot of the QR.')
  const upi = parseUpi(code.data)
  if (!upi) throw new Error("That QR isn't a UPI payment QR.")
  return upi
}
