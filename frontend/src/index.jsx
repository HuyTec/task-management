import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'

/**
 * index.js - Entry point của ứng dụng React
 * Đây là file đầu tiên được Vite gọi khi khởi động
 * Nó "gắn" (mount) toàn bộ ứng dụng React vào thẻ <div id="root"> trong public/index.html
 */
const root = ReactDOM.createRoot(document.getElementById('root'))
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
