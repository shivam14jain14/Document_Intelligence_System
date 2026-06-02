import { useQuery } from 'react-query'
import { adminService } from '../../services/adminService'

export default function AdminAudit() {
  const { data } = useQuery('admin-audit', () => adminService.audit(0, 100))

  return (
    <div className="card overflow-hidden">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-slate-700">
            {['Time', 'User', 'Action', 'Target', 'Details'].map((h) => (
              <th key={h} className="px-4 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data?.content.map((a) => (
            <tr key={a.id} className="border-b border-slate-700/50 hover:bg-slate-800/30">
              <td className="px-4 py-2.5 text-slate-500 text-xs whitespace-nowrap">
                {new Date(a.createdAt).toLocaleString()}
              </td>
              <td className="px-4 py-2.5 text-slate-400">{a.userEmail || '—'}</td>
              <td className="px-4 py-2.5">
                <span className="text-accent text-xs font-mono px-1.5 py-0.5 bg-accent/10 rounded">{a.action}</span>
              </td>
              <td className="px-4 py-2.5 text-slate-300 truncate max-w-[200px]">{a.target || '—'}</td>
              <td className="px-4 py-2.5 text-slate-500 text-xs truncate max-w-[200px]">{a.details || '—'}</td>
            </tr>
          ))}
          {data?.content.length === 0 && (
            <tr><td colSpan={5} className="px-4 py-8 text-center text-slate-500">No audit entries yet.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
