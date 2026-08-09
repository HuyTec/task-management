import { useEffect, useState } from 'react'

const EMPTY_TASK = { title: '', description: '', priority: 'MEDIUM', status: 'TODO', dueDate: '' }
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE', 'BLOCKED']

function TaskForm({ initialTask, isSaving, onCancel, onSubmit }) {
  const [form, setForm] = useState(EMPTY_TASK)

  useEffect(() => {
    setForm(initialTask ? {
      title: initialTask.title ?? '', description: initialTask.description ?? '',
      priority: initialTask.priority ?? 'MEDIUM', status: initialTask.status ?? 'TODO',
      dueDate: initialTask.dueDate ?? '',
    } : EMPTY_TASK)
  }, [initialTask])

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    const payload = {
      title: form.title.trim(), description: form.description.trim() || null,
      priority: form.priority, dueDate: form.dueDate || null,
    }
    if (initialTask) payload.status = form.status
    onSubmit(payload)
  }

  return (
    <form className="entity-form" onSubmit={handleSubmit}>
      <div className="entity-form__heading">
        <div><p className="eyebrow">{initialTask ? 'Edit task' : 'New task'}</p><h2>{initialTask ? 'Refine the work item.' : 'Plan the next outcome.'}</h2></div>
        <button className="text-button" type="button" onClick={onCancel}>Close</button>
      </div>
      <label className="form-field"><span>Title</span><input name="title" value={form.title} onChange={updateField} required maxLength="160" autoFocus /></label>
      <label className="form-field entity-form__wide"><span>Description</span><textarea name="description" value={form.description} onChange={updateField} rows="4" maxLength="2000" /></label>
      <label className="form-field"><span>Priority</span><select name="priority" value={form.priority} onChange={updateField}>{PRIORITIES.map((value) => <option key={value}>{value}</option>)}</select></label>
      {initialTask && <label className="form-field"><span>Status</span><select name="status" value={form.status} onChange={updateField}>{STATUSES.map((value) => <option key={value} value={value}>{value.replace('_', ' ')}</option>)}</select></label>}
      <label className="form-field"><span>Due date</span><input name="dueDate" type="date" value={form.dueDate} onChange={updateField} /></label>
      <div className="entity-form__actions"><button className="primary-button" type="submit" disabled={isSaving}>{isSaving ? 'Saving...' : initialTask ? 'Save changes' : 'Create task'}</button></div>
    </form>
  )
}

export default TaskForm
