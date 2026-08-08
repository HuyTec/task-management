import AppHeader from '../components/layout/AppHeader'

function DashboardPage() {
  return (
    <main className="dashboard-shell">
      <AppHeader />
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
