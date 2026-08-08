import WorkspaceHeader from '../layout/WorkspaceHeader'

function AdminHeader() {
  return (
    <WorkspaceHeader
      mark="AD"
      title="Administration"
      subtitle="System workspace"
      homePath="/admin/dashboard"
      navigation={[
        { label: 'Overview', to: '/admin/dashboard' },
        { label: 'Users', to: '/admin/users' },
        { label: 'Tasks', to: '/admin/tasks' },
      ]}
      signOutPath="/admin/login"
    />
  )
}

export default AdminHeader
