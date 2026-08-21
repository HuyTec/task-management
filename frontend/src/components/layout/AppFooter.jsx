import { Link } from 'react-router-dom'

import AppLogo from '../icons/AppLogo'

const FOOTER_VARIANTS = {
  app: {
    title: 'Task Management',
    description: 'A calm workspace for deliberate progress.',
    links: [['Tasks', '/tasks'], ['Expenses', '/expenses'], ['Projects', '/projects'], ['Profile', '/profile']],
    meta: 'Private by default · Focused by design',
  },
  admin: {
    title: 'Administration',
    description: 'Protected tools for careful system stewardship.',
    links: [['Overview', '/admin/dashboard'], ['Users', '/admin/users'], ['Tasks', '/admin/tasks']],
    meta: 'Protected workspace · Role verified',
  },
}

function AppFooter({ variant = 'app' }) {
  const footer = FOOTER_VARIANTS[variant] || FOOTER_VARIANTS.app

  return (
    <footer className="app-footer">
      <div className="app-footer__brand">
        <AppLogo admin={variant === 'admin'} className="app-footer__mark" />
        <div>
          <strong>{footer.title}</strong>
          <span>{footer.description}</span>
        </div>
      </div>

      <nav className="app-footer__links" aria-label="Footer navigation">
        {footer.links.map(([label, to]) => <Link key={to} to={to}>{label}</Link>)}
      </nav>

      <p className="app-footer__meta">{footer.meta}</p>
    </footer>
  )
}

export default AppFooter
