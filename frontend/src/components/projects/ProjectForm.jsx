import { useEffect, useState } from 'react'

const EMPTY_PROJECT = {
  name: '',
  description: '',
  startDate: '',
  endDate: '',
}

function ProjectForm({ initialProject, isSaving, onCancel, onSubmit }) {
  const [form, setForm] = useState(EMPTY_PROJECT)
  const [dateError, setDateError] = useState('')

  useEffect(() => {
    setForm(initialProject ? {
      name: initialProject.name ?? '',
      description: initialProject.description ?? '',
      startDate: initialProject.startDate ?? '',
      endDate: initialProject.endDate ?? '',
    } : EMPTY_PROJECT)
    setDateError('')
  }, [initialProject])

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    if (name === 'startDate' || name === 'endDate') setDateError('')
  }

  function handleSubmit(event) {
    event.preventDefault()
    if (form.startDate > form.endDate) {
      setDateError('Start date must be on or before end date.')
      return
    }

    onSubmit({
      name: form.name.trim(),
      description: form.description.trim() || null,
      startDate: form.startDate,
      endDate: form.endDate,
    })
  }

  return (
    <form className="entity-form" onSubmit={handleSubmit}>
      <div className="entity-form__heading">
        <div>
          <p className="eyebrow">{initialProject ? 'Edit project' : 'New project'}</p>
          <h2>{initialProject ? 'Refine the project plan.' : 'Define a focused outcome.'}</h2>
        </div>
        <button className="text-button" type="button" onClick={onCancel}>Close</button>
      </div>

      <label className="form-field entity-form__wide">
        <span>Name</span>
        <input
          name="name"
          value={form.name}
          onChange={updateField}
          required
          maxLength="100"
          autoFocus
        />
      </label>

      <label className="form-field entity-form__wide">
        <span>Description</span>
        <textarea
          name="description"
          value={form.description}
          onChange={updateField}
          rows="4"
          maxLength="255"
        />
      </label>

      <label className="form-field">
        <span>Start date</span>
        <input
          name="startDate"
          type="date"
          value={form.startDate}
          max={form.endDate || undefined}
          onChange={updateField}
          required
        />
      </label>

      <label className="form-field">
        <span>End date</span>
        <input
          name="endDate"
          type="date"
          value={form.endDate}
          min={form.startDate || undefined}
          onChange={updateField}
          aria-describedby={dateError ? 'project-date-error' : undefined}
          required
        />
      </label>

      {dateError && (
        <p className="form-alert form-alert--error entity-form__wide" id="project-date-error" role="alert">
          {dateError}
        </p>
      )}

      <div className="entity-form__actions">
        <button className="primary-button" type="submit" disabled={isSaving}>
          {isSaving ? 'Saving...' : initialProject ? 'Save changes' : 'Create project'}
        </button>
      </div>
    </form>
  )
}

export default ProjectForm
