import api from './api'
import type { DocumentDTO, PageResponse, CategoryDTO, MultipartUploadInitResponse } from '../types/document'

const DIRECT_S3_MULTIPART_THRESHOLD_BYTES = 95 * 1024 * 1024

export const documentService = {
  upload: async (file: File, category: string, storageProvider = 'LOCAL') => {
    if (storageProvider === 'S3' && file.size >= DIRECT_S3_MULTIPART_THRESHOLD_BYTES) {
      return documentService.uploadLargeToS3(file, category)
    }

    const form = new FormData()
    form.append('file', file)
    form.append('category', category)
    form.append('storageProvider', storageProvider)
    return api.post<DocumentDTO>('/documents/upload', form).then((r) => r.data)
  },

  list: (params?: { category?: string; status?: string; page?: number; size?: number }) =>
    api.get<PageResponse<DocumentDTO>>('/documents', { params }).then((r) => r.data),

  getById: (id: string) =>
    api.get<DocumentDTO>(`/documents/${id}`).then((r) => r.data),

  delete: (id: string) =>
    api.delete(`/documents/${id}`),

  categories: () =>
    api.get<CategoryDTO[]>('/documents/categories').then((r) => r.data),

  // Download strategy:
  //  1. Ask for a pre-signed URL — for S3 the browser downloads DIRECTLY from storage,
  //     offloading file bytes from our app server. 204 = backend has no presigned URL.
  //  2. Fall back to streaming through our authenticated API (local FS, or if presign fails).
  download: async (id: string, filename: string) => {
    try {
      const res = await api.get(`/documents/${id}/download-url`)
      if (res.status === 200 && res.data?.url) {
        const a = document.createElement('a')
        a.href = res.data.url            // pre-signed S3 URL (self-authenticating, time-limited)
        a.target = '_blank'
        document.body.appendChild(a)
        a.click()
        a.remove()
        return
      }
    } catch {
      /* fall through to streaming */
    }

    // Fallback: stream through the backend with the JWT (local FS, etc.)
    const res = await api.get(`/documents/${id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename || 'document'
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  },

  uploadLargeToS3: async (file: File, category: string) => {
    const init = await api.post<MultipartUploadInitResponse>('/documents/multipart/initiate', {
      filename: file.name,
      category,
      contentType: file.type || 'application/octet-stream',
      fileSizeBytes: file.size,
    }).then((r) => r.data)

    try {
      const completedParts: { partNumber: number; eTag: string }[] = []

      for (const part of init.parts) {
        const start = (part.partNumber - 1) * init.partSizeBytes
        const end = Math.min(start + init.partSizeBytes, file.size)
        const blob = file.slice(start, end)

        const res = await fetch(part.url, {
          method: 'PUT',
          body: blob,
          headers: {
            'Content-Type': file.type || 'application/octet-stream',
          },
        })
        if (!res.ok) {
          throw new Error(`Part ${part.partNumber} upload failed with status ${res.status}`)
        }

        const eTag = res.headers.get('ETag') || res.headers.get('etag')
        if (!eTag) {
          throw new Error('S3 upload succeeded but ETag was not readable. Check S3 bucket CORS ExposeHeaders for ETag.')
        }
        completedParts.push({ partNumber: part.partNumber, eTag })
      }

      return api.post<DocumentDTO>('/documents/multipart/complete', {
        uploadId: init.uploadId,
        key: init.key,
        filename: file.name,
        category,
        contentType: file.type || 'application/octet-stream',
        fileSizeBytes: file.size,
        parts: completedParts,
      }).then((r) => r.data)
    } catch (error) {
      try {
        await api.post('/documents/multipart/abort', { uploadId: init.uploadId, key: init.key })
      } catch {
        // Best-effort cleanup only.
      }
      throw error
    }
  },
}
