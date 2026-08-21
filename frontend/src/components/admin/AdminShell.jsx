import AppFooter from '../layout/AppFooter'
import AdminHeader from './AdminHeader'

function AdminShell({ children, wide = false }) {
  return (
    <main className="dashboard-shell admin-shell">
      <AdminHeader />
      <section className={`dashboard-content${wide ? ' dashboard-content--wide' : ''}`}>{children}</section>
      <AppFooter variant="admin" />
    </main>
  )
}

export default AdminShell
