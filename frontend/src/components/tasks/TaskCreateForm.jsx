import { useEffect, useMemo, useState } from 'react'

import { getProjectMembers } from '../../api/projectApi'
import { formatEnum } from '../../utils/entityFormatters'
import getApiErrorMessage from '../../utils/getApiErrorMessage'

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
let nextCriterionKey = 0
const EMPTY_CRITERION = () => ({ key: nextCriterionKey++, content: '' })

function TaskCreateForm({ projects = [], fixedProject, isSaving, onCancel, onSubmit }) {
  const manageableProjects = useMemo(
    () => projects.filter((project) => project.currentUserRole === 'OWNER' || project.currentUserRole === 'MANAGER'),
    [projects],
  )
  const [form, setForm] = useState({
    title: '',
    description: '',
    priority: 'MEDIUM',
    dueDate: '',
    projectId: fixedProject?.id ? String(fixedProject.id) : '',
  })
  const [criteria, setCriteria] = useState([EMPTY_CRITERION()])
  const [assigneeUsername, setAssigneeUsername] = useState('')
  const [members, setMembers] = useState([])
  const [isLoadingMembers, setIsLoadingMembers] = useState(false)
  const [membersError, setMembersError] = useState('')

  useEffect(() => {
    if (fixedProject?.id) {
      setForm((current) => ({ ...current, projectId: String(fixedProject.id) }))
    }
  }, [fixedProject])

  const selectedProject = fixedProject || manageableProjects.find((project) => String(project.id) === form.projectId) || null
  const selectedProjectId = selectedProject?.id
  const isProjectTask = selectedProject != null

  useEffect(() => {
    if (!selectedProjectId) {
      setMembers([])
      setAssigneeUsername('')
      setMembersError('')
      return undefined
    }

    const controller = new AbortController()
    async function loadMembers() {
      setIsLoadingMembers(true)
      setMembersError('')
      try {
        const memberPage = await getProjectMembers(selectedProjectId, { page: 0, size: 100, sort: 'joinedAt,asc' }, controller.signal)
        setMembers(memberPage.content.filter((member) => member.role !== 'VIEWER'))
      } catch (apiError) {
        if (apiError.code !== 'ERR_CANCELED') setMembersError(getApiErrorMessage(apiError, 'Unable to load assignment candidates.'))
      } finally {
        if (!controller.signal.aborted) setIsLoadingMembers(false)
      }
    }
    loadMembers()
    return () => controller.abort()
  }, [selectedProjectId])

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function updateCriterion(key, content) {
    setCriteria((current) => current.map((criterion) => criterion.key === key ? { ...criterion, content } : criterion))
  }

  function removeCriterion(key) {
    setCriteria((current) => current.length === 1 ? current : current.filter((criterion) => criterion.key !== key))
  }

  function handleSubmit(event) {
    event.preventDefault()
    const normalizedCriteria = criteria.map((criterion) => criterion.content.trim()).filter(Boolean)
    if (isProjectTask && normalizedCriteria.length === 0) return

    onSubmit({
      taskData: {
        title: form.title.trim(),
        description: form.description.trim() || null,
        priority: form.priority,
        dueDate: form.dueDate || null,
        projectId: selectedProject?.id ?? null,
      },
      criteria: isProjectTask ? normalizedCriteria : [],
      assigneeUsername: isProjectTask ? assigneeUsername || null : null,
    })
  }

  const hasCriterion = criteria.some((criterion) => criterion.content.trim())
  const projectDatesInvalid = isProjectTask && form.dueDate && (
    (selectedProject.startDate && form.dueDate < selectedProject.startDate)
    || (selectedProject.endDate && form.dueDate > selectedProject.endDate)
  )
  const projectReady = !isProjectTask || (form.description.trim() && form.dueDate && hasCriterion && !projectDatesInvalid)

  return (
    <form className="task-create-form" onSubmit={handleSubmit}>
      <section className="create-task-section">
        <div className="create-task-section__heading">
          <div><p className="eyebrow">1 · Scope</p><h2>Define the expected outcome</h2></div>
          <span>Created as TODO</span>
        </div>
        <div className="create-task-grid">
          <label className="form-field create-task-grid__wide"><span>Task title</span><input name="title" value={form.title} onChange={updateField} required maxLength="160" autoFocus placeholder="Use a specific, outcome-oriented title" /></label>
          <label className="form-field create-task-grid__wide"><span>Description {isProjectTask && <em>Required for project work</em>}</span><textarea name="description" value={form.description} onChange={updateField} required={isProjectTask} rows="6" maxLength="2000" placeholder="Explain the context, expected result, constraints and relevant references" /></label>
          <label className="form-field"><span>Priority</span><select name="priority" value={form.priority} onChange={updateField}>{PRIORITIES.map((value) => <option key={value} value={value}>
  {formatEnum(value)}
</option>)}</select></label>
          <label className="form-field"><span>Due date {isProjectTask && <em>Required</em>}</span><input name="dueDate" type="date" value={form.dueDate} onChange={updateField} required={isProjectTask} min={isProjectTask ? selectedProject.startDate : undefined} max={isProjectTask ? selectedProject.endDate : undefined} /></label>
          {fixedProject ? (
            <div className="form-field create-task-grid__wide"><span>Project</span><div className="locked-relation"><strong>{fixedProject.name}</strong><small>{formatEnum(fixedProject.currentUserRole)} · {fixedProject.startDate} to {fixedProject.endDate}</small></div></div>
          ) : (
            <label className="form-field create-task-grid__wide"><span>Work context</span><select name="projectId" value={form.projectId} onChange={updateField}><option value="">Independent task</option>{manageableProjects.map((project) => <option key={project.id} value={project.id}>{project.name} · {formatEnum(project.currentUserRole)}</option>)}</select><small className="field-help">Only projects where you are OWNER or MANAGER can receive new tasks.</small></label>
          )}
        </div>
        {projectDatesInvalid && <p className="form-alert form-alert--error" role="alert">Due date must stay within the project schedule: {selectedProject.startDate} to {selectedProject.endDate}.</p>}
      </section>

      {isProjectTask && (
        <>
          <section className="create-task-section">
            <div className="create-task-section__heading">
              <div><p className="eyebrow">2 · Verification</p><h2>Define acceptance criteria</h2></div>
              <span>{criteria.filter((criterion) => criterion.content.trim()).length} ready</span>
            </div>
            <p className="create-task-guidance">Each item should describe an observable result that a reviewer can verify. Avoid vague criteria such as “works well” or “looks good”.</p>
            <div className="criteria-editor">
              {criteria.map((criterion, index) => (
                <div className="criterion-editor-row" key={criterion.key}>
                  <span>{index + 1}</span>
                  <label className="form-field"><span>Criterion {index + 1}</span><input value={criterion.content} required={criteria.length === 1} maxLength="1000" placeholder="Example: Validation errors are shown beside every invalid field" onChange={(event) => updateCriterion(criterion.key, event.target.value)} /></label>
                  <button className="text-button action-button--delete" type="button" disabled={criteria.length === 1 || isSaving} onClick={() => removeCriterion(criterion.key)}>Remove</button>
                </div>
              ))}
            </div>
            <button className="text-button criterion-add-button" type="button" disabled={isSaving} onClick={() => setCriteria((current) => [...current, EMPTY_CRITERION()])}>+ Add another criterion</button>
            {!hasCriterion && <p className="form-alert form-alert--warning">At least one acceptance criterion is required before creating a Project Task.</p>}
          </section>

          <section className="create-task-section">
            <div className="create-task-section__heading">
              <div><p className="eyebrow">3 · Ownership</p><h2>Choose initial responsibility</h2></div>
              <span>Optional</span>
            </div>
            <p className="create-task-guidance">Leave this open when members should claim the task themselves. Assign it now when responsibility is already agreed.</p>
            {membersError && <p className="form-alert form-alert--error" role="alert">{membersError}</p>}
            <label className="form-field assignee-create-field"><span>Initial assignee</span><select value={assigneeUsername} disabled={isLoadingMembers || Boolean(membersError)} onChange={(event) => setAssigneeUsername(event.target.value)}><option value="">Open for MEMBER claim</option>{members.map((member) => <option key={member.username} value={member.username}>{member.displayName || member.username} · {formatEnum(member.role)}</option>)}</select><small className="field-help">VIEWER accounts are excluded. Assignment is recorded as ASSIGNED.</small></label>
          </section>
        </>
      )}

      <section className="create-task-submit">
        <div><strong>{isProjectTask ? 'Ready for transparent execution?' : 'Ready to create this personal task?'}</strong><p>{isProjectTask ? 'The scope and criteria will be visible to every project member before work begins.' : 'Personal tasks keep the lightweight workflow and do not require acceptance criteria.'}</p></div>
        <div className="create-task-submit__actions"><button className="text-button" type="button" disabled={isSaving} onClick={onCancel}>Cancel</button><button className="primary-button primary-button--fit" type="submit" disabled={isSaving || !projectReady || isLoadingMembers}>{isSaving ? 'Creating workflow...' : 'Create task'}</button></div>
      </section>
    </form>
  )
}

export default TaskCreateForm
