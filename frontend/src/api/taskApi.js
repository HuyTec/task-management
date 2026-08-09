import apiClient from './apiClient'

export async function getAllTasks(signal) {
  const response = await apiClient.get('/tasks', { signal })
  return response.data.data
}

export async function getTaskById(id, signal) {
  const response = await apiClient.get(`/tasks/${id}`, { signal })
  return response.data.data
}

export async function createTask(taskData, signal) {
  const response = await apiClient.post('/tasks', taskData, { signal })
  return response.data.data
}

export async function updateTaskById(taskId, taskData, signal) {
  const response = await apiClient.patch(`/tasks/${taskId}`, taskData, { signal })
  return response.data.data
}

export async function deleteTaskById(taskId, signal) {
  const response = await apiClient.delete(`/tasks/${taskId}`, { signal })
  return response.data.data
}

export async function getMyTasks(signal) {
  const response = await apiClient.get('/tasks/me', { signal })
  return response.data.data
}
