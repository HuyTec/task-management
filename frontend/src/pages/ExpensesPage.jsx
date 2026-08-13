import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { createExpense, deleteExpenseById, getMyExpenses, unlinkExpenseFromTask, updateExpenseById } from '../api/expenseApi'
import { getMyTasks } from '../api/taskApi'
import ExpenseForm from '../components/expenses/ExpenseForm'
import AppHeader from '../components/layout/AppHeader'
import getApiErrorMessage from '../utils/getApiErrorMessage'

function formatMoney(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0)
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(date)
}

function ExpensesPage() {
  const [expenses, setExpenses] = useState([])
  const [tasks, setTasks] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [busyExpenseId, setBusyExpenseId] = useState(null)
  const [editingExpense, setEditingExpense] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const taskNames = useMemo(() => new Map(tasks.map((task) => [task.id, task.title])), [tasks])
  const total = useMemo(() => expenses.reduce((sum, expense) => sum + Number(expense.amount || 0), 0), [expenses])

  const loadData = useCallback(async (signal) => {
    setIsLoading(true)
    setError('')
    try {
      const [expenseData, taskData] = await Promise.all([getMyExpenses(signal), getMyTasks(signal)])
      setExpenses(expenseData)
      setTasks(taskData)
    } catch (apiError) {
      if (apiError.code !== 'ERR_CANCELED') setError(getApiErrorMessage(apiError, 'Unable to load expense records.'))
    } finally {
      if (!signal?.aborted) setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    loadData(controller.signal)
    return () => controller.abort()
  }, [loadData])

  function openCreate() {
    setEditingExpense(null)
    setShowForm(true)
    setError('')
    setSuccess('')
  }

  async function saveExpense(payload) {
    setIsSaving(true)
    setError('')
    setSuccess('')
    try {
      if (editingExpense) {
        const previousTaskId = editingExpense.taskId
        const { taskId, ...updatePayload } = payload
        if (taskId) updatePayload.taskId = taskId
        await updateExpenseById(editingExpense.id, updatePayload)
        if (previousTaskId && !taskId) await unlinkExpenseFromTask(editingExpense.id)
        setSuccess('Expense updated successfully.')
      } else {
        await createExpense(payload)
        setSuccess('Expense recorded successfully.')
      }
      setShowForm(false)
      setEditingExpense(null)
      await loadData()
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to save the expense.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function removeExpense(expense) {
    if (!window.confirm(`Delete “${expense.description}”? This action cannot be undone.`)) return
    setBusyExpenseId(expense.id)
    setError('')
    try {
      await deleteExpenseById(expense.id)
      setExpenses((current) => current.filter((item) => item.id !== expense.id))
      setSuccess('Expense deleted successfully.')
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to delete the expense.'))
    } finally {
      setBusyExpenseId(null)
    }
  }

  return (
    <main className="dashboard-shell tab-theme tab-theme--expenses">
      <AppHeader />
      <section className="dashboard-content dashboard-content--wide">
        <div className="page-heading">
          <div><p className="eyebrow">Personal finance</p><h1>Your expense ledger.</h1><p className="dashboard-lead">A private view of costs across daily life and active tasks.</p></div>
          <button className="primary-button primary-button--fit" type="button" onClick={openCreate}>Add expense</button>
        </div>
        <div className="summary-strip">
          <div><span>Total recorded</span><strong>{formatMoney(total)}</strong></div>
          <div><span>Entries</span><strong>{expenses.length}</strong></div>
          <div><span>Linked to tasks</span><strong>{expenses.filter((expense) => expense.taskId).length}</strong></div>
        </div>
        {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
        {success && <p className="form-alert form-alert--success" role="status">{success}</p>}
        {showForm && <ExpenseForm initialExpense={editingExpense} tasks={tasks} isSaving={isSaving} onCancel={() => setShowForm(false)} onSubmit={saveExpense} />}
        {isLoading ? <p className="dashboard-lead">Loading your ledger...</p> : (
          <div className="table-scroll entity-table-wrap">
            <table className="dashboard-table expense-table">
              <thead><tr><th>Description</th><th>Category</th><th>Task</th><th>Date</th><th>Amount</th><th><span className="sr-only">Actions</span></th></tr></thead>
              <tbody>
                {expenses.map((expense) => (
                  <tr key={expense.id}>
                    <td><Link className="table-primary-link" to={`/expenses/${expense.id}`}>{expense.description}</Link></td>
                    <td><span className="category-badge">{expense.category?.replace('_', ' ') || 'Uncategorized'}</span></td>
                    <td>{expense.taskId ? taskNames.get(expense.taskId) || 'Linked task' : '—'}</td>
                    <td>{formatDate(expense.expenseDate)}</td>
                    <td className="money-cell">{formatMoney(expense.amount)}</td>
                    <td className="table-actions"><button className="text-button" type="button" disabled={busyExpenseId === expense.id} onClick={() => { setEditingExpense(expense); setShowForm(true); setError(''); setSuccess('') }}>Edit</button><button className="text-button action-button--delete" type="button" disabled={busyExpenseId === expense.id} onClick={() => removeExpense(expense)}>Delete</button></td>
                  </tr>
                ))}
                {expenses.length === 0 && <tr><td className="table-empty" colSpan="6">No expenses yet. Add the first record when a cost occurs.</td></tr>}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  )
}

export default ExpensesPage
