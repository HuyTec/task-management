import { Link } from 'react-router-dom'

const STATUSES = [['TODO', 'To do'], ['IN_PROGRESS', 'In progress'], ['DONE', 'Done'], ['BLOCKED', 'Blocked']]

function formatDate(value) {
  if (!value) return 'No due date'
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(date)
}

function TaskCard({ task, busy, onDelete, onEdit, onStatusChange }) {
  return (
    <article className={`task-card task-card--${task.priority?.toLowerCase()}`}>
      <div className="task-card__topline"><span className={`priority-badge priority-badge--${task.priority?.toLowerCase()}`}>{task.priority}</span></div>
      <h3>{task.title}</h3>
      <p className="task-card__date">{formatDate(task.dueDate)}</p>
      <label className="compact-field"><span>Move to</span><select value={task.status} disabled={busy} onChange={(event) => onStatusChange(task, event.target.value)}>{STATUSES.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <div className="task-card__actions"><Link className="text-button" to={`/tasks/${task.id}`}>Details</Link><button className="text-button" type="button" disabled={busy} onClick={() => onEdit(task)}>Edit</button><button className="text-button action-button--delete" type="button" disabled={busy} onClick={() => onDelete(task)}>Delete</button></div>
    </article>
  )
}

export default TaskCard
