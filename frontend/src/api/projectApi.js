import apiClient from './apiClient'

export async function getAllProjects(params = {}, signal) {
  const response = await apiClient.get('/projects', { params, signal })
  return response.data.data
}

export async function getMyProjects(params = {}, signal) {
  const response = await apiClient.get('/projects/me', { params, signal })
  return response.data.data
}

export async function getProjectById(projectId, signal) {
  const response = await apiClient.get(`/projects/${projectId}`, { signal })
  return response.data.data
}

export async function getProjectTasks(projectId, params = {}, signal) {
  const response = await apiClient.get(`/projects/${projectId}/tasks`, { params, signal })
  return response.data.data
}

export async function getProjectMembers(projectId, params = {}, signal) {
  const response = await apiClient.get(`/projects/${projectId}/members`, { params, signal })
  return response.data.data
}

export async function getMyProjectMembership(projectId, signal) {
  const response = await apiClient.get(`/projects/${projectId}/members/me`, { signal })
  return response.data.data
}

export async function addProjectMember(projectId, memberData, signal) {
  const response = await apiClient.post(`/projects/${projectId}/members`, memberData, { signal })
  return response.data.data
}

export async function removeProjectMember(projectId, username, signal) {
  const response = await apiClient.delete(`/projects/${projectId}/members/${encodeURIComponent(username)}`, { signal })
  return response.data.data
}

export async function updateProjectMemberRole(projectId, username, role, signal) {
  const response = await apiClient.patch(
    `/projects/${projectId}/members/${encodeURIComponent(username)}/role`,
    { role },
    { signal },
  )
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
