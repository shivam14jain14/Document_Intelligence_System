import { useState } from 'react'
import { useQuery } from 'react-query'
import { FolderOpen, FileText, Download, ChevronDown, ChevronRight } from 'lucide-react'
import { categoryService } from '../services/categoryService'
import { documentService } from '../services/documentService'
import StatusBadge from '../components/upload/StatusBadge'

export default function BrowsePage() {
  const { data: categories } = useQuery('categories', categoryService.list)
  const [open, setOpen] = useState<string | null>(null)

  return (
    <div className="page-shell">
      <div className="page-hero">
        <span className="page-kicker">Catalog</span>
        <div>
          <h2 className="section-title">Browse by Category</h2>
          <p className="section-subtitle mt-3 max-w-3xl">
            Explore the indexed library by business area and jump straight to the source files you need.
          </p>
        </div>
      </div>

      <div className="space-y-3">
        {(categories ?? []).map((cat) => (
          <div key={cat.id} className="card overflow-hidden">
            <button
              onClick={() => setOpen(open === cat.name ? null : cat.name)}
              className="w-full flex items-center gap-3 px-4 py-3.5 hover:bg-slate-800/40 transition-colors">
              {open === cat.name
                ? <ChevronDown className="w-4 h-4 text-slate-400" />
                : <ChevronRight className="w-4 h-4 text-slate-400" />}
              <div className="icon-chip bg-accent/10 border-accent/20 w-8 h-8">
                <FolderOpen className="w-4 h-4 text-accent" />
              </div>
              <span className="font-medium text-white">{cat.name}</span>
              <span className="text-xs px-2 py-0.5 rounded-full bg-slate-700 text-slate-300">
                {cat.documentCount} {cat.documentCount === 1 ? 'doc' : 'docs'}
              </span>
              {cat.description && (
                <span className="text-slate-500 text-xs ml-2 truncate hidden sm:inline">{cat.description}</span>
              )}
            </button>
            {open === cat.name && <CategoryDocs category={cat.name} />}
          </div>
        ))}
        {categories?.length === 0 && (
          <p className="text-slate-500 text-center py-8">No categories defined yet.</p>
        )}
      </div>
    </div>
  )
}

function CategoryDocs({ category }: { category: string }) {
  const { data, isLoading } = useQuery(['documents', category], () =>
    documentService.list({ category, size: 100 }))

  if (isLoading) return <div className="px-4 py-3 text-slate-500 text-sm">Loading…</div>
  if (!data?.content.length) return <div className="px-4 py-3 text-slate-500 text-sm">No documents in this category.</div>

  return (
    <div className="border-t border-slate-700 divide-y divide-slate-700/50">
      {data.content.map((doc) => (
        <div key={doc.id} className="flex items-center gap-3 px-4 py-2.5 hover:bg-slate-800/30">
          <FileText className="w-4 h-4 text-slate-500 shrink-0" />
          <span className="text-slate-200 text-sm flex-1 truncate">{doc.name}</span>
          <span className="text-slate-500 text-xs">{doc.fileType}</span>
          <StatusBadge status={doc.status} />
          <button onClick={() => documentService.download(doc.id, doc.name)}
            className="btn-ghost p-1.5" title="Download">
            <Download className="w-4 h-4" />
          </button>
        </div>
      ))}
    </div>
  )
}
