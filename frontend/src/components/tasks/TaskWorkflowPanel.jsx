import { useEffect, useMemo, useState } from 'react'

import {
  addTaskCriterion,
  approveTask,
  assignTask,
  claimTask,
  clearTaskAssignee,
  deleteTaskCriterion,
  releaseTaskClaim,
  requestTaskChanges,
  startTask,
  updateTaskCriterion,
} from '../../api/taskApi'
import { formatDateTime, formatEnum } from '../../utils/entityFormatters'
import getApiErrorMessage from '../../utils/getApiErrorMessage'
import SubmissionPanel from './SubmissionPanel'

function AssignmentPanel({ task, membership, members, busyAction, runAction }) {
  const assignment = task.activeAssignment
  const canManage = membership.role === 'OWNER' || membership.role === 'MANAGER'
  const isAssignee = assignment?.assigneeUsername === membership.username
  const assignmentMutable = task.status !== 'IN_REVIEW' && task.status !== 'DONE'
  const canClaim = membership.role === 'MEMBER' && task.status === 'TODO' && !assignment
  const canRelease = isAssignee && assignment?.type === 'CLAIMED' && task.status === 'TODO'
  const canStart = isAssignee && ['TODO', 'CHANGES_REQUESTED'].includes(task.status)
  const candidates = useMemo(
    () => members
      .filter((member) => member.role !== 'VIEWER')
      .sort((first, second) => (first.displayName || first.username).localeCompare(second.displayName || second.username)),
    [members],
  )
  const [selectedUsername, setSelectedUsername] = useState('')

  useEffect(() => {
    setSelectedUsername(assignment?.assigneeUsername || candidates[0]?.username || '')
  }, [assignment?.assigneeUsername, candidates])

  return (
    <section className="workflow-card">
      <div className="workflow-card__heading">
        <div><p className="eyebrow">Responsibility</p><h2>Task assignment</h2></div>
        <span className={`assignment-state ${assignment ? 'assignment-state--active' : ''}`}>{assignment ? 'Assigned' : 'Open'}</span>
      </div>

      {assignment ? (
        <div className="assignment-summary">
          <div className="member-avatar" aria-hidden="true">{(assignment.assigneeDisplayName || assignment.assigneeUsername).charAt(0).toUpperCase()}</div>
          <div>
            <strong>{assignment.assigneeDisplayName || assignment.assigneeUsername}{isAssignee ? ' (You)' : ''}</strong>
            <span>@{assignment.assigneeUsername} · {formatEnum(assignment.type)}</span>
            <small>{assignment.assignedByUsername ? `Assigned by @${assignment.assignedByUsername}` : 'Self-claimed'} · {formatDateTime(assignment.assignedAt)}</small>
          </div>
        </div>
      ) : (
        <p className="workflow-empty">No one is responsible for this task yet.</p>
      )}

      <div className="workflow-actions">
        {canClaim && <button className="primary-button primary-button--fit" type="button" disabled={Boolean(busyAction)} onClick={() => runAction('claim', () => claimTask(task.id), 'Task claimed. You can start when ready.')}>Claim task</button>}
        {canRelease && <button className="text-button action-button--delete" type="button" disabled={Boolean(busyAction)} onClick={() => runAction('release', () => releaseTaskClaim(task.id), 'Claim released.')}>Release claim</button>}
        {canStart && <button className="primary-button primary-button--fit" type="button" disabled={Boolean(busyAction)} onClick={() => runAction('start', () => startTask(task.id), 'Task moved to In progress.')}>{task.status === 'CHANGES_REQUESTED' ? 'Resume work' : 'Start work'}</button>}
      </div>

      {canManage && assignmentMutable && (
        <div className="assignment-manager">
          <label className="form-field">
            <span>{assignment ? 'Reassign to' : 'Assign to'}</span>
            <select value={selectedUsername} onChange={(event) => setSelectedUsername(event.target.value)} disabled={Boolean(busyAction) || candidates.length === 0}>
              {candidates.length === 0 && <option value="">No eligible members</option>}
              {candidates.map((member) => <option key={member.username} value={member.username}>{member.displayName || member.username} · {formatEnum(member.role)}</option>)}
            </select>
          </label>
          <button className="primary-button primary-button--fit" type="button" disabled={Boolean(busyAction) || !selectedUsername || selectedUsername === assignment?.assigneeUsername} onClick={() => runAction('assign', () => assignTask(task.id, selectedUsername), assignment ? 'Task reassigned successfully.' : 'Task assigned successfully.')}>{assignment ? 'Reassign' : 'Assign'}</button>
          {assignment && <button className="text-button action-button--delete" type="button" disabled={Boolean(busyAction)} onClick={() => runAction('clear-assignee', () => clearTaskAssignee(task.id), 'Assignee removed.')}>Remove assignee</button>}
        </div>
      )}
    </section>
  )
}

