import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import {
  createProject,
  deleteProjectById,
  getMyProjects,
  getProjectById,
  updateProjectById,
} from '../api/projectApi'
import AppShell from '../components/layout/AppShell'
import Pagination from '../components/layout/Pagination'
import ProjectForm from '../components/projects/ProjectForm'
import StatePanel from '../components/ui/StatePanel'
import { formatDate, formatEnum } from '../utils/entityFormatters'
import getApiErrorMessage from '../utils/getApiErrorMessage'
import { decrementPageTotal } from '../utils/pageUtils'

function localDateValue() {
  const today = new Date()
  const offset = today.getTimezoneOffset() * 60_000
  return new Date(today.getTime() - offset).toISOString().slice(0, 10)
}

function getProjectPhase(project, today = localDateValue()) {
  if (today < project.startDate) return { label: 'Upcoming', className: 'category-badge' }
  if (today > project.endDate) return { label: 'Completed', className: 'detail-status detail-status--done' }
  return { label: 'In progress', className: 'status-badge status-badge--active' }
}

function ProjectsPage() {
  const [projects, setProjects] = useState([])
  const [projectPage, setProjectPage] = useState(null)
  const [pageNumber, setPageNumber] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [busyProjectId, setBusyProjectId] = useState(null)
  const [editingProject, setEditingProject] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const phaseTotals = useMemo(() => projects.reduce((totals, project) => {
    const phase = getProjectPhase(project).label
    totals[phase] = (totals[phase] || 0) + 1
    return totals
  }, {}), [projects])

  const loadProjects = useCallback(async (signal) => {
    setIsLoading(true)
    setError('')
    try {
      const data = await getMyProjects({ page: pageNumber, size: 20 }, signal)
      setProjects(data.content)
      setProjectPage(data)
    } catch (apiError) {
      if (apiError.code !== 'ERR_CANCELED') {
        setError(getApiErrorMessage(apiError, 'Unable to load your projects.'))
      }
    } finally {
      if (!signal?.aborted) setIsLoading(false)
    }
  }, [pageNumber])

  useEffect(() => {
    const controller = new AbortController()
    loadProjects(controller.signal)
    return () => controller.abort()
  }, [loadProjects])

  function openCreate() {
    setEditingProject(null)
    setShowForm(true)
    setError('')
    setSuccess('')
  }

  async function openEdit(project) {
    setBusyProjectId(project.id)
    setError('')
    try {
      setEditingProject(await getProjectById(project.id))
      setShowForm(true)
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to load project details.'))
    } finally {
      setBusyProjectId(null)
    }
  }

  async function saveProject(payload) {
    setIsSaving(true)
    setError('')
    setSuccess('')
    try {
      if (editingProject) {
        await updateProjectById(editingProject.id, payload)
        setSuccess('Project updated successfully.')
      } else {
        await createProject(payload)
        setSuccess('Project created successfully.')
      }
      setShowForm(false)
      setEditingProject(null)
      await loadProjects()
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to save the project.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function removeProject(project) {
    if (!window.confirm(`Delete "${project.name}"? This action cannot be undone.`)) return

    setBusyProjectId(project.id)
    setError('')
    setSuccess('')
    try {
      await deleteProjectById(project.id)
      setSuccess('Project deleted successfully.')
      if (projects.length === 1 && pageNumber > 0) {
        setPageNumber((current) => current - 1)
      } else {
        setProjects((current) => current.filter((item) => item.id !== project.id))
        setProjectPage(decrementPageTotal)
      }
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to delete the project.'))
    } finally {
      setBusyProjectId(null)
    }
  }

  return (
    <AppShell theme="projects" wide>
        <div className="page-heading">
          <div>
            <p className="eyebrow">Project planning</p>
            <h1>Your project portfolio.</h1>
            <p className="dashboard-lead">
              Give related outcomes a shared direction and a clear delivery window.
            </p>
          </div>
          <button className="primary-button primary-button--fit" type="button" onClick={openCreate}>
            New project
          </button>
        </div>

        <div className="summary-strip">
          <div><span>Total projects</span><strong>{projectPage?.totalElements || 0}</strong></div>
          <div><span>In progress on page</span><strong>{phaseTotals['In progress'] || 0}</strong></div>
          <div><span>Upcoming on page</span><strong>{phaseTotals.Upcoming || 0}</strong></div>
        </div>

        {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
        {success && <p className="form-alert form-alert--success" role="status">{success}</p>}

        {showForm && (
          <ProjectForm
            initialProject={editingProject}
            isSaving={isSaving}
            onCancel={() => {
              setShowForm(false)
              setEditingProject(null)
            }}
            onSubmit={saveProject}
          />
        )}

        {isLoading ? (
          <StatePanel compact tone="loading" title="Loading your projects" description="Preparing your portfolio and membership roles." />
        ) : (
          <div className="table-scroll entity-table-wrap">
            <table className="dashboard-table project-table mobile-card-table">
              <thead>
                <tr>
                  <th>Project</th>
                  <th>Schedule</th>
                  <th>Phase</th>
                  <th>Your role</th>
                  <th>Description</th>
                  <th><span className="sr-only">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                {projects.map((project) => {
                  const phase = getProjectPhase(project)
                  const canEdit = project.currentUserRole === 'OWNER' || project.currentUserRole === 'MANAGER'
                  const canDelete = project.currentUserRole === 'OWNER'
                  return (
                    <tr key={project.id}>
                      <td>
                        <Link className="table-primary-link" to={`/projects/${project.id}`}>
                          {project.name}
                        </Link>
                      </td>
                      <td>{formatDate(project.startDate)} to {formatDate(project.endDate)}</td>
                      <td><span className={phase.className}>{phase.label}</span></td>
                      <td><span className={`status-badge ${project.currentUserRole === 'OWNER' ? 'status-badge--active' : ''}`}>{formatEnum(project.currentUserRole)}</span></td>
                      <td>{project.description || '-'}</td>
                      <td className="table-actions">
                        <Link className="text-button" to={`/projects/${project.id}/members`}>Team</Link>
                        {canEdit && <button
                          className="text-button"
                          type="button"
                          disabled={busyProjectId === project.id}
                          onClick={() => openEdit(project)}
                        >
                          Edit
                        </button>}
                        {canDelete && <button
                          className="text-button action-button--delete"
                          type="button"
                          disabled={busyProjectId === project.id}
                          onClick={() => removeProject(project)}
                        >
                          Delete
                        </button>}
                      </td>
                    </tr>
                  )
                })}
                {projects.length === 0 && (
                  <tr>
                    <td className="table-empty" colSpan="6">
                      No projects yet. Create one when several outcomes need a shared direction.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
        {!isLoading && <Pagination page={projectPage} label="projects" onPageChange={setPageNumber} />}
    </AppShell>
  )
}

export default ProjectsPage
