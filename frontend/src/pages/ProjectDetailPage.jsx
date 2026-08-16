import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { getProjectById, getProjectTasks } from '../api/projectApi'
import AppHeader from '../components/layout/AppHeader'
import Pagination from '../components/layout/Pagination'
import { formatDate, formatEnum } from '../utils/entityFormatters'
import getApiErrorMessage from '../utils/getApiErrorMessage'

function getScheduleState(project) {
  const today = new Date()
  const offset = today.getTimezoneOffset() * 60_000
  const todayValue = new Date(today.getTime() - offset).toISOString().slice(0, 10)

  if (todayValue < project.startDate) {
    return { label: 'Upcoming', className: 'category-badge' }
  }
  if (todayValue > project.endDate) {
    return { label: 'Completed', className: 'detail-status detail-status--done' }
  }
  return {
    label: 'In progress',
    className: 'status-badge status-badge--active',
  }
}

function getDuration(startDate, endDate) {
  const start = Date.parse(`${startDate}T00:00:00Z`)
  const end = Date.parse(`${endDate}T00:00:00Z`)
  if (Number.isNaN(start) || Number.isNaN(end)) return 'Not available'
  return `${Math.floor((end - start) / 86_400_000) + 1} days`
}

function ProjectDetailPage() {
  const { projectId } = useParams()
  const [project, setProject] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [tasks, setTasks] = useState([])
  const [taskPage, setTaskPage] = useState(null)
  const [taskPageNumber, setTaskPageNumber] = useState(0)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadProject() {
      setIsLoading(true)
      setProject(null)
      setError('')
      try {
        const [projectData, taskData] = await Promise.all([
          getProjectById(projectId, controller.signal),
          getProjectTasks(projectId, { page: taskPageNumber, size: 10 }, controller.signal),
        ])
        setProject(projectData)
        setTasks(taskData.content)
        setTaskPage(taskData)
      } catch (apiError) {
        if (apiError.code !== 'ERR_CANCELED') {
          setError(getApiErrorMessage(apiError, 'Unable to load this project.'))
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    loadProject()
    return () => controller.abort()
  }, [projectId, reloadKey, taskPageNumber])

  const scheduleState = useMemo(
    () => project ? getScheduleState(project) : null,
    [project],
  )

  return (
    <main className="dashboard-shell tab-theme tab-theme--projects">
      <AppHeader />
      <section className="dashboard-content">
        <Link className="back-link" to="/projects">Back to project portfolio</Link>

        {isLoading && <p className="dashboard-lead">Loading project details...</p>}

        {!isLoading && error && (
          <div className="detail-error">
            <h1>Project unavailable.</h1>
            <p className="form-alert form-alert--error" role="alert">{error}</p>
            <button
              className="text-button"
              type="button"
              onClick={() => setReloadKey((key) => key + 1)}
            >
              Try again
            </button>
          </div>
        )}

        {!isLoading && project && (
          <>
            <div className="detail-hero">
              <div>
                <p className="eyebrow">Project details</p>
                <h1>{project.name}</h1>
                <p className="dashboard-lead">
                  {project.description || 'No description has been added to this project.'}
                </p>
              </div>
              <div className="detail-hero__actions">
                <span className="status-badge">{formatEnum(project.currentUserRole)}</span>
                <span className={scheduleState.className}>{scheduleState.label}</span>
                <details className="overflow-menu">
                  <summary aria-label="Open project actions">☰</summary>
                  <div className="overflow-menu__panel">
                    <Link to={`/projects/${projectId}/members`}>Member details</Link>
                  </div>
                </details>
              </div>
            </div>

            <dl className="detail-grid detail-grid--three">
              <div><dt>Start date</dt><dd>{formatDate(project.startDate)}</dd></div>
              <div><dt>End date</dt><dd>{formatDate(project.endDate)}</dd></div>
              <div><dt>Planned duration</dt><dd>{getDuration(project.startDate, project.endDate)}</dd></div>
            </dl>

            <section className="linked-task-panel">
              <p className="eyebrow">Project work</p>
              <div className="section-heading">
                <h2>{taskPage?.totalElements ? `${taskPage.totalElements} linked task${taskPage.totalElements === 1 ? '' : 's'}` : 'No tasks assigned yet.'}</h2>
                {(project.currentUserRole === 'OWNER' || project.currentUserRole === 'MANAGER') && <Link className="primary-button primary-button--fit" to={`/tasks/new?projectId=${projectId}`}>New task</Link>}
              </div>
              {tasks.length ? (
                <div className="table-scroll entity-table-wrap">
                  <table className="dashboard-table">
                    <thead><tr><th>Task</th><th>Assignee</th><th>Status</th><th>Priority</th><th>Due date</th></tr></thead>
                    <tbody>
                      {tasks.map((task) => (
                        <tr key={task.id}>
                          <td><Link className="table-primary-link" to={`/tasks/${task.id}`}>{task.title}</Link></td>
                          <td>{task.assigneeDisplayName || task.assigneeUsername || 'Open for claim'}</td>
                          <td><span className={`detail-status detail-status--${task.status?.toLowerCase()}`}>{formatEnum(task.status)}</span></td>
                          <td><span className={`priority-badge priority-badge--${task.priority?.toLowerCase()}`}>{formatEnum(task.priority)}</span></td>
                          <td>{formatDate(task.dueDate)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p>Assign a task from the task form when this project is ready for execution.</p>
              )}
              <Pagination page={taskPage} label="project tasks" onPageChange={setTaskPageNumber} />
              <Link className="text-button inline-action" to="/tasks">Open task board</Link>
            </section>

          </>
        )}
      </section>
    </main>
  )
}

export default ProjectDetailPage
