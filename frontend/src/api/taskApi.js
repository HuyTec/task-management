import apiClient from './apiClient'

export async function getAllTasks(signal) {
  const response = await apiClient.get('/tasks', { signal })
  return response.data.data
}
