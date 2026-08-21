import { useEffect, useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'

import { logout } from '../../api/authApi'
import AppLogo from '../icons/AppLogo'
import NavigationIcon from '../icons/NavigationIcon'

function WorkspaceHeader({ mark, title, subtitle, homePath, navigation, signOutPath }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [isNavigationOpen, setIsNavigationOpen] = useState(false)

  useEffect(() => {
    setIsNavigationOpen(false)
  }, [location.pathname])

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
        <AppLogo admin={mark !== 'HT'} className="brand-mark brand-mark--small" />
        <span className="workspace-brand__copy">
          <strong>{title}</strong>
          <span>{subtitle}</span>
        </span>
      </NavLink>

      <button
        className="workspace-menu-button"
        type="button"
        aria-controls="workspace-navigation"
        aria-expanded={isNavigationOpen}
        onClick={() => setIsNavigationOpen((current) => !current)}
      >
        <span aria-hidden="true">{isNavigationOpen ? '×' : '☰'}</span>
        <span>{isNavigationOpen ? 'Close' : 'Menu'}</span>
      </button>

      <nav
        className={`workspace-nav${isNavigationOpen ? ' workspace-nav--open' : ''}`}
        id="workspace-navigation"
        aria-label={`${title} navigation`}
      >
        {navigation.map((item) => (
          <NavLink
            className={({ isActive }) => `nav-link${isActive ? ' nav-link--active' : ''}`}
            end={item.end}
            key={item.to}
            to={item.to}
          >
            <NavigationIcon name={item.icon} />
            {item.label}
          </NavLink>
        ))}
        <button className="nav-link nav-link--danger" type="button" onClick={handleSignOut}>
          <NavigationIcon name="signout" />
          Sign out
        </button>
      </nav>
    </header>
  )
}

export default WorkspaceHeader
