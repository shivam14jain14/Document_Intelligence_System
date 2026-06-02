import api from './api'

export const authService = {
  login: (email: string, password: string) =>
    api.post('/auth/login', { email, password }).then((r) => r.data),

  register: (email: string, password: string, fullName: string) =>
    api.post('/auth/register', { email, password, fullName }).then((r) => r.data),

  logoutAll: () => api.post('/auth/logout-all'),
}
