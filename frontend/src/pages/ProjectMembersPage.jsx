import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { addProjectMember, getMyProjectMembership, getProjectMembers, removeProjectMember, updateProjectMemberRole } from '../api/projectApi'
import AppHeader from '../components/layout/AppHeader'
import Pagination from '../components/layout/Pagination'
import { formatDateTime, formatEnum } from '../utils/entityFormatters'
import getApiErrorMessage from '../utils/getApiErrorMessage'
import { decrementPageTotal } from '../utils/pageUtils'

function ProjectMembersPage() {
  const { projectId } = useParams()
  const [myMembership, setMyMembership] = useState(null)
  const [members, setMembers] = useState([])
  const [memberPage, setMemberPage] = useState(null)
  const [query, setQuery] = useState({ page: 0, size: 20, search: '' })
  const [searchInput, setSearchInput] = useState('')
  const [username, setUsername] = useState('')
  const [role, setRole] = useState('MEMBER')
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [removingUsername, setRemovingUsername] = useState('')
  const [updatingUsername, setUpdatingUsername] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const loadMembers = useCallback(async (signal) => {
    setIsLoading(true)
    setError('')
    try {
      const params = { page: query.page, size: query.size, search: query.search || undefined }
      const [membershipData, memberData] = await Promise.all([
        getMyProjectMembership(projectId, signal),
        getProjectMembers(projectId, params, signal),
      ])
      setMyMembership(membershipData)
      setMembers(memberData.content)
      setMemberPage(memberData)
    } catch (apiError) {
      if (apiError.code !== 'ERR_CANCELED') {
        setError(getApiErrorMessage(apiError, 'Unable to load project members.'))
      }
    } finally {
      if (!signal?.aborted) setIsLoading(false)
    }
  }, [projectId, query])

  useEffect(() => {
    const controller = new AbortController()
    loadMembers(controller.signal)
    return () => controller.abort()
  }, [loadMembers])

  function submitSearch(event) {
    event.preventDefault()
    setQuery((current) => ({ ...current, page: 0, search: searchInput.trim() }))
  }

  async function handleAddMember(event) {
    event.preventDefault()
    setIsSaving(true)
    setError('')
    setSuccess('')
    try {
      const member = await addProjectMember(projectId, { username: username.trim(), role })
      setUsername('')
      setRole('MEMBER')
      setSuccess(`${member.displayName || member.username} added to the project.`)
      setQuery((current) => ({ ...current }))
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to add this project member.'))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleRemoveMember(member) {
    if (!window.confirm(`Remove ${member.displayName || member.username} from this project?`)) return
    setRemovingUsername(member.username)
    setError('')
    setSuccess('')
    try {
      await removeProjectMember(projectId, member.username)
      setSuccess(`${member.displayName || member.username} removed from the project.`)
      if (members.length === 1 && query.page > 0) {
        setQuery((current) => ({ ...current, page: current.page - 1 }))
      } else {
        setMembers((current) => current.filter((item) => item.username !== member.username))
        setMemberPage(decrementPageTotal)
      }
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to remove this project member.'))
    } finally {
      setRemovingUsername('')
    }
  }

  async function handleRoleChange(member, nextRole) {
    if (nextRole === member.role) return
    const warning = nextRole === 'MANAGER'
      ? `${member.displayName || member.username} will be able to add, update and remove regular project members. Continue?`
      : `Change ${member.displayName || member.username}'s role to ${formatEnum(nextRole)}?`
    if (!window.confirm(warning)) return

    setUpdatingUsername(member.username)
    setError('')
    setSuccess('')
    try {
      const updatedMember = await updateProjectMemberRole(projectId, member.username, nextRole)
      setMembers((current) => current.map((item) => item.username === updatedMember.username ? updatedMember : item))
      setSuccess(`${updatedMember.displayName || updatedMember.username} is now ${formatEnum(updatedMember.role)}.`)
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to update this project role.'))
    } finally {
      setUpdatingUsername('')
    }
  }

  const canManage = myMembership?.role === 'OWNER' || myMembership?.role === 'MANAGER'
  const isOwner = myMembership?.role === 'OWNER'

  function canManageMember(member) {
    if (!canManage || member.role === 'OWNER') return false
    return !(myMembership.role === 'MANAGER' && member.role === 'MANAGER')
  }

  return (
    <main className="dashboard-shell tab-theme tab-theme--projects">
      <AppHeader />
      <section className="dashboard-content">
        <Link className="back-link" to={`/projects/${projectId}`}>Back to project tasks</Link>
        <div className="page-heading">
          <div>
            <p className="eyebrow">Project team</p>
            <h1>Project members.</h1>
            <p className="dashboard-lead">Find existing members, review project roles and invite another account.</p>
          </div>
        </div>

        {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
        {success && <p className="form-alert form-alert--success" role="status">{success}</p>}

        {!isLoading && myMembership && (
          <div className="member-role-guide" role="note">
            <strong>Your role: {formatEnum(myMembership.role)}</strong>
            <span>Owner controls managers and project deletion. Managers manage members and viewers. Members and viewers have read-only access to this team list.</span>
          </div>
        )}

        {!isLoading && (canManage ? (
          <form className="member-add-form" onSubmit={handleAddMember}>
            <label className="form-field"><span>Username</span><input value={username} onChange={(event) => setUsername(event.target.value)} placeholder="Enter an existing username" required maxLength="255" /></label>
            <label className="form-field"><span>Role</span><select value={role} onChange={(event) => setRole(event.target.value)}>{isOwner && <option value="MANAGER">Manager</option>}<option value="MEMBER">Member</option><option value="VIEWER">Viewer</option></select></label>
            <button className="primary-button primary-button--fit" type="submit" disabled={isSaving}>{isSaving ? 'Adding...' : 'Add member'}</button>
          </form>
        ) : (
          <p className="form-alert form-alert--warning" role="status">You can view the project team. Only the project owner or an authorized manager can manage members.</p>
        ))}

        <form className="list-toolbar member-search" onSubmit={submitSearch}>
          <label className="form-field list-toolbar__search"><span>Search members</span><input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Username, display name or email" maxLength="100" /></label>
          <button className="primary-button primary-button--fit" type="submit">Search</button>
          {(query.search || searchInput) && <button className="text-button" type="button" onClick={() => { setSearchInput(''); setQuery((current) => ({ ...current, page: 0, search: '' })) }}>Clear</button>}
        </form>

        {isLoading ? <p className="dashboard-lead">Loading project members...</p> : (
          <>
            <div className="section-heading"><h2>{memberPage?.totalElements || 0} matching member{memberPage?.totalElements === 1 ? '' : 's'}</h2></div>
            <div className="member-list">
              {members.map((member) => (
                <article className="member-row" key={member.username}>
                  <div className="member-avatar" aria-hidden="true">{(member.displayName || member.username).charAt(0).toUpperCase()}</div>
                  <div className="member-identity"><strong>{member.displayName || member.username}{member.username === myMembership?.username ? ' (You)' : ''}</strong><span>@{member.username} · {member.email}</span></div>
                  <div className="member-meta">
                    {canManageMember(member) ? (
                      <select className="member-role-select" value={member.role} disabled={Boolean(updatingUsername)} onChange={(event) => handleRoleChange(member, event.target.value)}>
                        {isOwner && <option value="MANAGER">Manager</option>}
                        <option value="MEMBER">Member</option>
                        <option value="VIEWER">Viewer</option>
                      </select>
                    ) : (
                      <span className={`status-badge ${member.role === 'OWNER' ? 'status-badge--active' : ''}`}>{formatEnum(member.role)}</span>
                    )}
                    <small>Joined {formatDateTime(member.joinedAt)}</small>
                  </div>
                  {canManageMember(member) && <button className="text-button action-button--delete" type="button" disabled={Boolean(removingUsername) || Boolean(updatingUsername)} onClick={() => handleRemoveMember(member)}>{removingUsername === member.username ? 'Removing...' : 'Remove'}</button>}
                </article>
              ))}
              {members.length === 0 && <p className="table-empty">No members match this search.</p>}
            </div>
            <Pagination page={memberPage} label="members" onPageChange={(page) => setQuery((current) => ({ ...current, page }))} />
          </>
        )}
      </section>
    </main>
  )
}

export default ProjectMembersPage
