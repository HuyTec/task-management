import AdminHeader from '../../components/admin/AdminHeader'

function AdminExpensesPage() {
  return (
    <main className="dashboard-shell">
      <AdminHeader />
      <section className="dashboard-content">
        <p className="eyebrow">Privacy boundary</p>
        <h1>Expense details stay private.</h1>
        <p className="dashboard-lead">Personal descriptions, categories and amounts are visible only to their owner. Administration does not load an all-user expense list.</p>
        <p className="form-alert form-alert--warning" role="status">This page intentionally exposes no expense records.</p>
      </section>
    </main>
  )
}

export default AdminExpensesPage
