import apiClient from './apiClient'

export async function getAllProjects(signal) {
  const response = await apiClient.get('/projects', { signal })
  return response.data.data
}

export async function getMyProjects(signal) {
  const response = await apiClient.get('/projects/me', { signal })
  return response.data.data
}

export async function getProjectById(projectId, signal) {
  const response = await apiClient.get(`/projects/${projectId}`, { signal })
  return response.data.data
}

export async function getProjectTasks(projectId, signal) {
  const response = await apiClient.get(`/projects/${projectId}/tasks`, { signal })
  return response.data.data
}

export async function createProject(projectData, signal) {
  const response = await apiClient.post('/projects', projectData, { signal })
  return response.data.data
}

export async function updateProjectById(projectId, projectData, signal) {
  const response = await apiClient.patch(`/projects/${projectId}`, projectData, { signal })
  return response.data.data
}

export async function deleteProjectById(projectId, signal) {
  const response = await apiClient.delete(`/projects/${projectId}`, { signal })
  return response.data.data
}
