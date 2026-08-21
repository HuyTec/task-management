import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { register } from '../api/authApi'
import PasswordField from '../components/auth/PasswordField'
import AppLogo from '../components/icons/AppLogo'
import Alert from '../components/ui/Alert'

function RegisterPage() {
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      const data = await register({ displayName, username, email, password })

      if (!data?.accessToken) {
        throw new Error('Access token was not returned by the server')
      }

      localStorage.setItem('accessToken', data.accessToken)
      navigate('/dashboard')
    } catch (apiError) {
      setError(apiError.response?.data?.message || apiError.message || 'Unable to create your account. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-story" aria-labelledby="product-title">
        <AppLogo className="brand-mark" />
        <div className="story-copy">
          <p className="eyebrow">Build a steady system</p>
          <h1 id="product-title">Start clearly.<br />Grow consistently.</h1>
          <p className="story-description">Create one workspace for the tasks you want to finish and the expenses you want to understand.</p>
        </div>
        <div className="focus-card" aria-label="Workspace overview preview">
          <div className="focus-card__header"><span>Your workspace</span><span>Ready</span></div>
          <div className="focus-progress" aria-hidden="true"><span /></div>
          <p>A thoughtful place for every plan and expense.</p>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <div className="auth-heading">
            <p className="eyebrow">Create your account</p>
            <h2>Begin your workspace</h2>
            <p>Use a unique username and a password with at least eight characters.</p>
          </div>
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field-group">
              <label htmlFor="displayName">Display name</label>
              <input id="displayName" name="displayName" value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="Enter your display name" autoComplete="name" />
            </div>
            <div className="field-group">
              <label htmlFor="username">Username</label>
              <input id="username" name="username" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="Letters, numbers, and underscores" autoComplete="username" pattern="[a-zA-Z0-9_]+" required />
            </div>
            <div className="field-group">
              <label htmlFor="email">Email</label>
              <input id="email" name="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="Enter your email" autoComplete="email" required />
            </div>
            <PasswordField
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="new-password"
              description="Use at least eight characters. A longer passphrase is easier to remember and safer."
            />
            {error && <Alert tone="error">{error}</Alert>}
            <button className="primary-button" type="submit" disabled={isSubmitting}>
              <span>{isSubmitting ? 'Creating account…' : 'Create account'}</span>
            </button>
          </form>
          <p className="auth-note">
            Already have an account?{' '}
            <button className="text-button" type="button" onClick={() => navigate('/login')}>
              Sign in
            </button>
          </p>
        </div>
        <p className="auth-footer">Task & Expense Management - Built for steady progress</p>
      </section>
    </main>
  )
}

export default RegisterPage
