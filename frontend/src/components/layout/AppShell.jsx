import AppHeader from './AppHeader'
import AppFooter from './AppFooter'

function AppShell({ children, theme = '', wide = false }) {
  const shellClassName = `dashboard-shell${theme ? ` tab-theme tab-theme--${theme}` : ''}`
  const contentClassName = `dashboard-content${wide ? ' dashboard-content--wide' : ''}`

  return (
    <main className={shellClassName}>
      <AppHeader />
      <section className={contentClassName}>{children}</section>
      <AppFooter />
    </main>
  )
}

export default AppShell
