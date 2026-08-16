import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { deleteTaskById, getMyTasks, getTaskById, updateTaskById } from '../api/taskApi'
import { getMyProjects } from '../api/projectApi'
import AppHeader from '../components/layout/AppHeader'
import Pagination from '../components/layout/Pagination'
import TaskCard from '../components/tasks/TaskCard'
import TaskForm from '../components/tasks/TaskForm'
import getApiErrorMessage from '../utils/getApiErrorMessage'
import { decrementPageTotal } from '../utils/pageUtils'

const COLUMNS = [
  ['TODO', 'To do', 'Ready to begin'],
  ['IN_PROGRESS', 'In progress', 'Work in motion'],
  ['IN_REVIEW', 'In review', 'Waiting for review'],
  ['CHANGES_REQUESTED', 'Changes requested', 'Needs revision'],
  ['DONE', 'Done', 'Completed outcomes'],
]

const WORKSPACES = [
  ['MY_WORK', 'My work', 'Personal tasks and active Project assignments'],
  ['REVIEW_QUEUE', 'Review queue', 'Project tasks waiting for your decision'],
  ['ALL_ACCESSIBLE', 'All accessible', 'Every task visible through your projects'],
]

function TasksPage() {
  const [tasks, setTasks] = useState([])
  const [taskPage, setTaskPage] = useState(null)
  const [projects, setProjects] = useState([])
  const [searchInput, setSearchInput] = useState('')
  const [query, setQuery] = useState({ page: 0, size: 20, search: '', status: '', priority: '', workspace: 'MY_WORK' })
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [busyTaskId, setBusyTaskId] = useState(null)
  const [editingTask, setEditingTask] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const loadTasks = useCallback(async (signal) => {
    setIsLoading(true)
    setError('')
    try {
      const params = Object.fromEntries(Object.entries(query).filter(([, value]) => value !== ''))
      const [taskData, projectData] = await Promise.all([
        getMyTasks(params, signal),
        getMyProjects({ page: 0, size: 100, sort: 'name,asc' }, signal),
      ])
      setTasks(taskData.content)
      setTaskPage(taskData)
      setProjects(projectData.content)
    } catch (apiError) {
      if (apiError.code !== 'ERR_CANCELED') setError(getApiErrorMessage(apiError, 'Unable to load your tasks.'))
    } finally {
      if (!signal?.aborted) setIsLoading(false)
    }
  }, [query])

  useEffect(() => {
    const controller = new AbortController()
    loadTasks(controller.signal)
    return () => controller.abort()
  }, [loadTasks])

  async function openEdit(task) {
    setBusyTaskId(task.id)
    setError('')
    try {
      setEditingTask(await getTaskById(task.id))
      setShowForm(true)
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to load task details.'))
    } finally {
      setBusyTaskId(null)
    }
  }

  async function saveTask(payload) {
    setIsSaving(true)
    setError('')
    setSuccess('')
    try {
      await updateTaskById(editingTask.id, payload)
      setSuccess('Task updated successfully.')
      setShowForm(false)
      setEditingTask(null)
      await loadTasks()
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to save the task.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function moveTask(task, status) {
    if (status === task.status) return
    setBusyTaskId(task.id)
    const previousStatus = task.status
    setTasks((current) => current.map((item) => item.id === task.id ? { ...item, status } : item))
    setError('')
    try {
      await updateTaskById(task.id, { status })
      setSuccess(`“${task.title}” moved successfully.`)
      if (query.status && query.status !== status) {
        setTasks((current) => current.filter((item) => item.id !== task.id))
        setTaskPage(decrementPageTotal)
      }
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to move the task.'))
      setTasks((current) => current.map((item) => item.id === task.id ? { ...item, status: previousStatus } : item))
    } finally {
      setBusyTaskId(null)
    }
  }

  async function removeTask(task) {
    if (!window.confirm(`Delete “${task.title}”? This action cannot be undone.`)) return
    setBusyTaskId(task.id)
    setError('')
    try {
      await deleteTaskById(task.id)
      setSuccess('Task deleted successfully.')
      if (tasks.length === 1 && query.page > 0) {
        setQuery((current) => ({ ...current, page: current.page - 1 }))
      } else {
        setTasks((current) => current.filter((item) => item.id !== task.id))
        setTaskPage(decrementPageTotal)
      }
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to delete the task.'))
    } finally {
      setBusyTaskId(null)
    }
  }

  function dropTask(event, status) {
    event.preventDefault()
    const taskId = Number(event.dataTransfer.getData('text/plain'))
    const task = tasks.find((item) => item.id === taskId)
    if (task && task.projectId == null) moveTask(task, status)
  }

  function updateFilter(name, value) {
    setQuery((current) => ({ ...current, [name]: value, page: 0 }))
  }

  function selectWorkspace(workspace) {
    setQuery((current) => ({ ...current, workspace, status: workspace === 'REVIEW_QUEUE' ? '' : current.status, page: 0 }))
  }

  function submitSearch(event) {
    event.preventDefault()
    updateFilter('search', searchInput.trim())
  }

  return (
    <main className="dashboard-shell tab-theme tab-theme--tasks">
      <AppHeader />
      <section className="dashboard-content dashboard-content--wide">
        <div className="page-heading">
          <div><p className="eyebrow">Workspaces</p><h1>Your task board.</h1><p className="dashboard-lead">Follow work you own, then switch to the review queue when a project decision is required.</p></div>
          <Link className="primary-button primary-button--fit" to="/tasks/new">New task</Link>
        </div>
        {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
        {success && <p className="form-alert form-alert--success" role="status">{success}</p>}
        {showForm && <TaskForm initialTask={editingTask} projects={projects} isSaving={isSaving} onCancel={() => setShowForm(false)} onSubmit={saveTask} />}
        <nav className="workspace-switcher" aria-label="Task workspace">
          {WORKSPACES.map(([value, label, description]) => <button className={query.workspace === value ? 'workspace-switcher__item workspace-switcher__item--active' : 'workspace-switcher__item'} type="button" key={value} onClick={() => selectWorkspace(value)}><strong>{label}</strong><span>{description}</span></button>)}
        </nav>
        <form className="list-toolbar" onSubmit={submitSearch}>
          <label className="form-field list-toolbar__search"><span>Search</span><input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Search title or description" maxLength="100" /></label>
          <label className="form-field"><span>Status</span><select value={query.status} onChange={(event) => updateFilter('status', event.target.value)}><option value="">All statuses</option>{COLUMNS.map(([status, title]) => <option key={status} value={status}>{title}</option>)}</select></label>
          <label className="form-field"><span>Priority</span><select value={query.priority} onChange={(event) => updateFilter('priority', event.target.value)}><option value="">All priorities</option><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option><option value="URGENT">Urgent</option></select></label>
          <button className="primary-button primary-button--fit" type="submit">Search</button>
        </form>
        {isLoading ? <p className="dashboard-lead">Loading your board...</p> : (
          <><p className="page-context">{WORKSPACES.find(([value]) => value === query.workspace)?.[1]} shows {tasks.length} task{tasks.length === 1 ? '' : 's'} on this page, from {taskPage?.totalElements || 0} matching tasks.</p><div className="kanban-board">
            {COLUMNS.map(([status, title, note]) => {
              const columnTasks = tasks.filter((task) => task.status === status)
              return (
                <section
                  className={`kanban-column kanban-column--${status.toLowerCase()}`}
                  key={status}
                  onDragOver={(event) => event.preventDefault()}
                  onDrop={(event) => dropTask(event, status)}
                >
                  <header className="kanban-column__header"><div><h2>{title}</h2><p>{note}</p></div><span>{columnTasks.length}</span></header>
                  <div className="kanban-column__body">
                    {columnTasks.map((task) => <TaskCard key={task.id} task={task} busy={busyTaskId === task.id} onDelete={removeTask} onEdit={openEdit} onStatusChange={moveTask} />)}
                    {columnTasks.length === 0 && <p className="kanban-empty">No tasks here.</p>}
                  </div>
                </section>
              )
            })}
          </div><Pagination page={taskPage} label="tasks" onPageChange={(page) => setQuery((current) => ({ ...current, page }))} /></>
        )}
      </section>
    </main>
  )
}

export default TasksPage
