import { Link } from 'react-router-dom'

import AppHeader from '../components/layout/AppHeader'

function DashboardPage() {
  return (
    <main className="dashboard-shell">
      <AppHeader />
      <section className="dashboard-content">
        <p className="eyebrow">Personal workspace</p>
        <h1>Welcome back.</h1>
        <p className="dashboard-lead">Plan your work, keep personal costs organized and maintain your account from one calm workspace.</p>
        <div className="dashboard-grid">
          <Link className="metric-card metric-card--accent metric-card--link" to="/tasks"><span>Workflow</span><strong>Task board</strong><p>Create, prioritize and move work across the Kanban board.</p></Link>
          <Link className="metric-card metric-card--link" to="/expenses"><span>Personal finance</span><strong>Expense ledger</strong><p>Record private costs and connect them to relevant tasks.</p></Link>
          <Link className="metric-card metric-card--link" to="/profile"><span>Account</span><strong>Your profile</strong><p>Review and maintain the information attached to your account.</p></Link>
        </div>
      </section>
    </main>
  )
}

export default DashboardPage
