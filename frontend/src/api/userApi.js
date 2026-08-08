import apiClient from './apiClient'

export async function getMyProfile(signal) {
  const response = await apiClient.get('/users/me', { signal })
  return response.data.data
}

export async function updateMyProfile(profile, signal) {
  const response = await apiClient.patch('/users/me', profile, { signal })
  return response.data.data
}

export async function getAllUsers(signal) {
  const response = await apiClient.get('/users', { signal })
  return response.data.data
}

export async function getUserById(id, signal) {
  const response = await apiClient.get(`/users/${id}`, { signal })
  return response.data.data
}

export async function activateUser(id) {
  const response = await apiClient.patch(`/users/${id}/activate`)
  return response.data.data
}

export async function deactivateUser(id) {
  const response = await apiClient.patch(`/users/${id}/deactivate`)
  return response.data.data
}

export async function updateUserById(userId, userData, signal) {
  const response = await apiClient.patch(`/users/${userId}`, userData, { signal })
  return response.data.data
}

export async function deleteUserById(userId, signal) {
  const response = await apiClient.delete(`/users/${userId}`, { signal })
  return response.data.data
}
