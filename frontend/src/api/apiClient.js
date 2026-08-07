import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10_000,
  withCredentials: true,
})

export default apiClient
