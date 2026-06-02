export interface Category {
  id: string
  name: string
  description?: string
  documentCount: number
}

export interface AdminUser {
  id: string
  email: string
  fullName?: string
  role: 'USER' | 'ADMIN'
  status: 'PENDING' | 'ACTIVE' | 'DISABLED'
  lastLogin?: string
  createdAt: string
}

export interface DashboardStats {
  totalDocuments: number
  totalChunks: number
  totalUsers: number
  pendingUsers: number
  totalCategories: number
  documentsByCategory: Record<string, number>
  documentsByStatus: Record<string, number>
  recentActivity: { userEmail: string; action: string; target: string; createdAt: string }[]
}

export interface AuditEntry {
  id: string
  userEmail: string
  action: string
  target: string
  details?: string
  createdAt: string
}

export interface QuestionnaireQuestion {
  id: string
  questionKey: string
  questionText: string
  active: boolean
  sortOrder: number
}

export const STORAGE_PROVIDERS = [
  { value: 'LOCAL', label: 'Local File System' },
  { value: 'S3', label: 'AWS S3' },
]
