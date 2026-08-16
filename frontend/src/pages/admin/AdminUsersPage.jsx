import { useEffect, useState } from 'react'

import { activateUser, deactivateUser, deleteUserById, getAllUsers } from '../../api/userApi'
import AdminHeader from '../../components/admin/AdminHeader'
import Pagination from '../../components/layout/Pagination'
import getApiErrorMessage from '../../utils/getApiErrorMessage'
import { decrementPageTotal } from '../../utils/pageUtils'

function formatDate(value) {
  if (!value) return 'Not available'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Not available'

  return new Intl.DateTimeFormat('en', { dateStyle: 'medium' }).format(date)
}

function AdminUsersPage() {
  const [users, setUsers] = useState([])
  const [userPage, setUserPage] = useState(null)
  const [pageNumber, setPageNumber] = useState(0)
  const [error, setError] = useState('')
  const [statusMessage, setStatusMessage] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [activeAction, setActiveAction] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadUsers() {
      setError('')
      setIsLoading(true)

      try {
        const data = await getAllUsers({ page: pageNumber, size: 20 }, controller.signal)
        setUsers(data.content)
        setUserPage(data)
      } catch (apiError) {
        if (apiError.code !== 'ERR_CANCELED') {
          setError(getApiErrorMessage(apiError, 'Unable to load users.'))
        }
      } finally {
        if (!controller.signal.aborted) setIsLoading(false)
      }
    }

    loadUsers()

    return () => controller.abort()
  }, [pageNumber, reloadKey])

  async function changeActivation(user, action) {
    const actionKey = `${action}-${user.id}`
    setError('')
    setStatusMessage('')
    setActiveAction(actionKey)

    try {
      const updatedUser = action === 'activate'
        ? await activateUser(user.id)
        : await deactivateUser(user.id)

      setUsers((currentUsers) => currentUsers.map((currentUser) => (
        currentUser.id === updatedUser.id ? updatedUser : currentUser
      )))
      setStatusMessage(`${updatedUser.username} was ${action}d successfully.`)
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, `Unable to ${action} this user.`))
    } finally {
      setActiveAction('')
    }
  }

  async function removeUser(user) {
    if (!window.confirm(`Permanently delete ${user.username}? This action cannot be undone.`)) return

    const actionKey = `delete-${user.id}`
    setError('')
    setStatusMessage('')
    setActiveAction(actionKey)

    try {
      await deleteUserById(user.id)
      setStatusMessage(`${user.username} was deleted successfully.`)
      if (users.length === 1 && pageNumber > 0) {
        setPageNumber((current) => current - 1)
      } else {
        setUsers((current) => current.filter((item) => item.id !== user.id))
        setUserPage(decrementPageTotal)
      }
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to delete this user.'))
    } finally {
      setActiveAction('')
    }
  }

  return (
    <main className="dashboard-shell">
      <AdminHeader />
      <section className="dashboard-content">
        <p className="eyebrow">User management</p>
        <h1>System users.</h1>
        <p className="dashboard-lead">Review accounts, change activation state or permanently remove an account.</p>

        {isLoading && <p className="dashboard-lead">Loading users...</p>}
        {!isLoading && error && users.length === 0 && (
          <>
            <p className="form-alert form-alert--error" role="alert">{error}</p>
            <button className="text-button" type="button" onClick={() => setReloadKey((key) => key + 1)}>Try again</button>
          </>
        )}
        {error && users.length > 0 && <p className="form-alert form-alert--error" role="alert">{error}</p>}
        {statusMessage && <p className="form-alert form-alert--success" role="status">{statusMessage}</p>}

        {!isLoading && users.length === 0 && !error && <p className="dashboard-lead">No users found.</p>}
        {users.length > 0 && (
        <div className="table-scroll">
          <table className="dashboard-table">
          <thead>
            <tr>
              <th>Role</th>
              <th>Name</th>
              <th>Username</th>
              <th>Email</th>
              <th>Status</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr
                key={user.id}
                className={user.deactivated ? 'row--deactivated' : user.role === 'ADMIN' ? 'row--accent' : ''}
              >
                <td>{user.role}</td>
                <td>{user.displayName || user.username}</td>
                <td>@{user.username}</td>
                <td>{user.email}</td>
                <td>
                  <span className={`status-badge ${user.deactivated ? 'status-badge--deactivated' : 'status-badge--active'}`}>
                    {user.deactivated ? 'Deactivated' : 'Active'}
                  </span>
                </td>
                <td>{formatDate(user.createdAt)}</td>
                <td>
                  <button
                    className="text-button action-button--activate"
                    type="button"
                    disabled={Boolean(activeAction) || !user.deactivated}
                    onClick={() => changeActivation(user, 'activate')}
                  >
                    Activate
                  </button>
                  <button
                    className="text-button action-button--deactivate"
                    type="button"
                    disabled={Boolean(activeAction) || user.deactivated}
                    onClick={() => changeActivation(user, 'deactivate')}
                  >
                    Deactivate
                  </button>
                  <button
                    className="text-button action-button--delete"
                    type="button"
                    disabled={Boolean(activeAction)}
                    onClick={() => removeUser(user)}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
          </table>
        </div>
      )}
      {!isLoading && <Pagination page={userPage} label="users" onPageChange={setPageNumber} />}
      </section>
    </main>
  )
}

export default AdminUsersPage
