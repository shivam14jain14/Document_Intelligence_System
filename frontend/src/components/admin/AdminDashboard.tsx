import { useQuery } from 'react-query'
import { FileText, Layers, Users, Clock, FolderCog } from 'lucide-react'
import { adminService } from '../../services/adminService'

export default function AdminDashboard() {
  const { data, isLoading } = useQuery('admin-stats', adminService.stats)

  if (isLoading || !data) return <div className="text-slate-500">Loading stats…</div>

  const cards = [
    { label: 'Documents', value: data.totalDocuments, icon: FileText, color: 'text-accent' },
    { label: 'Chunks Indexed', value: data.totalChunks, icon: Layers, color: 'text-emerald-400' },
    { label: 'Users', value: data.totalUsers, icon: Users, color: 'text-purple-400' },
    { label: 'Pending Approval', value: data.pendingUsers, icon: Clock, color: 'text-yellow-400' },
    { label: 'Categories', value: data.totalCategories, icon: FolderCog, color: 'text-blue-400' },
  ]

  return (
    <div className="space-y-6">
      {/* Stat cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        {cards.map((c) => {
          const Icon = c.icon
          return (
            <div key={c.label} className="card p-4">
              <Icon className={`w-5 h-5 mb-2 ${c.color}`} />
              <div className="text-2xl font-bold text-white">{c.value}</div>
              <div className="text-slate-400 text-xs mt-0.5">{c.label}</div>
            </div>
          )
        })}
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        {/* Docs by category */}
        <div className="card p-5">
          <h3 className="text-sm font-semibold text-white mb-3">Documents by Category</h3>
          <div className="space-y-2">
            {Object.entries(data.documentsByCategory).length === 0 && (
              <p className="text-slate-500 text-sm">No documents yet.</p>
            )}
            {Object.entries(data.documentsByCategory).map(([cat, count]) => {
              const max = Math.max(...Object.values(data.documentsByCategory), 1)
              return (
                <div key={cat} className="flex items-center gap-3">
                  <span className="text-slate-300 text-sm w-28 truncate">{cat}</span>
                  <div className="flex-1 bg-slate-800 rounded-full h-2 overflow-hidden">
                    <div className="bg-accent h-full rounded-full" style={{ width: `${(count / max) * 100}%` }} />
                  </div>
                  <span className="text-slate-400 text-xs w-6 text-right">{count}</span>
                </div>
              )
            })}
          </div>
        </div>

        {/* Recent activity */}
        <div className="card p-5">
          <h3 className="text-sm font-semibold text-white mb-3">Recent Activity</h3>
          <div className="space-y-2">
            {data.recentActivity.length === 0 && <p className="text-slate-500 text-sm">No activity yet.</p>}
            {data.recentActivity.map((a, i) => (
              <div key={i} className="flex items-center gap-2 text-sm">
                <span className="text-accent text-xs font-mono px-1.5 py-0.5 bg-accent/10 rounded">{a.action}</span>
                <span className="text-slate-400 truncate flex-1">{a.target || '—'}</span>
                <span className="text-slate-600 text-xs">{a.userEmail?.split('@')[0]}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
