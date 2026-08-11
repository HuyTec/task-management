import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'

import { getMyProfile } from '../../api/userApi'

function AdminGuard({ children }) {
  const [status, setStatus] = useState('checking')
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    const accessToken = localStorage.getItem('accessToken')

    if (!accessToken) {
      setStatus('unauthenticated')
      return () => controller.abort()
    }

    async function verifyAdmin() {
      try {
        const profile = await getMyProfile(controller.signal)
        setStatus(profile.role === 'ADMIN' ? 'allowed' : 'forbidden')
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          if (error.response?.status === 403) {
            setStatus('forbidden')
          } else if (error.response?.status === 401) {
            localStorage.removeItem('accessToken')
            setStatus('unauthenticated')
          } else {
            setStatus('error')
          }
        }
      }
    }

    verifyAdmin()

    return () => controller.abort()
  }, [retryKey])

  if (status === 'checking') {
    return (
      <main className="dashboard-shell">
        <section className="dashboard-content">
          <p className="eyebrow">Administrator</p>
          <h1>Verifying access...</h1>
        </section>
      </main>
    )
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/admin/login" replace />
  }

  if (status === 'forbidden') {
    return <Navigate to="/403" replace />
  }

  if (status === 'error') {
    return (
      <main className="dashboard-shell">
        <section className="dashboard-content detail-error">
          <p className="eyebrow">Administrator</p>
          <h1>Access verification is unavailable.</h1>
          <p className="dashboard-lead">We could not verify your role. Check the connection and try again.</p>
          <button className="text-button" type="button" onClick={() => {
            setStatus('checking')
            setRetryKey((key) => key + 1)
          }}>Try again</button>
        </section>
      </main>
    )
  }

  return children
}

export default AdminGuard
