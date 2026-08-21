const ICONS = {
  error: '!',
  warning: '!',
  success: '✓',
  info: 'i',
}

function Alert({ children, title, tone = 'info', className = '' }) {
  const role = tone === 'error' ? 'alert' : 'status'

  return (
    <div className={`alert alert--${tone} ${className}`.trim()} role={role}>
      <span className="alert__icon" aria-hidden="true">{ICONS[tone]}</span>
      <div className="alert__content">
        {title && <strong>{title}</strong>}
        <div>{children}</div>
      </div>
    </div>
  )
}

export default Alert
