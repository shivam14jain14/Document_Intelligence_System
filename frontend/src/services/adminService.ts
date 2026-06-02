import api from './api'
import type { AdminUser, DashboardStats, AuditEntry, QuestionnaireQuestion } from '../types/admin'

interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number }

export const adminService = {
  stats: () => api.get<DashboardStats>('/admin/stats').then((r) => r.data),

  listUsers: () => api.get<AdminUser[]>('/admin/users').then((r) => r.data),

  createUser: (email: string, password: string, fullName: string, role: string) =>
    api.post<AdminUser>('/admin/users', { email, password, fullName, role }).then((r) => r.data),

  approve: (id: string) => api.post<AdminUser>(`/admin/users/${id}/approve`).then((r) => r.data),

  setStatus: (id: string, status: string) =>
    api.put<AdminUser>(`/admin/users/${id}/status`, { status }).then((r) => r.data),

  setRole: (id: string, role: string) =>
    api.put<AdminUser>(`/admin/users/${id}/role`, { role }).then((r) => r.data),

  audit: (page = 0, size = 50) =>
    api.get<Page<AuditEntry>>('/admin/audit', { params: { page, size } }).then((r) => r.data),

  listQuestionnaireQuestions: () =>
    api.get<QuestionnaireQuestion[]>('/admin/questionnaire-questions').then((r) => r.data),

  createQuestionnaireQuestion: (payload: Omit<QuestionnaireQuestion, 'id'>) =>
    api.post<QuestionnaireQuestion>('/admin/questionnaire-questions', payload).then((r) => r.data),

  updateQuestionnaireQuestion: (id: string, payload: Omit<QuestionnaireQuestion, 'id'>) =>
    api.put<QuestionnaireQuestion>(`/admin/questionnaire-questions/${id}`, payload).then((r) => r.data),

  setQuestionnaireQuestionActive: (id: string, active: boolean) =>
    api.put<QuestionnaireQuestion>(`/admin/questionnaire-questions/${id}/active`, { active }).then((r) => r.data),

  deleteQuestionnaireQuestion: (id: string) =>
    api.delete(`/admin/questionnaire-questions/${id}`),
}
