import AppShell from '../components/layout/AppShell'
import StatePanel from '../components/ui/StatePanel'

function HistoryPage() {
  return (
    <AppShell>
      <p className="eyebrow">History</p>
      <h1>Workspace activity.</h1>
      <p className="dashboard-lead">A chronological view will live here when the backend activity feed is available.</p>
      <StatePanel
        title="No activity feed yet"
        description="This is an honest product state, not an empty API result. Task and review history remains available inside each task."
      />
    </AppShell>
  )
}

export default HistoryPage
