import { useNavigate } from 'react-router-dom'

function DashboardPage() {
  const navigate = useNavigate()

  function handleSignOut() {
    localStorage.removeItem('accessToken')
    navigate('/login', { replace: true })
  }

  return (
    <main className="dashboard-shell">
      <header className="dashboard-header">
        <div className="dashboard-brand"><span className="brand-mark brand-mark--small" aria-hidden="true">HT</span><div><strong>Task Management</strong><span>Personal workspace</span></div></div>
        <button className="text-button" type="button" onClick={handleSignOut}>Sign out</button>
      </header>
      <section className="dashboard-content">
        <p className="eyebrow">Workspace ready</p>
        <h1>Welcome back.</h1>
        <p className="dashboard-lead">Authentication is connected. Task and expense modules can now be added to this workspace one feature at a time.</p>
        <div className="dashboard-grid">
          <article className="metric-card metric-card--accent"><span>Next milestone</span><strong>Task dashboard</strong><p>Build the first real feature on top of this authenticated shell.</p></article>
          <article className="metric-card"><span>Foundation</span><strong>Authentication</strong><p>Login, protected routing and a clear error state are in place.</p></article>
          <article className="metric-card"><span>Design system</span><strong>Ready to extend</strong><p>Shared colors, spacing, controls and responsive rules are defined.</p></article>
        </div>
      </section>
    </main>
  )
}

export default DashboardPage
