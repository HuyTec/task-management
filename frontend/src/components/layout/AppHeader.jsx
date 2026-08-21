import WorkspaceHeader from './WorkspaceHeader'

function AppHeader() {
  return (
    <WorkspaceHeader
      mark="HT"
      title="Task Management"
      subtitle="Personal workspace"
      homePath="/dashboard"
      navigation={[
        { icon: 'home', label: 'Home', to: '/dashboard', end: true },
        { icon: 'tasks', label: 'Tasks', to: '/tasks' },
        { icon: 'expenses', label: 'Expenses', to: '/expenses' },
        { icon: 'projects', label: 'Projects', to: '/projects' },
        { icon: 'history', label: 'History', to: '/history', end: true },
        { icon: 'profile', label: 'Profile', to: '/profile' },
      ]}
      signOutPath="/login"
    />
  )
}

export default AppHeader
