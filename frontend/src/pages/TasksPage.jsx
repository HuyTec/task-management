import { useCallback, useEffect, useState } from 'react'

import { createTask, deleteTaskById, getMyTasks, getTaskById, updateTaskById } from '../api/taskApi'
import AppHeader from '../components/layout/AppHeader'
import TaskCard from '../components/tasks/TaskCard'
import TaskForm from '../components/tasks/TaskForm'
import getApiErrorMessage from '../utils/getApiErrorMessage'

const COLUMNS = [
  ['TODO', 'To do', 'Ready to begin'],
  ['IN_PROGRESS', 'In progress', 'Work in motion'],
  ['DONE', 'Done', 'Completed outcomes'],
  ['BLOCKED', 'Blocked', 'Needs attention'],
]

function TasksPage() {
  const [tasks, setTasks] = useState([])
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
      setTasks(await getMyTasks(signal))
    } catch (apiError) {
      if (apiError.code !== 'ERR_CANCELED') setError(getApiErrorMessage(apiError, 'Unable to load your tasks.'))
    } finally {
      if (!signal?.aborted) setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    loadTasks(controller.signal)
    return () => controller.abort()
  }, [loadTasks])

  function openCreate() {
    setEditingTask(null)
    setShowForm(true)
    setError('')
    setSuccess('')
  }

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
      if (editingTask) {
        await updateTaskById(editingTask.id, payload)
        setSuccess('Task updated successfully.')
      } else {
        await createTask(payload)
        setSuccess('Task created successfully.')
      }
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
    setError('')
    try {
      await updateTaskById(task.id, { status })
      setTasks((current) => current.map((item) => item.id === task.id ? { ...item, status } : item))
      setSuccess(`“${task.title}” moved successfully.`)
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to move the task.'))
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
      setTasks((current) => current.filter((item) => item.id !== task.id))
      setSuccess('Task deleted successfully.')
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to delete the task.'))
    } finally {
      setBusyTaskId(null)
    }
  }

  return (
    <main className="dashboard-shell">
      <AppHeader />
      <section className="dashboard-content dashboard-content--wide">
        <div className="page-heading">
          <div><p className="eyebrow">Personal workflow</p><h1>Your task board.</h1><p className="dashboard-lead">Organize work by outcome and move each task as its state changes.</p></div>
          <button className="primary-button primary-button--fit" type="button" onClick={openCreate}>New task</button>
        </div>
        {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
        {success && <p className="form-alert form-alert--success" role="status">{success}</p>}
        {showForm && <TaskForm initialTask={editingTask} isSaving={isSaving} onCancel={() => setShowForm(false)} onSubmit={saveTask} />}
        {isLoading ? <p className="dashboard-lead">Loading your board...</p> : (
          <div className="kanban-board">
            {COLUMNS.map(([status, title, note]) => {
              const columnTasks = tasks.filter((task) => task.status === status)
              return (
                <section className={`kanban-column kanban-column--${status.toLowerCase()}`} key={status}>
                  <header className="kanban-column__header"><div><h2>{title}</h2><p>{note}</p></div><span>{columnTasks.length}</span></header>
                  <div className="kanban-column__body">
                    {columnTasks.map((task) => <TaskCard key={task.id} task={task} busy={busyTaskId === task.id} onDelete={removeTask} onEdit={openEdit} onStatusChange={moveTask} />)}
                    {columnTasks.length === 0 && <p className="kanban-empty">No tasks here.</p>}
                  </div>
                </section>
              )
            })}
          </div>
        )}
      </section>
    </main>
  )
}

export default TasksPage
