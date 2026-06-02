import { useQuery } from 'react-query'
import { CheckCircle, Loader2, XCircle, AlertTriangle, FileUp } from 'lucide-react'
import { documentService } from '../../services/documentService'

interface Props {
  documentId: string
  fileName: string
  onClose: () => void
}

// Ordered pipeline steps shown in the popup.
const STEPS = [
  { key: 'UPLOADED', label: 'Uploaded to storage' },
  { key: 'PARSING', label: 'Parsing & extracting text' },
  { key: 'CHUNKING', label: 'Chunking content' },
  { key: 'EMBEDDING', label: 'Generating embeddings' },
  { key: 'INDEXED', label: 'Indexed & searchable' },
] as const

// Rank used to decide which steps are done/active.
const RANK: Record<string, number> = {
  UPLOADED: 0, PARSING: 1, CHUNKING: 2, EMBEDDING: 3, INDEXED: 4,
}

export default function UploadProgressModal({ documentId, fileName, onClose }: Props) {
  const { data: doc } = useQuery(
    ['doc-progress', documentId],
    () => documentService.getById(documentId),
    {
      refetchInterval: (d) =>
        d && (d.status === 'INDEXED' || d.status === 'FAILED' || d.status === 'NEEDS_OCR') ? false : 800,
    }
  )

  const status = doc?.status ?? 'PROCESSING'
  const failed = status === 'FAILED' || status === 'NEEDS_OCR'
  const done = status === 'INDEXED'

  // Current rank: INDEXED wins; else map the processingStage; else just UPLOADED.
  const current =
    status === 'INDEXED' ? RANK.INDEXED
    : doc?.processingStage && RANK[doc.processingStage] != null ? RANK[doc.processingStage]
    : RANK.UPLOADED

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fade-in">
      <div className="card w-full max-w-md p-6">
        <div className="flex items-center gap-3 mb-5">
          <div className="icon-chip bg-accent/10 border-accent/20">
            <FileUp className="w-5 h-5 text-accent" />
          </div>
          <div className="min-w-0">
            <h3 className="font-semibold text-white truncate">{fileName}</h3>
            <p className="text-slate-400 text-xs">
              {done ? 'Processing complete' : failed ? 'Processing failed' : 'Processing document…'}
            </p>
          </div>
        </div>

        {/* Step checklist */}
        <div className="space-y-1">
          {STEPS.map((step) => {
            const r = RANK[step.key]
            const isDone = failed ? r < current : r < current || (done && r <= current)
            const isActive = !failed && !done && r === current
            const isFailedHere = failed && r === current

            return (
              <div key={step.key} className="flex items-center gap-3 py-2">
                {isFailedHere ? (
                  status === 'NEEDS_OCR'
                    ? <AlertTriangle className="w-5 h-5 text-orange-400 shrink-0" />
                    : <XCircle className="w-5 h-5 text-red-400 shrink-0" />
                ) : isDone ? (
                  <CheckCircle className="w-5 h-5 text-emerald-400 shrink-0" />
                ) : isActive ? (
                  <Loader2 className="w-5 h-5 text-accent shrink-0 animate-spin" />
                ) : (
                  <div className="w-5 h-5 rounded-full border-2 border-slate-600 shrink-0" />
                )}
                <span className={`text-sm ${
                  isDone ? 'text-slate-300'
                  : isActive ? 'text-white font-medium'
                  : isFailedHere ? 'text-red-300'
                  : 'text-slate-500'}`}>
                  {step.label}
                </span>
              </div>
            )
          })}
        </div>

        {/* Footer */}
        <div className="mt-5">
          {failed && (
            <p className="text-sm text-red-300 bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2 mb-3">
              {doc?.errorMessage || (status === 'NEEDS_OCR'
                ? 'This document has no extractable text (likely a scanned image). OCR is required.'
                : 'Something went wrong while processing this document.')}
            </p>
          )}
          {done && (
            <p className="text-sm text-emerald-300 bg-emerald-500/10 border border-emerald-500/20 rounded-lg px-3 py-2 mb-3">
              Indexed {doc?.chunkCount ? `· ${doc.chunkCount} chunks` : ''} — ready to query in Chat.
            </p>
          )}
          <button onClick={onClose}
            className={done || failed ? 'btn-primary w-full' : 'btn-ghost w-full'}>
            {done || failed ? 'Done' : 'Run in background'}
          </button>
        </div>
      </div>
    </div>
  )
}
