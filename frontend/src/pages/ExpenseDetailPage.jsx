import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { getExpenseById } from '../api/expenseApi'
import { getTaskById } from '../api/taskApi'
import AppHeader from '../components/layout/AppHeader'
import { formatDate, formatDateTime, formatEnum, formatMoney } from '../utils/entityFormatters'
import getApiErrorMessage from '../utils/getApiErrorMessage'

function ExpenseDetailPage() {
  const { expenseId } = useParams()
  const [expense, setExpense] = useState(null)
  const [linkedTask, setLinkedTask] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadExpense() {
      setIsLoading(true)
      setError('')
      setLinkedTask(null)
      try {
        const data = await getExpenseById(expenseId, controller.signal)
        setExpense(data)
        if (data.taskId) {
          try {
            setLinkedTask(await getTaskById(data.taskId, controller.signal))
          } catch (taskError) {
            if (taskError.code === 'ERR_CANCELED') return
          }
        }
      } catch (apiError) {
        if (apiError.code !== 'ERR_CANCELED') setError(getApiErrorMessage(apiError, 'Unable to load this expense.'))
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    loadExpense()
    return () => controller.abort()
  }, [expenseId, reloadKey])

  return (
    <main className="dashboard-shell">
      <AppHeader />
      <section className="dashboard-content">
        <Link className="back-link" to="/expenses">← Back to expense ledger</Link>
        {isLoading && <p className="dashboard-lead">Loading expense details...</p>}
        {!isLoading && error && (
          <div className="detail-error"><h1>Expense unavailable.</h1><p className="form-alert form-alert--error" role="alert">{error}</p><button className="text-button" type="button" onClick={() => setReloadKey((key) => key + 1)}>Try again</button></div>
        )}
        {!isLoading && expense && (
          <>
            <div className="detail-hero">
              <div><p className="eyebrow">Private expense</p><h1>{expense.description}</h1><p className="dashboard-lead">This record is returned through your authenticated account and is not exposed in administration.</p></div>
              <strong className="detail-amount">{formatMoney(expense.amount)}</strong>
            </div>
            <dl className="detail-grid detail-grid--three">
              <div><dt>Category</dt><dd>{formatEnum(expense.category, 'Uncategorized')}</dd></div>
              <div><dt>Expense date</dt><dd>{formatDate(expense.expenseDate)}</dd></div>
              <div><dt>Recorded at</dt><dd>{formatDateTime(expense.createdAt)}</dd></div>
            </dl>
            <section className="linked-task-panel">
              <p className="eyebrow">Related task</p>
              {linkedTask ? <><h2>{linkedTask.title}</h2><p>{linkedTask.description || 'No task description.'}</p><Link className="text-button inline-action" to={`/tasks/${linkedTask.id}`}>View task details</Link></> : <><h2>{expense.taskId ? 'Linked task unavailable' : 'No linked task'}</h2><p>{expense.taskId ? 'The relation exists, but its task details could not be loaded.' : 'This expense currently stands on its own.'}</p></>}
            </section>
          </>
        )}
      </section>
    </main>
  )
}

export default ExpenseDetailPage
