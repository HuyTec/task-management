import StatusPage from '../components/layout/StatusPage'

function ForbiddenPage() {
  return (
    <StatusPage
      code="403"
      eyebrow="Access restricted"
      title="This workspace is not available to your account."
      description="You are signed in, but your account does not have permission to open this page. Return to your workspace or ask an administrator to review your access."
    />
  )
}

export default ForbiddenPage
