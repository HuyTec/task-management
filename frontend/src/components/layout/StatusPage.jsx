import { Link, useNavigate } from 'react-router-dom'

import AppLogo from '../icons/AppLogo'

function StatusPage({ code, eyebrow, title, description }) {
  const navigate = useNavigate()
  const isAuthenticated = Boolean(localStorage.getItem('accessToken'))
  const homePath = isAuthenticated ? '/dashboard' : '/login'

  function goBack() {
    if (window.history.length > 1) {
      navigate(-1)
      return
    }

    navigate(homePath, { replace: true })
  }

  return (
    <main className="status-page">
      <section className="status-card" aria-labelledby={`status-title-${code}`}>
        <Link className="status-brand" to={homePath} aria-label="Task Management home">
          <AppLogo className="brand-mark brand-mark--small" />
          <span>Task Management</span>
        </Link>

        <div className="status-code" aria-hidden="true">{code}</div>
        <div className="status-copy">
          <p className="eyebrow">{eyebrow}</p>
          <h1 id={`status-title-${code}`}>{title}</h1>
          <p>{description}</p>
        </div>

        <div className="status-actions">
          <Link className="primary-button status-primary-action" to={homePath}>
            {isAuthenticated ? 'Back to workspace' : 'Go to sign in'}
          </Link>
          <button className="text-button" type="button" onClick={goBack}>Go back</button>
        </div>
      </section>
    </main>
  )
}

export default StatusPage
