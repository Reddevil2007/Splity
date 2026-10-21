import { describe, expect, it } from 'vitest'
import { parseUpi } from './upiQr.js'

describe('parseUpi', () => {
  it('reads the payee UPI ID and name from a UPI payment link', () => {
    expect(parseUpi('upi://pay?pa=asha@okaxis&pn=Asha%20Rao&cu=INR')).toEqual({ upiId: 'asha@okaxis', name: 'Asha Rao' })
  })

  it('rejects QR codes that are not UPI payments', () => {
    expect(parseUpi('https://example.com')).toBeNull()
    expect(parseUpi('upi://pay?pn=NoId')).toBeNull()
  })
})
