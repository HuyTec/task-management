import WorkspaceHeader from '../layout/WorkspaceHeader'

function AdminHeader() {
  return (
    <WorkspaceHeader
      mark="AD"
      title="Administration"
      subtitle="System workspace"
      homePath="/admin/dashboard"
      navigation={[
        { icon: 'home', label: 'Overview', to: '/admin/dashboard' },
        { icon: 'users', label: 'Users', to: '/admin/users' },
        { icon: 'tasks', label: 'Tasks', to: '/admin/tasks' },
      ]}
      signOutPath="/admin/login"
    />
  )
}

export default AdminHeader