function CriteriaPanel({ task, membership, busyAction, runAction }) {
  const criteria = task.acceptanceCriteria || []
  const canManage = membership.role === 'OWNER' || membership.role === 'MANAGER'
  const canEditStructure = canManage && task.status === 'TODO'
  const canReview = canManage && task.status === 'IN_REVIEW'
  const [content, setContent] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [editingContent, setEditingContent] = useState('')

  function addCriterion(event) {
    event.preventDefault()
    const normalizedContent = content.trim()
    if (!normalizedContent) return
    const nextPosition = criteria.reduce((highest, criterion) => Math.max(highest, criterion.position), -1) + 1
    runAction(
      'add-criterion',
      () => addTaskCriterion(task.id, { content: normalizedContent, position: nextPosition }),
      'Acceptance criterion added.',
      () => setContent(''),
    )
  }

  function saveCriterion(criterion) {
    const normalizedContent = editingContent.trim()
    if (!normalizedContent || normalizedContent === criterion.content) {
      setEditingId(null)
      return
    }
    runAction(
      `criterion-${criterion.id}`,
      () => updateTaskCriterion(task.id, criterion.id, { content: normalizedContent }),
      'Acceptance criterion updated.',
      () => setEditingId(null),
    )
  }

  return (
    <section className="workflow-card">
      <div className="workflow-card__heading">
        <div><p className="eyebrow">Definition of done</p><h2>Acceptance criteria</h2></div>
        <span className="criteria-progress">{criteria.filter((criterion) => criterion.satisfied).length}/{criteria.length} satisfied</span>
      </div>

      <div className="criteria-list">
        {criteria.map((criterion) => (
          <article className={`criterion-row ${criterion.satisfied ? 'criterion-row--satisfied' : ''}`} key={criterion.id}>
            {canReview ? (
              <input
                aria-label={`Mark ${criterion.content} as satisfied`}
                type="checkbox"
                checked={criterion.satisfied}
                disabled={Boolean(busyAction)}
                onChange={(event) => runAction(`criterion-${criterion.id}`, () => updateTaskCriterion(task.id, criterion.id, { satisfied: event.target.checked }), event.target.checked ? 'Criterion marked as satisfied.' : 'Criterion reopened.')}
              />
            ) : (
              <span className="criterion-indicator" aria-hidden="true">{criterion.satisfied ? '✓' : criterion.position + 1}</span>
            )}

            {editingId === criterion.id ? (
              <input className="criterion-edit-input" value={editingContent} maxLength="1000" autoFocus onChange={(event) => setEditingContent(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') saveCriterion(criterion); if (event.key === 'Escape') setEditingId(null) }} />
            ) : (
              <span className="criterion-content">{criterion.content}</span>
            )}

            {canEditStructure && (
              <div className="criterion-actions">
                {editingId === criterion.id ? (
                  <><button className="text-button" type="button" disabled={Boolean(busyAction)} onClick={() => saveCriterion(criterion)}>Save</button><button className="text-button" type="button" disabled={Boolean(busyAction)} onClick={() => setEditingId(null)}>Cancel</button></>
                ) : (
                  <><button className="text-button" type="button" disabled={Boolean(busyAction)} onClick={() => { setEditingId(criterion.id); setEditingContent(criterion.content) }}>Edit</button><button className="text-button action-button--delete" type="button" disabled={Boolean(busyAction)} onClick={() => { if (window.confirm('Delete this acceptance criterion?')) runAction(`criterion-${criterion.id}`, () => deleteTaskCriterion(task.id, criterion.id), 'Acceptance criterion deleted.') }}>Delete</button></>
                )}
              </div>
            )}
          </article>
        ))}
        {criteria.length === 0 && <p className="workflow-empty">No acceptance criteria have been defined. The task cannot be submitted for review yet.</p>}
      </div>

      {canEditStructure && (
        <form className="criterion-add-form" onSubmit={addCriterion}>
          <label className="form-field"><span>New criterion</span><input value={content} maxLength="1000" placeholder="Describe one observable completion condition" onChange={(event) => setContent(event.target.value)} /></label>
          <button className="primary-button primary-button--fit" type="submit" disabled={Boolean(busyAction) || !content.trim()}>Add criterion</button>
        </form>
      )}
      {!canEditStructure && task.status !== 'TODO' && <p className="workflow-note">Criterion wording is locked after work starts. During review, OWNER/MANAGER can mark each item as satisfied.</p>}
    </section>
  )
}

function ReviewPanel({ task, membership, busyAction, runAction }) {
  const assignment = task.activeAssignment
  const criteria = task.acceptanceCriteria || []
  const reviews = task.reviews || []
  const canManage = membership.role === 'OWNER' || membership.role === 'MANAGER'
  const isAssignee = assignment?.assigneeUsername === membership.username
  const canReview = canManage && task.status === 'IN_REVIEW'
  const allSatisfied = criteria.length > 0 && criteria.every((criterion) => criterion.satisfied)
  const [reason, setReason] = useState('')

  return (
    <section className="workflow-card workflow-card--review" id="review">
      <div className="workflow-card__heading">
        <div><p className="eyebrow">Quality gate</p><h2>Review</h2></div>
        <span className={`detail-status detail-status--${task.status.toLowerCase()}`}>{formatEnum(task.status)}</span>
      </div>

      {isAssignee && ['IN_PROGRESS', 'CHANGES_REQUESTED'].includes(task.status) && <p className="workflow-note">Create a Submission and attach evidence before sending this task to review.</p>}

      {canReview && (
        <div className="review-controls">
          <p className="workflow-note">Review every criterion first. Approval is enabled only when every item is satisfied.</p>
          <button className="primary-button" type="button" disabled={Boolean(busyAction) || !allSatisfied} onClick={() => runAction('approve', () => approveTask(task.id), 'Task approved and completed.')}>Approve task</button>
          <div className="review-divider"><span>or request revisions</span></div>
          <label className="form-field"><span>Change request reason</span><textarea value={reason} maxLength="5000" rows="5" placeholder="Explain what is missing and what result you expect" onChange={(event) => setReason(event.target.value)} /></label>
          <button className="text-button review-request-button" type="button" disabled={Boolean(busyAction) || !reason.trim()} onClick={() => runAction('request-changes', () => requestTaskChanges(task.id, reason.trim()), 'Changes requested and returned to the assignee.', () => setReason(''))}>Request changes</button>
        </div>
      )}

      {!canReview && task.status !== 'DONE' && <p className="workflow-empty">No reviewer action is available for your role at this stage.</p>}

      <div className="review-history">
        <h3>Review history</h3>
        {reviews.map((review) => (
          <article className="review-entry" key={review.id}>
            <span className={`review-decision review-decision--${review.decision.toLowerCase()}`}>{formatEnum(review.decision)}</span>
            <div><strong>@{review.reviewerUsername}</strong><small>{formatDateTime(review.createdAt)}</small>{review.message && <p>{review.message}</p>}</div>
          </article>
        ))}
        {reviews.length === 0 && <p className="workflow-empty">No review decision has been recorded.</p>}
      </div>
    </section>
  )
}

function TaskWorkflowPanel({ task, membership, members, onChanged }) {
  const [busyAction, setBusyAction] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    if (window.location.hash === '#review') {
      const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      window.requestAnimationFrame(() => document.getElementById('review')?.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'start' }))
    }
  }, [])

  async function runAction(actionName, action, successMessage, afterSuccess) {
    setBusyAction(actionName)
    setError('')
    setSuccess('')
    try {
      await action()
      afterSuccess?.()
      setSuccess(successMessage)
      await onChanged()
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to update the task workflow.'))
    } finally {
      setBusyAction('')
    }
  }

  return (
    <section className="task-workflow">
      <div className="section-heading">
        <div><p className="eyebrow">Project workflow</p><h2>Assignment and quality review</h2></div>
        <span>Your role: {formatEnum(membership.role)}</span>
      </div>
      {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
      {success && <p className="form-alert form-alert--success" role="status">{success}</p>}
      <div className="workflow-layout">
        <div className="workflow-main">
          <AssignmentPanel task={task} membership={membership} members={members} busyAction={busyAction} runAction={runAction} />
          <CriteriaPanel task={task} membership={membership} busyAction={busyAction} runAction={runAction} />
          <SubmissionPanel task={task} membership={membership} onChanged={onChanged} />
        </div>
        <ReviewPanel task={task} membership={membership} busyAction={busyAction} runAction={runAction} />
      </div>
    </section>
  )
}

export default TaskWorkflowPanel
