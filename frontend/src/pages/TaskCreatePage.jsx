import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'

import { getMyProjects, getProjectById } from '../api/projectApi'
import { addTaskCriterion, assignTask, createTask } from '../api/taskApi'
import AppHeader from '../components/layout/AppHeader'
import TaskCreateForm from '../components/tasks/TaskCreateForm'
import getApiErrorMessage from '../utils/getApiErrorMessage'

function TaskCreatePage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const projectId = searchParams.get('projectId')
  const [projects, setProjects] = useState([])
  const [fixedProject, setFixedProject] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const [partiallyCreatedTaskId, setPartiallyCreatedTaskId] = useState(null)

  useEffect(() => {
    const controller = new AbortController()

    async function loadRelations() {
      setIsLoading(true)
      setError('')
      try {
        const projectPagePromise = getMyProjects({ page: 0, size: 100, sort: 'name,asc' }, controller.signal)
        const fixedProjectPromise = projectId
          ? getProjectById(projectId, controller.signal)
          : Promise.resolve(null)
        const [projectPage, selectedProject] = await Promise.all([projectPagePromise, fixedProjectPromise])
        setProjects(projectPage.content)
        setFixedProject(selectedProject)
      } catch (apiError) {
        if (apiError.code !== 'ERR_CANCELED') {
          setError(getApiErrorMessage(apiError, 'Unable to prepare the task form.'))
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    loadRelations()
    return () => controller.abort()
  }, [projectId])

  async function saveTask({ taskData, criteria, assigneeUsername }) {
    setIsSaving(true)
    setError('')
    setPartiallyCreatedTaskId(null)
    let task = null
    try {
      task = await createTask(taskData)
      for (const [position, content] of criteria.entries()) {
        await addTaskCriterion(task.id, { content, position })
      }
      if (assigneeUsername) await assignTask(task.id, assigneeUsername)
      navigate(`/tasks/${task.id}`, { replace: true })
    } catch (apiError) {
      if (task) {
        setPartiallyCreatedTaskId(task.id)
        setError(`The task was created, but its workflow setup is incomplete. ${getApiErrorMessage(apiError, 'Open the task to finish setup safely.')}`)
      } else {
        setError(getApiErrorMessage(apiError, 'Unable to create the task.'))
      }
    } finally {
      setIsSaving(false)
    }
  }

  const cancelPath = fixedProject ? `/projects/${fixedProject.id}` : '/tasks'

  return (
    <main className="dashboard-shell tab-theme tab-theme--tasks">
      <AppHeader />
      <section className="dashboard-content">
        <Link className="back-link" to={cancelPath}>Cancel task creation</Link>
        <div className="page-heading">
          <div>
            <p className="eyebrow">Task planning</p>
            <h1>Create a focused work item.</h1>
            <p className="dashboard-lead">Define the outcome, verification checklist and responsibility before execution begins.</p>
          </div>
        </div>
        {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
        {partiallyCreatedTaskId && <Link className="primary-button primary-button--fit partial-task-link" to={`/tasks/${partiallyCreatedTaskId}`}>Open created task</Link>}
        {isLoading ? (
          <p className="dashboard-lead">Preparing task relations...</p>
        ) : fixedProject && fixedProject.currentUserRole !== 'OWNER' && fixedProject.currentUserRole !== 'MANAGER' ? (
          <p className="form-alert form-alert--warning">Only the project OWNER or MANAGER can create a project task.</p>
        ) : (
          <TaskCreateForm projects={projects} fixedProject={fixedProject} isSaving={isSaving || Boolean(partiallyCreatedTaskId)} onCancel={() => navigate(cancelPath)} onSubmit={saveTask} />
        )}
      </section>
    </main>
  )
}

export default TaskCreatePage
