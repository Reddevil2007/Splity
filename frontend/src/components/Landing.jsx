import { useEffect, useRef } from 'react'

/**
 * Flowing "water current" lines drawn on a canvas: each line is a sum of slow
 * sine waves, so the field drifts like water. One static frame is drawn when the
 * visitor prefers reduced motion.
 */
function FlowField() {
  const canvasRef = useRef(null)

  useEffect(() => {
    const canvas = canvasRef.current
    const ctx = canvas.getContext('2d')
    if (!ctx) return undefined
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    let width = 0
    let height = 0
    let frame = 0

    function resize() {
      const ratio = Math.min(window.devicePixelRatio || 1, 2)
      width = canvas.clientWidth
      height = canvas.clientHeight
      canvas.width = width * ratio
      canvas.height = height * ratio
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
    }

    function draw(time) {
      const t = time / 1000
      ctx.clearRect(0, 0, width, height)
      const lines = 34
      for (let i = 0; i < lines; i++) {
        const p = i / (lines - 1)
        const base = height * (0.18 + p * 0.74)
        const amp = 18 + 34 * Math.sin(p * Math.PI)
        const gradient = ctx.createLinearGradient(0, 0, width, 0)
        gradient.addColorStop(0, `rgba(63, 184, 168, ${0.05 + 0.22 * (1 - p)})`)
        gradient.addColorStop(0.6, `rgba(120, 190, 230, ${0.08 + 0.14 * p})`)
        gradient.addColorStop(1, `rgba(242, 169, 0, ${0.06 + 0.3 * p})`)
        ctx.strokeStyle = gradient
        ctx.lineWidth = 1.1
        ctx.beginPath()
        for (let x = -20; x <= width + 20; x += 8) {
          const y = base
            + amp * Math.sin(x * 0.0042 + t * 0.55 + i * 0.32)
            + amp * 0.45 * Math.sin(x * 0.011 - t * 0.8 + i * 0.6)
            + 10 * Math.sin(x * 0.02 + t * 1.3 + i)
          if (x === -20) ctx.moveTo(x, y)
          else ctx.lineTo(x, y)
        }
        ctx.stroke()
      }
      if (!reduceMotion) frame = requestAnimationFrame(draw)
    }

    resize()
    window.addEventListener('resize', resize)
    frame = requestAnimationFrame(draw)
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('resize', resize)
    }
  }, [])

  return <canvas ref={canvasRef} className="flow-canvas" aria-hidden="true" />
}

const PREVIEW = [
  { name: 'Aarav', net: 7200 },
  { name: 'Diya', net: -2300 },
  { name: 'Kabir', net: -1700 },
  { name: 'Meera', net: -3200 },
]

export default function Landing({ onOpen, onTrySample }) {
  return (
    <div className="landing">
      <FlowField />
      <header className="landing-nav">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          <span className="brand-name">SplitCloud</span>
        </div>
        <button type="button" className="btn btn-ghost" onClick={onOpen}>Open the app</button>
      </header>

      <main className="landing-main">
        <section className="hero">
          <h1 className="hero-title">Split it.<br />Settle it.<br />Move on.</h1>
          <p className="hero-sub">
            SplitCloud turns a trip&apos;s worth of shared bills into the fewest possible payments,
            each one a tap away in any UPI app.
          </p>
          <div className="hero-actions">
            <button type="button" className="btn btn-pay btn-lg" onClick={onOpen}>Open SplitCloud</button>
            <button type="button" className="btn btn-ghost btn-lg" onClick={onTrySample}>Try a sample trip</button>
          </div>
        </section>

        <figure className="preview" aria-label="Example: Goa Trip balances">
          <div className="preview-bar">
            <span className="preview-dot" /><span className="preview-dot" /><span className="preview-dot" />
            <span className="preview-title">Goa Trip</span>
          </div>
          <ul className="preview-rows">
            {PREVIEW.map((p) => (
              <li key={p.name} className={p.net > 0 ? 'is-credit' : 'is-debit'}>
                <span className="preview-name">{p.name}</span>
                <span className="preview-track">
                  <span className="preview-centre" />
                  <span className="preview-fill" style={{ width: `${(Math.abs(p.net) / 7200) * 50}%` }} />
                </span>
                <span className="preview-amount">{p.net > 0 ? '+' : '−'}₹{Math.abs(p.net).toLocaleString('en-IN')}</span>
              </li>
            ))}
          </ul>
          <figcaption>4 people, ₹19,500 of bills, settled in 3 payments.</figcaption>
        </figure>
      </main>

      <section className="facts" aria-label="What SplitCloud does">
        <div>
          <strong>At most n − 1 payments</strong>
          <p>Debts are simplified so a group of n people never needs more than n − 1 transfers.</p>
        </div>
        <div>
          <strong>Exact to the paisa</strong>
          <p>Split equally, by exact amounts, percentages or shares. Every split adds up exactly.</p>
        </div>
        <div>
          <strong>Pay with any UPI app</strong>
          <p>Each payment comes with a UPI link and QR code. Money goes straight to your friend.</p>
        </div>
      </section>

      <footer className="landing-foot">
        Built on Azure App Service, Azure Database for PostgreSQL and Azure Storage. Deployed with Terraform and GitHub Actions.
      </footer>
    </div>
  )
}
