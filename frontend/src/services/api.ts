import axios from 'axios'
import { useAuthStore } from '../store/authStore'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let isRefreshing = false
let queue: ((token: string | null) => void)[] = []

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config
    const { refreshToken, clearAuth, setOnboardingCompleted } = useAuthStore.getState()

    // Don't try to refresh the refresh/login calls themselves
    const isAuthCall = original?.url?.includes('/auth/')

    if (err.response?.status === 401 && !original._retry && refreshToken && !isAuthCall) {
      if (isRefreshing) {
        // Queue requests while a refresh is in flight
        return new Promise((resolve, reject) => {
          queue.push((token) => {
            if (token) { original.headers.Authorization = `Bearer ${token}`; resolve(api(original)) }
            else reject(err)
          })
        })
      }

      original._retry = true
      isRefreshing = true
      try {
        const { data } = await axios.post('/api/auth/refresh', { refreshToken })
        useAuthStore.getState().setAuth(data.token, data.refreshToken, {
          email: data.email,
          fullName: data.fullName,
          role: data.role,
          onboardingCompleted: data.onboardingCompleted,
        })
        queue.forEach((cb) => cb(data.token)); queue = []
        original.headers.Authorization = `Bearer ${data.token}`
        return api(original)
      } catch (refreshErr) {
        queue.forEach((cb) => cb(null)); queue = []
        clearAuth()
        window.location.href = '/login'
        return Promise.reject(refreshErr)
      } finally {
        isRefreshing = false
      }
    }

    if (err.response?.status === 401 && isAuthCall) {
      // login/refresh genuinely failed — surface it
      return Promise.reject(err)
    }

    if (err.response?.status === 403 && err.response?.data?.code === 'ONBOARDING_REQUIRED') {
      setOnboardingCompleted(false)
      if (window.location.pathname !== '/onboarding') {
        window.location.href = '/onboarding'
      }
    }

    return Promise.reject(err)
  }
)

export default api
