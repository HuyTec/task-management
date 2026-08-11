import StatusPage from '../components/layout/StatusPage'

function NotFoundPage() {
  return (
    <StatusPage
      code="404"
      eyebrow="Page not found"
      title="This page seems to have moved on."
      description="The address may be incorrect, or the page may no longer exist. Return to a familiar place and continue from there."
    />
  )
}

export default NotFoundPage
