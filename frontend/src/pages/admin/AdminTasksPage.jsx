import { useEffect, useState } from 'react'

import { getAllTasks } from '../../api/taskApi'
import AdminHeader from '../../components/admin/AdminHeader'
import getApiErrorMessage from '../../utils/getApiErrorMessage'

function formatTotal(total) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(total || 0)
}

function formatDate(value) {
  if (!value) return 'Not set'

  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return value

  return new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(date)
}

function AdminTasksPage() {
  const [tasks, setTasks] = useState([])
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadTasks() {
      setError('')
      setIsLoading(true)

      try {
        setTasks(await getAllTasks(controller.signal))
      } catch (apiError) {
        if (apiError.code !== 'ERR_CANCELED') {
          setError(getApiErrorMessage(apiError, 'Unable to load tasks.'))
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    loadTasks()

    return () => controller.abort()
  }, [reloadKey])

  return (
    <main className="dashboard-shell">
      <AdminHeader />
      <section className="dashboard-content">
        <p className="eyebrow">Task management</p>
        <h1>System tasks.</h1>
        <p className="dashboard-lead">The backend currently permits administrators to list all tasks. Editing and deletion remain ownership-restricted.</p>

        {isLoading && <p className="dashboard-lead">Loading tasks...</p>}
        {!isLoading && error && (
          <>
            <p className="form-alert form-alert--error" role="alert">{error}</p>
            <button className="text-button" type="button" onClick={() => setReloadKey((key) => key + 1)}>Try again</button>
          </>
        )}
        {!isLoading && !error && tasks.length === 0 && <p className="dashboard-lead">No tasks found.</p>}
        {tasks.length > 0 && (
          <div className="table-scroll">
            <table className="dashboard-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Title</th>
                  <th>Status</th>
                  <th>Priority</th>
                  <th>Due date</th>
                  <th>Expense total</th>
                </tr>
              </thead>
              <tbody>
                {tasks.map((task) => (
                  <tr key={task.id} className={task.priority === 'URGENT' ? 'row--accent' : ''}>
                    <td>#{task.id}</td>
                    <td>{task.title}</td>
                    <td>{task.status}</td>
                    <td>{task.priority}</td>
                    <td>{formatDate(task.dueDate)}</td>
                    <td>{formatTotal(task.total)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  )
}

export default AdminTasksPage
