function ConfirmationPanel({ children, description, title }) {
  return (
    <section className="confirmation-panel" aria-labelledby="confirmation-panel-title">
      <div className="confirmation-panel__heading">
        <span aria-hidden="true">✓</span>
        <div>
          <h3 id="confirmation-panel-title">{title}</h3>
          <p>{description}</p>
        </div>
      </div>
      {children}
    </section>
  )
}

export default ConfirmationPanel
