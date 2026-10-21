import { useState } from 'react'
import { CATEGORIES } from '../categories.js'
import { formatPaise } from '../money.js'

const SIZE = 220
const RADIUS = 86
const STROKE = 34
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

/** Donut chart of spending by category, drawn as SVG circle segments. */
export default function SpendingChart({ expenses }) {
  const [active, setActive] = useState(null)

  const totals = CATEGORIES.map((c) => ({
    ...c,
    total: expenses.filter((e) => e.category === c.id).reduce((sum, e) => sum + e.amountPaise, 0),
  })).filter((c) => c.total > 0)
  const grandTotal = totals.reduce((sum, c) => sum + c.total, 0)

  if (grandTotal === 0) {
    return (
      <div className="card chart-card empty">
        <p>Spending by category appears here once you add an expense.</p>
      </div>
    )
  }

  let offset = 0
  const segments = totals.map((c) => {
    const length = (c.total / grandTotal) * CIRCUMFERENCE
    const segment = { ...c, length, offset }
    offset += length
    return segment
  })
  const focus = totals.find((c) => c.id === active)

  return (
    <div className="card chart-card">
      <h2 className="card-title">Where the money went</h2>
      <div className="chart-layout">
        <svg className="donut" viewBox={`0 0 ${SIZE} ${SIZE}`} role="img"
          aria-label={totals.map((c) => `${c.label} ${formatPaise(c.total)}`).join(', ')}>
          <g transform={`rotate(-90 ${SIZE / 2} ${SIZE / 2})`}>
            {segments.map((s) => (
              <circle
                key={s.id}
                className={`donut-segment ${active && active !== s.id ? 'is-dim' : ''}`}
                cx={SIZE / 2}
                cy={SIZE / 2}
                r={RADIUS}
                fill="none"
                stroke={s.color}
                strokeWidth={active === s.id ? STROKE + 6 : STROKE}
                strokeDasharray={`${Math.max(s.length - 2, 0.5)} ${CIRCUMFERENCE}`}
                strokeDashoffset={-s.offset}
                onMouseEnter={() => setActive(s.id)}
                onMouseLeave={() => setActive(null)}
              />
            ))}
          </g>
          <text x="50%" y="46%" textAnchor="middle" className="donut-label">
            {focus ? focus.label : 'Total'}
          </text>
          <text x="50%" y="58%" textAnchor="middle" className="donut-value">
            {formatPaise(focus ? focus.total : grandTotal)}
          </text>
        </svg>

        <ul className="legend">
          {totals.map((c) => (
            <li key={c.id}>
              <button type="button" className={`legend-item ${active === c.id ? 'is-active' : ''}`}
                onMouseEnter={() => setActive(c.id)} onMouseLeave={() => setActive(null)}
                onFocus={() => setActive(c.id)} onBlur={() => setActive(null)}>
                <span className="swatch" style={{ background: c.color }} aria-hidden="true" />
                <span className="legend-name">{c.label}</span>
                <span className="figure">{formatPaise(c.total)}</span>
                <span className="legend-pct">{Math.round((c.total / grandTotal) * 100)}%</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
