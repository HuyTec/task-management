import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { addProjectMember, getProjectById, getProjectMembers, getProjectTasks, removeProjectMember } from '../api/projectApi'
import { createTask } from '../api/taskApi'
import AppHeader from '../components/layout/AppHeader'
import TaskForm from '../components/tasks/TaskForm'
import { formatDate, formatDateTime, formatEnum } from '../utils/entityFormatters'
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
  const [members, setMembers] = useState([])
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showTaskForm, setShowTaskForm] = useState(false)
  const [isSavingTask, setIsSavingTask] = useState(false)
  const [memberUsername, setMemberUsername] = useState('')
  const [memberRole, setMemberRole] = useState('MEMBER')
  const [isSavingMember, setIsSavingMember] = useState(false)
  const [removingUsername, setRemovingUsername] = useState('')
  const [memberError, setMemberError] = useState('')
  const [memberSuccess, setMemberSuccess] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadProject() {
      setIsLoading(true)
      setProject(null)
      setError('')
      try {
        const [projectData, taskData, memberData] = await Promise.all([getProjectById(projectId, controller.signal), getProjectTasks(projectId, controller.signal), getProjectMembers(projectId, controller.signal)])
        setProject(projectData)
        setTasks(taskData)
        setMembers(memberData)
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
  }, [projectId, reloadKey])

  const scheduleState = useMemo(
    () => project ? getScheduleState(project) : null,
    [project],
  )

  async function addTask(payload) {
    setIsSavingTask(true)
    setError('')
    setSuccess('')
    try {
      await createTask(payload)
      setShowTaskForm(false)
      setSuccess('Task created and assigned to this project successfully.')
      setReloadKey((key) => key + 1)
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to create a task for this project.'))
    } finally {
      setIsSavingTask(false)
    }
  }

  async function handleAddMember(event) {
    event.preventDefault()
    setIsSavingMember(true)
    setMemberError('')
    setMemberSuccess('')
    try {
      const newMember = await addProjectMember(projectId, { username: memberUsername.trim(), role: memberRole })
      setMembers((current) => [...current, newMember])
      setMemberUsername('')
      setMemberRole('MEMBER')
      setMemberSuccess(`${newMember.displayName || newMember.username} added to the project.`)
    } catch (apiError) {
      setMemberError(getApiErrorMessage(apiError, 'Unable to add this project member.'))
    } finally {
      setIsSavingMember(false)
    }
  }

  async function handleRemoveMember(member) {
    if (!window.confirm(`Remove ${member.displayName || member.username} from this project?`)) return
    setRemovingUsername(member.username)
    setMemberError('')
    setMemberSuccess('')
    try {
      await removeProjectMember(projectId, member.username)
      setMembers((current) => current.filter((item) => item.username !== member.username))
      setMemberSuccess(`${member.displayName || member.username} removed from the project.`)
    } catch (apiError) {
      setMemberError(getApiErrorMessage(apiError, 'Unable to remove this project member.'))
    } finally {
      setRemovingUsername('')
    }
  }

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
              <span className={scheduleState.className}>{scheduleState.label}</span>
            </div>

            <dl className="detail-grid detail-grid--three">
              <div><dt>Start date</dt><dd>{formatDate(project.startDate)}</dd></div>
              <div><dt>End date</dt><dd>{formatDate(project.endDate)}</dd></div>
              <div><dt>Planned duration</dt><dd>{getDuration(project.startDate, project.endDate)}</dd></div>
            </dl>

            <section className="linked-task-panel">
              <p className="eyebrow">Project work</p>
              <div className="section-heading">
                <h2>{tasks.length ? `${tasks.length} linked task${tasks.length === 1 ? '' : 's'}` : 'No tasks assigned yet.'}</h2>
                <button className="primary-button primary-button--fit" type="button" onClick={() => { setShowTaskForm(true); setError(''); setSuccess('') }}>New task</button>
              </div>
              {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
              {success && <p className="form-alert form-alert--success" role="status">{success}</p>}
              {showTaskForm && <TaskForm fixedProject={project} isSaving={isSavingTask} onCancel={() => setShowTaskForm(false)} onSubmit={addTask} />}
              {tasks.length ? (
                <div className="table-scroll entity-table-wrap">
                  <table className="dashboard-table">
                    <thead><tr><th>Task</th><th>Status</th><th>Priority</th><th>Due date</th></tr></thead>
                    <tbody>
                      {tasks.map((task) => (
                        <tr key={task.id}>
                          <td><Link className="table-primary-link" to={`/tasks/${task.id}`}>{task.title}</Link></td>
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
              <Link className="text-button inline-action" to="/tasks">Open task board</Link>
            </section>

            <section className="linked-task-panel project-members-panel">
              <p className="eyebrow">Project team</p>
              <div className="section-heading"><h2>{members.length} project member{members.length === 1 ? '' : 's'}</h2></div>
              <form className="member-add-form" onSubmit={handleAddMember}>
                <label className="form-field"><span>Username</span><input value={memberUsername} onChange={(event) => setMemberUsername(event.target.value)} placeholder="Enter an existing username" required maxLength="255" /></label>
                <label className="form-field"><span>Role</span><select value={memberRole} onChange={(event) => setMemberRole(event.target.value)}><option value="MEMBER">Member</option><option value="VIEWER">Viewer</option></select></label>
                <button className="primary-button primary-button--fit" type="submit" disabled={isSavingMember}>{isSavingMember ? 'Adding...' : 'Add member'}</button>
              </form>
              {memberError && <p className="form-alert form-alert--error" role="alert">{memberError}</p>}
              {memberSuccess && <p className="form-alert form-alert--success" role="status">{memberSuccess}</p>}
              <div className="member-list">
                {members.map((member) => (
                  <article className="member-row" key={member.username}>
                    <div className="member-avatar" aria-hidden="true">{(member.displayName || member.username).charAt(0).toUpperCase()}</div>
                    <div className="member-identity"><strong>{member.displayName || member.username}</strong><span>@{member.username} · {member.email}</span></div>
                    <div className="member-meta"><span className={`status-badge ${member.role === 'OWNER' ? 'status-badge--active' : ''}`}>{formatEnum(member.role)}</span><small>Joined {formatDateTime(member.joinedAt)}</small></div>
                    <button className="text-button action-button--delete" type="button" disabled={member.role === 'OWNER' || Boolean(removingUsername)} onClick={() => handleRemoveMember(member)}>{removingUsername === member.username ? 'Removing...' : 'Remove'}</button>
                  </article>
                ))}
              </div>
            </section>
          </>
        )}
      </section>
    </main>
  )
}

export default ProjectDetailPage
