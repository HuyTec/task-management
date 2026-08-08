import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'

import { getMyProfile } from '../../api/userApi'

function AdminGuard({ children }) {
  const [status, setStatus] = useState('checking')

  useEffect(() => {
    const controller = new AbortController()
    const accessToken = localStorage.getItem('accessToken')

    if (!accessToken) {
      setStatus('denied')
      return () => controller.abort()
    }

    async function verifyAdmin() {
      try {
        const profile = await getMyProfile(controller.signal)
        setStatus(profile.role === 'ADMIN' ? 'allowed' : 'denied')

        if (profile.role !== 'ADMIN') {
          localStorage.removeItem('accessToken')
        }
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          localStorage.removeItem('accessToken')
          setStatus('denied')
        }
      }
    }

    verifyAdmin()

    return () => controller.abort()
  }, [])

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

  if (status === 'denied') {
    return <Navigate to="/admin/login" replace />
  }

  return children
}

export default AdminGuard
