import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10_000,
  withCredentials: true,
})

let refreshPromise = null

apiClient.interceptors.request.use((config) => {
  const accessToken = localStorage.getItem('accessToken')

  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,

  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status

    const isAuthRequest = originalRequest?.url?.startsWith('/auth/')

    if (
      status !== 401 ||
      !originalRequest ||
      originalRequest._retry ||
      isAuthRequest
    ) {
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      if (!refreshPromise) {
        refreshPromise = axios
          .post('/api/auth/refresh', null, {
            timeout: 10_000,
            withCredentials: true,
          })
          .then((response) => {
            const newAccessToken = response.data.data?.accessToken

            if (!newAccessToken) {
              throw new Error('Access token was not returned')
            }

            localStorage.setItem('accessToken', newAccessToken)

            return newAccessToken
          })
          .finally(() => {
            refreshPromise = null
          })
      }

      const newAccessToken = await refreshPromise

      originalRequest.headers.Authorization =
        `Bearer ${newAccessToken}`

      return apiClient(originalRequest)
    } catch (refreshError) {
      localStorage.removeItem('accessToken')

      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }

      return Promise.reject(refreshError)
    }
  },
)

export default apiClient