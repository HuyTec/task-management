import { useEffect, useState } from 'react'

import { getMyProfile, updateMyProfile } from '../api/userApi'
import PasswordField from '../components/auth/PasswordField'
import AppHeader from '../components/layout/AppHeader'

function formatDateTime(value) {
  if (!value) return 'Not available'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return 'Not available'

  return new Intl.DateTimeFormat('en', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function ProfilePage() {
  const [profile, setProfile] = useState(null)
  const [displayName, setDisplayName] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadProfile() {
      setError('')
      setSuccessMessage('')
      setIsLoading(true)

      try {
        const data = await getMyProfile(controller.signal)
        setProfile(data)
        setDisplayName(data.displayName || '')
      } catch (apiError) {
        if (apiError.code !== 'ERR_CANCELED') {
          setError(apiError.response?.data?.message || apiError.message || 'Unable to load your profile.')
        }
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false)
        }
      }
    }

    loadProfile()

    return () => controller.abort()
  }, [reloadKey])

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSuccessMessage('')

    const normalizedDisplayName = displayName.trim()
    const update = {}

    if (normalizedDisplayName !== (profile.displayName || '') && normalizedDisplayName.length < 2) {
      setError('Display name must contain at least 2 characters.')
      return
    }

    if (normalizedDisplayName !== (profile.displayName || '')) {
      update.displayName = normalizedDisplayName
    }

    if (newPassword) {
      update.password = newPassword
    }

    if (Object.keys(update).length === 0) {
      setSuccessMessage('No profile changes to save.')
      return
    }

    setIsSubmitting(true)

    try {
      const updatedProfile = await updateMyProfile(update)
      setProfile(updatedProfile)
      setDisplayName(updatedProfile.displayName || '')
      setNewPassword('')
      setSuccessMessage('Profile updated successfully.')
    } catch (apiError) {
      setError(apiError.response?.data?.message || apiError.message || 'Unable to update your profile.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="dashboard-shell">
      <AppHeader />
      <section className="dashboard-content">
        <p className="eyebrow">My profile</p>

        {isLoading && (
          <>
            <h1>Loading profile...</h1>
            <p className="dashboard-lead">We are synchronizing your account details.</p>
          </>
        )}

        {!isLoading && error && !profile && (
          <>
            <h1>Profile unavailable.</h1>
            <p className="form-alert form-alert--error" role="alert">{error}</p>
            <button className="text-button" type="button" onClick={() => setReloadKey((currentKey) => currentKey + 1)}>Try again</button>
          </>
        )}

        {!isLoading && profile && (
          <>
            <h1>{profile.displayName || profile.username}</h1>
            <p className="dashboard-lead">Review your account information and update the personal details supported by the backend.</p>

            <div className="dashboard-grid">
              <article className="metric-card metric-card--accent">
                <span>Account</span>
                <strong>@{profile.username}</strong>
                <p>Role: {profile.role}</p>
              </article>
              <article className="metric-card">
                <span>Contact</span>
                <strong>{profile.email}</strong>
                <p>Display name: {profile.displayName || 'Not set'}</p>
              </article>
              <article className="metric-card">
                <span>Account history</span>
                <strong>{formatDateTime(profile.createdAt)}</strong>
                <p>Last updated {formatDateTime(profile.updatedAt)}</p>
              </article>
            </div>

            <div className="auth-card">
              <form className="auth-form" onSubmit={handleSubmit}>
                <div className="field-group">
                  <label htmlFor="displayName">Display name</label>
                  <input id="displayName" name="displayName" value={displayName} onChange={(event) => setDisplayName(event.target.value)} minLength="2" maxLength="50" />
                </div>
                <div className="field-group">
                  <label htmlFor="username">Username</label>
                  <input id="username" name="username" value={profile.username} readOnly />
                </div>
                <div className="field-group">
                  <label htmlFor="email">Email</label>
                  <input id="email" name="email" type="email" value={profile.email} readOnly />
                </div>
                <PasswordField
                  id="newPassword"
                  label="New password"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  autoComplete="new-password"
                  placeholder="Leave blank to keep your current password"
                  required={false}
                />
                {error && <p className="form-alert form-alert--error" role="alert">{error}</p>}
                {successMessage && <p className="form-alert form-alert--success" role="status">{successMessage}</p>}
                <button className="primary-button" type="submit" disabled={isSubmitting}>
                  <span>{isSubmitting ? 'Saving...' : 'Save changes'}</span>
                </button>
              </form>
            </div>
          </>
        )}
      </section>
    </main>
  )
}

export default ProfilePage
