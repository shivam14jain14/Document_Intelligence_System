import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { Plus, Trash2, FolderOpen } from 'lucide-react'
import { categoryService } from '../../services/categoryService'

export default function AdminCategories() {
  const qc = useQueryClient()
  const { data: categories } = useQuery('categories', categoryService.list)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const invalidate = () => qc.invalidateQueries('categories')

  const createMut = useMutation(
    () => categoryService.create(name, description),
    { onSuccess: () => { invalidate(); setName(''); setDescription('') } }
  )
  const deleteMut = useMutation((id: string) => categoryService.delete(id), { onSuccess: invalidate })

  return (
    <div className="space-y-4">
      <div className="card p-4 flex flex-wrap gap-3 items-end">
        <div className="flex-1 min-w-[160px]">
          <label className="text-xs text-slate-400 block mb-1">Category name</label>
          <input className="input-base w-full" placeholder="e.g. Compliance" value={name}
            onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="flex-[2] min-w-[200px]">
          <label className="text-xs text-slate-400 block mb-1">Description</label>
          <input className="input-base w-full" placeholder="Optional description" value={description}
            onChange={(e) => setDescription(e.target.value)} />
        </div>
        <button onClick={() => name.trim() && createMut.mutate()} disabled={createMut.isLoading}
          className="btn-primary flex items-center gap-2 text-sm">
          <Plus className="w-4 h-4" /> Add
        </button>
      </div>

      <div className="card overflow-hidden divide-y divide-slate-700/50">
        {categories?.map((c) => (
          <div key={c.id} className="flex items-center gap-3 px-4 py-3 hover:bg-slate-800/30">
            <FolderOpen className="w-5 h-5 text-accent shrink-0" />
            <div className="flex-1">
              <div className="text-slate-200 font-medium">{c.name}</div>
              {c.description && <div className="text-slate-500 text-xs">{c.description}</div>}
            </div>
            <span className="text-xs px-2 py-0.5 rounded-full bg-slate-700 text-slate-300">
              {c.documentCount} docs
            </span>
            <button onClick={() => deleteMut.mutate(c.id)}
              className="btn-ghost p-1.5 text-red-400" title="Delete category"
              disabled={c.documentCount > 0}>
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>
      <p className="text-slate-600 text-xs">Categories with documents can't be deleted until their documents are removed.</p>
    </div>
  )
}
