import { ChevronLeft, ChevronRight } from 'lucide-react'

interface Props {
  page: number          // 0-based current page
  totalPages: number
  totalElements: number
  pageSize: number
  onPageChange: (page: number) => void
}

export default function Pagination({ page, totalPages, totalElements, pageSize, onPageChange }: Props) {
  if (totalElements === 0) return null

  const from = page * pageSize + 1
  const to = Math.min((page + 1) * pageSize, totalElements)

  // Build a compact page-number window (e.g. 1 … 4 5 [6] 7 8 … 20)
  const pages: (number | '…')[] = []
  const window = 1
  for (let i = 0; i < totalPages; i++) {
    if (i === 0 || i === totalPages - 1 || (i >= page - window && i <= page + window)) {
      pages.push(i)
    } else if (pages[pages.length - 1] !== '…') {
      pages.push('…')
    }
  }

  return (
    <div className="flex items-center justify-between px-4 py-3 border-t border-slate-700 text-sm">
      <span className="text-slate-500">
        Showing <span className="text-slate-300">{from}–{to}</span> of{' '}
        <span className="text-slate-300">{totalElements}</span>
      </span>

      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className="btn-ghost p-1.5 disabled:opacity-30 disabled:cursor-not-allowed"
          aria-label="Previous page">
          <ChevronLeft className="w-4 h-4" />
        </button>

        {pages.map((p, i) =>
          p === '…' ? (
            <span key={`e${i}`} className="px-2 text-slate-600">…</span>
          ) : (
            <button
              key={p}
              onClick={() => onPageChange(p)}
              className={`min-w-[32px] h-8 rounded-md text-sm font-medium transition-colors ${
                p === page
                  ? 'bg-accent text-white'
                  : 'text-slate-400 hover:text-white hover:bg-slate-700'
              }`}>
              {p + 1}
            </button>
          )
        )}

        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className="btn-ghost p-1.5 disabled:opacity-30 disabled:cursor-not-allowed"
          aria-label="Next page">
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  )
}
