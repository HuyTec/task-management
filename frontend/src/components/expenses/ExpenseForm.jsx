import { useEffect, useState } from 'react'

const CATEGORIES = ['FOOD', 'TRANSPORTATION', 'LEARNING', 'HOBBIES', 'OTHERS']
const EMPTY_EXPENSE = { description: '', amount: '', category: 'OTHERS', expenseDate: '', taskId: '' }

function ExpenseForm({ initialExpense, tasks = [], fixedTask, isSaving, onCancel, onSubmit }) {
  const [form, setForm] = useState(EMPTY_EXPENSE)

  useEffect(() => {
    setForm(initialExpense ? {
      description: initialExpense.description ?? '', amount: initialExpense.amount ?? '',
      category: initialExpense.category ?? 'OTHERS', expenseDate: initialExpense.expenseDate ?? '',
      taskId: initialExpense.taskId ?? '',
    } : EMPTY_EXPENSE)
  }, [initialExpense])

  function updateField(event) {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  function handleSubmit(event) {
    event.preventDefault()
    onSubmit({
      description: form.description.trim(), amount: Number(form.amount), category: form.category,
      expenseDate: form.expenseDate,
      taskId: fixedTask?.id ?? (form.taskId ? Number(form.taskId) : null),
    })
  }

  return (
    <form className="entity-form" onSubmit={handleSubmit}>
      <div className="entity-form__heading">
        <div><p className="eyebrow">{initialExpense ? 'Edit expense' : 'New expense'}</p><h2>{initialExpense ? 'Correct this record.' : 'Record a new cost.'}</h2></div>
        <button className="text-button" type="button" onClick={onCancel}>Close</button>
      </div>
      <label className="form-field entity-form__wide"><span>Description</span><input name="description" value={form.description} onChange={updateField} required maxLength="255" autoFocus /></label>
      <label className="form-field"><span>Amount (VND)</span><input name="amount" type="number" min="1" step="1" value={form.amount} onChange={updateField} required /></label>
      <label className="form-field"><span>Category</span><select name="category" value={form.category} onChange={updateField}>{CATEGORIES.map((value) => <option key={value}>{value.replace('_', ' ')}</option>)}</select></label>
      <label className="form-field"><span>Expense date</span><input name="expenseDate" type="date" value={form.expenseDate} onChange={updateField} required /></label>
      {fixedTask ? (
        <div className="form-field"><span>Related task</span><div className="locked-relation"><strong>{fixedTask.title}</strong><small>Automatically linked to this task</small></div></div>
      ) : (
        <label className="form-field"><span>Related task</span><select name="taskId" value={form.taskId} onChange={updateField}><option value="">No task</option>{tasks.map((task) => <option key={task.id} value={task.id}>{task.title}</option>)}</select></label>
      )}
      <div className="entity-form__actions"><button className="primary-button" type="submit" disabled={isSaving}>{isSaving ? 'Saving...' : initialExpense ? 'Save changes' : 'Add expense'}</button></div>
    </form>
  )
}

export default ExpenseForm
