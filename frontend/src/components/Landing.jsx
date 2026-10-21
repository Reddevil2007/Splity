import { useEffect, useRef } from 'react'

/**
 * Flowing "water current" lines drawn on a canvas: each line is a sum of slow
 * sine waves, so the field drifts like water. The pointer acts like a finger in
 * the water: lines bulge away from it, ripple outward, and glow near it.
 * One static frame is drawn when the visitor prefers reduced motion.
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

    // Pointer target (raw) and an eased copy, so the effect glides rather than jumps.
    const pointer = { x: -9999, y: -9999, strength: 0 }
    const eased = { x: -9999, y: -9999, strength: 0 }

    function resize() {
      const ratio = Math.min(window.devicePixelRatio || 1, 2)
      width = canvas.clientWidth
      height = canvas.clientHeight
      canvas.width = width * ratio
      canvas.height = height * ratio
      ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
    }

    function onMove(event) {
      const rect = canvas.getBoundingClientRect()
      pointer.x = event.clientX - rect.left
      pointer.y = event.clientY - rect.top
      pointer.strength = 1
      if (eased.strength === 0) {
        eased.x = pointer.x
        eased.y = pointer.y
      }
    }

    function onLeave() {
      pointer.strength = 0
    }

    const RADIUS = 200

    function draw(time) {
      const t = time / 1000
      eased.x += (pointer.x - eased.x) * 0.12
      eased.y += (pointer.y - eased.y) * 0.12
      eased.strength += (pointer.strength - eased.strength) * 0.06
      const active = eased.strength > 0.01

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

        ctx.beginPath()
        for (let x = -20; x <= width + 20; x += 6) {
          let y = base
            + amp * Math.sin(x * 0.0042 + t * 0.55 + i * 0.32)
            + amp * 0.45 * Math.sin(x * 0.011 - t * 0.8 + i * 0.6)
            + 10 * Math.sin(x * 0.02 + t * 1.3 + i)
          if (active) {
            const dx = x - eased.x
            const dy = y - eased.y
            const dist = Math.hypot(dx, dy)
            if (dist < RADIUS * 2) {
              const falloff = Math.exp(-(dist * dist) / (2 * RADIUS * RADIUS * 0.35)) * eased.strength
              // Spread the lines away from the pointer like a lens (continuous, so no
              // line ever jumps across it), plus a ripple running outward from it.
              y += dy * 0.85 * falloff
              y += 7 * Math.sin(dist * 0.06 - t * 6) * falloff
            }
          }
          if (x === -20) ctx.moveTo(x, y)
          else ctx.lineTo(x, y)
        }
        ctx.strokeStyle = gradient
        ctx.lineWidth = 1.1
        ctx.stroke()

        if (active) {
          // Second pass over the same path: a glow that only shows near the pointer.
          const glow = ctx.createRadialGradient(eased.x, eased.y, 0, eased.x, eased.y, RADIUS)
          glow.addColorStop(0, `rgba(255, 214, 102, ${0.85 * eased.strength})`)
          glow.addColorStop(0.45, `rgba(120, 220, 205, ${0.45 * eased.strength})`)
          glow.addColorStop(1, 'rgba(120, 220, 205, 0)')
          ctx.strokeStyle = glow
          ctx.lineWidth = 2.2
          ctx.stroke()
        }
      }
      if (!reduceMotion) frame = requestAnimationFrame(draw)
    }

    resize()
    window.addEventListener('resize', resize)
    if (!reduceMotion) {
      window.addEventListener('pointermove', onMove)
      document.addEventListener('pointerleave', onLeave)
      window.addEventListener('blur', onLeave)
    }
    frame = requestAnimationFrame(draw)
    return () => {
      cancelAnimationFrame(frame)
      window.removeEventListener('resize', resize)
      window.removeEventListener('pointermove', onMove)
      document.removeEventListener('pointerleave', onLeave)
      window.removeEventListener('blur', onLeave)
    }
  }, [])

  return <canvas ref={canvasRef} className="flow-canvas" aria-hidden="true" />
}

/**
 * Renders text as individual letters so each one can light up under the cursor.
 * Screen readers get the plain text through aria-label.
 */
function HoverLetters({ lines }) {
  return lines.map((line, li) => (
    <span key={li} className="hover-line">
      {line.split(' ').map((word, wi) => (
        <span key={wi} className="hover-word">
          {[...word].map((ch, ci) => (
            <span key={ci} className="hover-ch" aria-hidden="true">{ch}</span>
          ))}
        </span>
      ))}
    </span>
  ))
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
          <h1 className="hero-title" aria-label="Split it. Settle it. Move on.">
            <HoverLetters lines={['Split it.', 'Settle it.', 'Move on.']} />
          </h1>
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
