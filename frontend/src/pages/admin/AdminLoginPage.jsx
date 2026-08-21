import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { login, logout } from '../../api/authApi'
import PasswordField from '../../components/auth/PasswordField'
import AppLogo from '../../components/icons/AppLogo'
import getApiErrorMessage from '../../utils/getApiErrorMessage'

function AdminLoginPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      const data = await login({ username, password })

      if (!data?.accessToken || data.user?.role !== 'ADMIN') {
        try {
          await logout()
        } catch {
          // The local login attempt still ends even if server-side cleanup fails.
        }
        localStorage.removeItem('accessToken')
        navigate('/403', { replace: true })
        return
      }

      localStorage.setItem('accessToken', data.accessToken)
      navigate('/admin/dashboard', { replace: true })
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, 'Unable to sign in to the administration workspace.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-story" aria-labelledby="admin-product-title">
        <AppLogo admin className="brand-mark" />
        <div className="story-copy">
          <p className="eyebrow">Administration workspace</p>
          <h1 id="admin-product-title">Manage clearly.<br />Act carefully.</h1>
          <p className="story-description">Review users, tasks and system capabilities from one protected workspace.</p>
        </div>
        <div className="focus-card" aria-label="Administrator access notice">
          <div className="focus-card__header"><span>Restricted access</span><span>ADMIN</span></div>
          <div className="focus-progress" aria-hidden="true"><span /></div>
          <p>Every protected page verifies your role with the backend.</p>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <div className="auth-heading">
            <p className="eyebrow">Administrator login</p>
            <h2>Enter the control workspace</h2>
            <p>Use the existing login service with an account assigned the ADMIN role.</p>
          </div>
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field-group">
              <label htmlFor="adminUsername">Username</label>
              <input id="adminUsername" name="username" value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" required />
            </div>
            <PasswordField
              id="adminPassword"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
            />
            {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
            <button className="primary-button" type="submit" disabled={isSubmitting}>
              <span>{isSubmitting ? 'Verifying...' : 'Sign in as administrator'}</span>
            </button>
          </form>
        </div>
        <p className="auth-footer">Task Management · Administration</p>
      </section>
    </main>
  )
}

export default AdminLoginPage
