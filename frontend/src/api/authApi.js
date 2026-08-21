import apiClient from './apiClient'

export async function login(credentials) {
  const response = await apiClient.post('/auth/login', credentials)
  return response.data.data
}

export async function loginWithGoogle(credential) {
  const response = await apiClient.post('/auth/google', { credential })
  return response.data.data
}

export async function linkGoogleAccount(credential, password) {
  const response = await apiClient.post('/auth/google/link', { credential, password })
  return response.data.data
}

export async function register(account) {
  const response = await apiClient.post('/auth/register', account)
  return response.data.data
}

export async function logout() {
  await apiClient.post('/auth/logout')
}
