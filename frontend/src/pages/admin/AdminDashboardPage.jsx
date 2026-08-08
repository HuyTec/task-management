import { useEffect, useState } from 'react'

import { getAllTasks } from '../../api/taskApi'
import { getAllUsers } from '../../api/userApi'
import AdminHeader from '../../components/admin/AdminHeader'
import getApiErrorMessage from '../../utils/getApiErrorMessage'

function AdminDashboardPage() {
  const [summary, setSummary] = useState({ users: 0, tasks: 0 })
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadSummary() {
      setError('')
      setIsLoading(true)

      try {
        const [users, tasks] = await Promise.all([
          getAllUsers(controller.signal),
          getAllTasks(controller.signal),
        ])

        setSummary({ users: users.length, tasks: tasks.length })
      } catch (apiError) {
        if (apiError.code !== 'ERR_CANCELED') {
          setError(getApiErrorMessage(apiError, 'Unable to load the administration summary.'))
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    loadSummary()

    return () => controller.abort()
  }, [reloadKey])

  return (
    <main className="dashboard-shell">
      <AdminHeader />
      <section className="dashboard-content">
        <p className="eyebrow">Administration</p>
        <h1>System overview.</h1>
        <p className="dashboard-lead">A concise view of the management capabilities currently exposed by the backend.</p>

        {isLoading && <p className="dashboard-lead">Loading system summary...</p>}
        {!isLoading && error && (
          <>
            <p className="form-alert form-alert--error" role="alert">{error}</p>
            <button className="text-button" type="button" onClick={() => setReloadKey((key) => key + 1)}>Try again</button>
          </>
        )}
        {!isLoading && !error && (
          <div className="dashboard-grid">
            <article className="metric-card metric-card--accent"><span>Users</span><strong>{summary.users}</strong><p>Accounts visible to the administrator.</p></article>
            <article className="metric-card"><span>Tasks</span><strong>{summary.tasks}</strong><p>Tasks across the complete system.</p></article>
            <article className="metric-card"><span>Expenses</span><strong>API pending</strong><p>The backend does not yet expose an administrator expense listing.</p></article>
          </div>
        )}
      </section>
    </main>
  )
}

export default AdminDashboardPage
