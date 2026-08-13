import WorkspaceHeader from './WorkspaceHeader'

function AppHeader() {
  return (
    <WorkspaceHeader
      mark="HT"
      title="Task Management"
      subtitle="Personal workspace"
      homePath="/dashboard"
      navigation={[
        { label: 'Home', to: '/dashboard' },
        { label: 'Tasks', to: '/tasks' },
        { label: 'Expenses', to: '/expenses' },
        { label: 'Projects', to: '/projects' },
        { label: 'Profile', to: '/profile' },
      ]}
      signOutPath="/login"
    />
  )
}

export default AppHeader
