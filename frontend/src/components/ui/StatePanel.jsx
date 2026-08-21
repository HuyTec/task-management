function StatePanel({ action, compact = false, description, eyebrow, title, tone = 'neutral' }) {
  return (
    <section
      className={`state-panel state-panel--${tone}${compact ? ' state-panel--compact' : ''}`}
      aria-live={tone === 'loading' ? 'polite' : undefined}
      aria-busy={tone === 'loading' ? 'true' : undefined}
    >
      <span className="state-panel__mark" aria-hidden="true">
        {tone === 'loading' ? <span className="state-panel__spinner" /> : tone === 'error' ? '!' : '·'}
      </span>
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h2>{title}</h2>
        {description && <p>{description}</p>}
        {action && <div className="state-panel__action">{action}</div>}
      </div>
    </section>
  )
}

export default StatePanel
