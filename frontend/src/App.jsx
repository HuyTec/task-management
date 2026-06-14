import React from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'

/**
 * App.js - Component gốc của ứng dụng React
 * Đây là nơi cấu hình routing (điều hướng giữa các trang)
 */
function App() {
  return (
    <Router>
      <div className="app">
        {/* Routes sẽ được bổ sung khi tạo các Page component */}
        <Routes>
          <Route path="/" element={<div><h1>Task Management App</h1><p>🚀 Frontend đang chạy!</p></div>} />
        </Routes>
      </div>
    </Router>
  )
}

export default App
