import { NavLink, useNavigate } from 'react-router-dom'

import { logout } from '../../api/authApi'

function WorkspaceHeader({ mark, title, subtitle, homePath, navigation, signOutPath }) {
  const navigate = useNavigate()

  async function handleSignOut() {
    try {
      await logout()
    } catch {
      // Local sign-out still completes when the server is temporarily unavailable.
    } finally {
      localStorage.removeItem('accessToken')
      navigate(signOutPath, { replace: true })
    }
  }

  return (
    <header className="dashboard-header">
      <NavLink className="workspace-brand" to={homePath} aria-label={`${title} home`}>
        <span className="brand-mark brand-mark--small" aria-hidden="true">{mark}</span>
        <span className="workspace-brand__copy">
          <strong>{title}</strong>
          <span>{subtitle}</span>
        </span>
      </NavLink>

      <nav className="workspace-nav" aria-label={`${title} navigation`}>
        {navigation.map((item) => (
          <NavLink
            className={({ isActive }) => `nav-link${isActive ? ' nav-link--active' : ''}`}
            end
            key={item.to}
            to={item.to}
          >
            {item.label}
          </NavLink>
        ))}
        <button className="nav-link nav-link--danger" type="button" onClick={handleSignOut}>Sign out</button>
      </nav>
    </header>
  )
}

export default WorkspaceHeader
