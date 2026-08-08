import AdminHeader from '../../components/admin/AdminHeader'

function AdminExpensesPage() {
  return (
    <main className="dashboard-shell">
      <AdminHeader />
      <section className="dashboard-content">
        <p className="eyebrow">Expense management</p>
        <h1>System expenses.</h1>
        <p className="dashboard-lead">The management table is prepared for the ExpenseResponse contract. Data loading remains disabled until the backend exposes an administrator-wide expense endpoint.</p>
        <p className="form-alert form-alert--warning" role="status">Expense administration is not available yet because the backend does not expose GET /api/expenses.</p>
        <div className="table-scroll">
          <table className="dashboard-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>User</th>
                <th>Task</th>
                <th>Description</th>
                <th>Category</th>
                <th>Amount</th>
                <th>Expense date</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="table-empty" colSpan="7">Admin expense API is not available yet. Required endpoint: GET /api/expenses.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  )
}

export default AdminExpensesPage
