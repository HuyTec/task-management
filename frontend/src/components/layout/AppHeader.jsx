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
        { label: 'Profile', to: '/profile' },
      ]}
      signOutPath="/login"
    />
  )
}

export default AppHeader
