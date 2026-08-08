import apiClient from './apiClient'

export async function login(credentials) {
  const response = await apiClient.post('/auth/login', credentials)
  return response.data.data
}

export async function register(account) {
  const response = await apiClient.post('/auth/register', account)
  return response.data.data
}
