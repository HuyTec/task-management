import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { login } from '../api/authApi'

function LoginPage() {
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
      if (!data?.accessToken) throw new Error('Access token was not returned by the server')
      localStorage.setItem('accessToken', data.accessToken)
      navigate('/dashboard')
    } catch (apiError) {
      setError(apiError.response?.data?.message || apiError.message || 'Unable to sign in. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-story" aria-labelledby="product-title">
        <div className="brand-mark" aria-hidden="true">HT</div>
        <div className="story-copy">
          <p className="eyebrow">A calmer way to make progress</p>
          <h1 id="product-title">Plan clearly.<br />Spend mindfully.</h1>
          <p className="story-description">Keep tasks and everyday expenses together in one focused workspace, built to help you finish what matters.</p>
        </div>
        <div className="focus-card" aria-label="Today overview preview">
          <div className="focus-card__header"><span>Today&apos;s focus</span><span>3 tasks</span></div>
          <div className="focus-progress" aria-hidden="true"><span /></div>
          <p>Two priorities completed. One thoughtful step left.</p>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <div className="auth-heading">
            <p className="eyebrow">Welcome back</p>
            <h2>Sign in to your workspace</h2>
            <p>Continue managing your tasks and personal expenses.</p>
          </div>
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field-group">
              <label htmlFor="username">Username</label>
              <input id="username" name="username" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="Enter your username" autoComplete="username" required />
            </div>
            <div className="field-group">
              <div className="field-label-row"><label htmlFor="password">Password</label><span>Minimum 8 characters</span></div>
              <input id="password" name="password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Enter your password" autoComplete="current-password" minLength="8" required />
            </div>
            {error && <p className="form-error" role="alert">{error}</p>}
            <button className="primary-button" type="submit" disabled={isSubmitting}>
              <span>{isSubmitting ? 'Signing in…' : 'Sign in'}</span><span aria-hidden="true">?</span>
            </button>
          </form>
          <p className="auth-note">New here? <span>Registration will be available next.</span></p>
        </div>
        <p className="auth-footer">Task Management · Built for steady progress</p>
      </section>
    </main>
  )
}

export default LoginPage
