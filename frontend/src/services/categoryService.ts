import api from './api'
import type { Category } from '../types/admin'

export const categoryService = {
  list: () => api.get<Category[]>('/categories').then((r) => r.data),

  create: (name: string, description: string) =>
    api.post<Category>('/categories', { name, description }).then((r) => r.data),

  update: (id: string, name: string, description: string) =>
    api.put<Category>(`/categories/${id}`, { name, description }).then((r) => r.data),

  delete: (id: string) => api.delete(`/categories/${id}`),
}
