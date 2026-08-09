import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { createExpense } from '../api/expenseApi'
import { getTaskById } from '../api/taskApi'
import ExpenseForm from '../components/expenses/ExpenseForm'
import AppHeader from '../components/layout/AppHeader'
import { formatDate, formatDateTime, formatEnum, formatMoney } from '../utils/entityFormatters'
import getApiErrorMessage from '../utils/getApiErrorMessage'

function TaskDetailPage() {
  const { taskId } = useParams()
  const [task, setTask] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showExpenseForm, setShowExpenseForm] = useState(false)
  const [isSavingExpense, setIsSavingExpense] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadTask() {
      setIsLoading(true)
      setError('')
      try {
        setTask(await getTaskById(taskId, controller.signal))
      } catch (apiError) {
        if (apiError.code !== 'ERR_CANCELED') setError(getApiErrorMessage(apiError, 'Unable to load this task.'))
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    loadTask()
    return () => controller.abort()
  }, [taskId, reloadKey])

  async function addExpense(payload) {
    setIsSavingExpense(true)
    setError('')
    setSuccess('')
    try {
      await createExpense(payload)
      setShowExpenseForm(false)
      setSuccess('Expense added and linked to this task successfully.')
      setReloadKey((key) => key + 1)
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to add the expense to this task.'))
    } finally {
      setIsSavingExpense(false)
    }
  }

  return (
    <main className="dashboard-shell">
      <AppHeader />
      <section className="dashboard-content">
        <Link className="back-link" to="/tasks">← Back to task board</Link>
        {isLoading && <p className="dashboard-lead">Loading task details...</p>}
        {!isLoading && error && (
          <div className="detail-error"><h1>Task unavailable.</h1><p className="form-alert form-alert--error" role="alert">{error}</p><button className="text-button" type="button" onClick={() => setReloadKey((key) => key + 1)}>Try again</button></div>
        )}
        {!isLoading && task && (
          <>
            <div className="detail-hero">
              <div><p className="eyebrow">Task details</p><h1>{task.title}</h1><p className="dashboard-lead">{task.description || 'No description has been added to this task.'}</p></div>
              <div className="detail-badges"><span className={`priority-badge priority-badge--${task.priority?.toLowerCase()}`}>{formatEnum(task.priority)}</span><span className={`detail-status detail-status--${task.status?.toLowerCase()}`}>{formatEnum(task.status)}</span></div>
            </div>

            {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
            {success && <p className="form-alert form-alert--success" role="status">{success}</p>}

            <dl className="detail-grid">
              <div><dt>Due date</dt><dd>{formatDate(task.dueDate)}</dd></div>
              <div><dt>Total expense</dt><dd>{formatMoney(task.total)}</dd></div>
              <div><dt>Created</dt><dd>{formatDateTime(task.createdAt)}</dd></div>
              <div><dt>Last updated</dt><dd>{formatDateTime(task.updatedAt)}</dd></div>
            </dl>

            <section className="detail-section">
              <div className="section-heading">
                <div><p className="eyebrow">Linked records</p><h2>Expenses for this task</h2></div>
                <div className="section-heading__actions"><span>{task.expenses?.length || 0} entries</span><button className="primary-button primary-button--fit" type="button" onClick={() => { setShowExpenseForm(true); setError(''); setSuccess('') }}>Add expense</button></div>
              </div>
              {showExpenseForm && <ExpenseForm fixedTask={task} isSaving={isSavingExpense} onCancel={() => setShowExpenseForm(false)} onSubmit={addExpense} />}
              <div className="table-scroll entity-table-wrap">
                <table className="dashboard-table expense-table">
                  <thead><tr><th>Description</th><th>Category</th><th>Date</th><th>Amount</th></tr></thead>
                  <tbody>
                    {task.expenses?.map((expense) => (
                      <tr key={expense.id}><td><Link className="table-primary-link" to={`/expenses/${expense.id}`}>{expense.description}</Link></td><td><span className="category-badge">{formatEnum(expense.category, 'Uncategorized')}</span></td><td>{formatDate(expense.expenseDate)}</td><td className="money-cell">{formatMoney(expense.amount)}</td></tr>
                    ))}
                    {!task.expenses?.length && <tr><td className="table-empty" colSpan="4">No expenses are linked to this task.</td></tr>}
                  </tbody>
                </table>
              </div>
            </section>
          </>
        )}
      </section>
    </main>
  )
}

export default TaskDetailPage
