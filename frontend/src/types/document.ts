export interface DocumentDTO {
  id: string
  name: string
  category: string
  source: string
  fileType: string
  fileSizeBytes: number
  status: 'PROCESSING' | 'INDEXED' | 'FAILED' | 'NEEDS_OCR'
  processingStage?: 'PARSING' | 'CHUNKING' | 'EMBEDDING' | 'DONE' | null
  errorMessage?: string
  pageCount?: number
  chunkCount?: number
  createdAt: string
}

export interface CategoryDTO {
  name: string
  documentCount: number
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface MultipartUploadInitResponse {
  uploadId: string
  key: string
  partSizeBytes: number
  parts: { partNumber: number; url: string }[]
}

export const CATEGORIES = ['Legal', 'HR', 'Finance', 'Security', 'Engineering', 'General', 'Other']
