import { Link } from 'react-router-dom'

import AppShell from '../components/layout/AppShell'

function DashboardPage() {
  return (
    <AppShell theme="home">
      <section className="dashboard-hero">
        <div className="dashboard-hero__copy">
          <p className="eyebrow">Personal workspace</p>
          <h1>Make space for meaningful progress.</h1>
          <p className="dashboard-lead">Plan your work, keep personal costs organized and maintain your account from one calm workspace.</p>
          <div className="dashboard-hero__actions">
            <Link className="primary-button primary-button--fit" to="/tasks">Open task board</Link>
            <Link className="text-button" to="/projects">View projects</Link>
          </div>
        </div>
        <aside className="dashboard-focus" aria-label="Workspace philosophy">
          <span className="dashboard-focus__index">01</span>
          <div><strong>One clear next step</strong><p>Separate planning, execution and review so every action has a visible purpose.</p></div>
        </aside>
      </section>

      <div className="section-intro">
        <div><p className="eyebrow">Your toolkit</p><h2>Everything in its right place.</h2></div>
        <p>Move between focused areas without losing the context that connects them.</p>
      </div>
      <div className="dashboard-grid">
        <Link className="metric-card metric-card--project metric-card--link" to="/projects"><span>Planning</span><strong>Project portfolio</strong><p>Group related outcomes inside a clear delivery window.</p><em>Explore projects <span aria-hidden="true">→</span></em></Link>
        <Link className="metric-card metric-card--accent metric-card--link" to="/tasks"><span>Workflow</span><strong>Task board</strong><p>Create, prioritize and move work across the Kanban board.</p><em>Plan your work <span aria-hidden="true">→</span></em></Link>
        <Link className="metric-card metric-card--expense metric-card--link" to="/expenses"><span>Personal finance</span><strong>Expense ledger</strong><p>Record private costs and connect them to relevant tasks.</p><em>Review expenses <span aria-hidden="true">→</span></em></Link>
        <Link className="metric-card metric-card--profile metric-card--link" to="/profile"><span>Account</span><strong>Your profile</strong><p>Review and maintain the information attached to your account.</p><em>Manage profile <span aria-hidden="true">→</span></em></Link>
      </div>
    </AppShell>
  )
}

export default DashboardPage
