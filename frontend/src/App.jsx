import { Navigate, Route, Routes } from 'react-router-dom'

import DashboardPage from './pages/DashboardPage'
import LoginPage from './pages/LoginPage'

function RequireAuth({ children }) {
  const accessToken = localStorage.getItem('accessToken')
  return accessToken ? children : <Navigate to="/login" replace />
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            <DashboardPage />
          </RequireAuth>
        }
      />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}

export default App
