import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { FileText, Trash2, Download, RefreshCw, CloudUpload, Layers, FolderOpen } from 'lucide-react'
import { documentService } from '../services/documentService'
import { categoryService } from '../services/categoryService'
import { STORAGE_PROVIDERS } from '../types/admin'
import type { DocumentDTO } from '../types/document'
import StatusBadge from '../components/upload/StatusBadge'
import Pagination from '../components/common/Pagination'
import { SkeletonRows } from '../components/common/Skeleton'
import UploadProgressModal from '../components/upload/UploadProgressModal'
import { toast } from '../store/toastStore'

const PAGE_SIZE = 10

export default function UploadPage() {
  const qc = useQueryClient()
  const [category, setCategory] = useState('General')
  const [storage, setStorage] = useState('LOCAL')
  const [uploading, setUploading] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [progress, setProgress] = useState<{ id: string; name: string } | null>(null)

  const { data: docs, isLoading } = useQuery(
    ['documents', page],
    () => documentService.list({ page, size: PAGE_SIZE }),
    {
      keepPreviousData: true,
      refetchInterval: (data) =>
        data?.content.some((d) => d.status === 'PROCESSING') ? 3000 : false,
    }
  )

  const { data: categories } = useQuery('categories', categoryService.list)

  const deleteMutation = useMutation(documentService.delete, {
    onSuccess: () => { qc.invalidateQueries('documents'); toast.success('Document deleted') },
    onError: () => toast.error('Could not delete document'),
  })

  const onDrop = useCallback(async (files: File[]) => {
    for (const file of files) {
      setUploading((prev) => [...prev, file.name])
      try {
        const doc = await documentService.upload(file, category, storage)
        qc.invalidateQueries('documents')
        toast.success(`"${file.name}" uploaded — indexing…`)
        setProgress({ id: doc.id, name: doc.name })   // open live progress popup
      } catch (e: any) {
        toast.error(e?.response?.data?.detail || `Upload failed for "${file.name}"`)
      }
      finally { setUploading((prev) => prev.filter((n) => n !== file.name)) }
    }
  }, [category, storage, qc])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'application/pdf': ['.pdf'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
      'application/vnd.openxmlformats-officedocument.presentationml.presentation': ['.pptx'],
    },
    multiple: true,
  })

  const totalDocs = docs?.totalElements ?? 0
  const indexedCount = docs?.content.filter((d) => d.status === 'INDEXED').length ?? 0
  const processingCount = docs?.content.filter((d) => d.status === 'PROCESSING').length ?? 0

  return (
    <div className="page-shell">
      <div className="page-hero">
        <span className="page-kicker">Document Hub</span>
        <div>
          <h2 className="section-title">Documents</h2>
          <p className="section-subtitle mt-3 max-w-3xl">
            Upload, track, and manage the knowledge base that powers your semantic search and cited answers.
          </p>
        </div>
      </div>

      {/* Two-column layout: upload panel (left) + stats (right) — fills horizontal space */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        {/* Upload panel */}
        <div className="lg:col-span-2 card p-6">
          <div className="flex flex-wrap items-center gap-4 mb-4">
            <div className="flex items-center gap-2">
              <label className="text-sm text-slate-400 font-medium shrink-0">Category</label>
              <select value={category} onChange={(e) => setCategory(e.target.value)} className="input-base">
                {(categories ?? []).map((c) => <option key={c.id} value={c.name}>{c.name}</option>)}
              </select>
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm text-slate-400 font-medium shrink-0">Store in</label>
              <select value={storage} onChange={(e) => setStorage(e.target.value)} className="input-base">
                {STORAGE_PROVIDERS.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
              </select>
            </div>
          </div>

          <div {...getRootProps()} className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors ${
            isDragActive ? 'border-accent bg-accent/5' : 'border-slate-600 hover:border-slate-500 hover:bg-slate-800/50'}`}>
            <input {...getInputProps()} />
            <CloudUpload className={`w-9 h-9 mx-auto mb-3 ${isDragActive ? 'text-accent' : 'text-slate-500'}`} />
            <p className="text-slate-300 font-medium">
              {isDragActive ? 'Drop files here…' : 'Drag & drop files, or click to browse'}
            </p>
            <p className="text-slate-500 text-sm mt-1">
              PDF · DOCX · XLSX · PPTX · local uploads up to 100MB
            </p>
            {storage === 'S3' && (
              <p className="text-accent/80 text-xs mt-2">
                Large S3 files upload directly from the browser to S3 using multipart upload.
              </p>
            )}
          </div>

          {uploading.length > 0 && (
            <div className="mt-4 space-y-2">
              {uploading.map((name) => (
                <div key={name} className="flex items-center gap-3 text-sm text-slate-400">
                  <RefreshCw className="w-4 h-4 animate-spin text-accent" />
                  Uploading {name}…
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Stats panel */}
        <div className="grid grid-cols-3 lg:grid-cols-1 gap-4">
          <StatCard icon={FileText} label="Total documents" value={totalDocs}
            color="text-accent" chip="bg-accent/10 border-accent/20" />
          <StatCard icon={Layers} label="Indexed (page)" value={indexedCount}
            color="text-emerald-400" chip="bg-emerald-400/10 border-emerald-400/20" />
          <StatCard icon={FolderOpen} label="Categories" value={categories?.length ?? 0}
            color="text-purple-400" chip="bg-purple-400/10 border-purple-400/20" />
        </div>
      </div>

      {/* Document Table */}
      <div className="table-shell">
        <div className="px-4 py-3 border-b border-slate-700 flex items-center justify-between">
          <span className="text-sm font-medium text-white">
            All Documents {processingCount > 0 && (
              <span className="text-yellow-400 text-xs ml-2">{processingCount} processing…</span>
            )}
          </span>
          <button onClick={() => qc.invalidateQueries('documents')} className="btn-ghost text-xs flex items-center gap-1.5">
            <RefreshCw className="w-3.5 h-3.5" /> Refresh
          </button>
        </div>

        {isLoading ? (
          <SkeletonRows rows={6} cols={6} />
        ) : !docs?.content.length ? (
          <div className="p-12 text-center text-slate-500">
            <FileText className="w-8 h-8 mx-auto mb-2 opacity-50" />
            No documents yet. Upload one above.
          </div>
        ) : (
          <>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-700/80 bg-slate-900/25">
                  {['Name', 'Category', 'Type', 'Size', 'Status', 'Chunks', 'Uploaded', 'Actions'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {docs.content.map((doc: DocumentDTO) => (
                  <tr key={doc.id} className="border-b border-slate-700/50 hover:bg-slate-800/30 transition-colors">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-slate-500 shrink-0" />
                        <span className="text-slate-200 truncate max-w-[280px]">{doc.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="px-2 py-0.5 rounded-full bg-slate-700 text-slate-300 text-xs">{doc.category}</span>
                    </td>
                    <td className="px-4 py-3 text-slate-400">{doc.fileType}</td>
                    <td className="px-4 py-3 text-slate-400">{formatSize(doc.fileSizeBytes)}</td>
                    <td className="px-4 py-3"><StatusBadge status={doc.status} /></td>
                    <td className="px-4 py-3 text-slate-400">{doc.chunkCount ?? '—'}</td>
                    <td className="px-4 py-3 text-slate-500 text-xs whitespace-nowrap">
                      {doc.createdAt ? new Date(doc.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        <button onClick={() => documentService.download(doc.id, doc.name)}
                          className="btn-ghost p-1.5" title="Download">
                          <Download className="w-3.5 h-3.5" />
                        </button>
                        <button onClick={() => deleteMutation.mutate(doc.id)}
                          className="btn-ghost p-1.5 text-red-400 hover:text-red-300" title="Delete">
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <Pagination
              page={page}
              totalPages={docs.totalPages}
              totalElements={docs.totalElements}
              pageSize={PAGE_SIZE}
              onPageChange={setPage}
            />
          </>
        )}
      </div>

      {progress && (
        <UploadProgressModal
          documentId={progress.id}
          fileName={progress.name}
          onClose={() => setProgress(null)}
        />
      )}
    </div>
  )
}

function StatCard({ icon: Icon, label, value, color, chip }: {
  icon: typeof FileText; label: string; value: number; color: string; chip: string
}) {
  return (
    <div className="card card-hover p-4 flex items-center gap-4 lg:flex-col lg:items-start">
      <div className={`icon-chip ${chip}`}>
        <Icon className={`w-5 h-5 ${color}`} />
      </div>
      <div>
        <div className="text-2xl font-bold text-white leading-tight">{value}</div>
        <div className="text-slate-400 text-xs mt-0.5">{label}</div>
      </div>
    </div>
  )
}

function formatSize(bytes?: number): string {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
