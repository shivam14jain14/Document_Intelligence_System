import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { UserPlus, Check, Ban, RotateCcw, Shield, User as UserIcon } from 'lucide-react'
import { adminService } from '../../services/adminService'
import type { AdminUser } from '../../types/admin'

export default function AdminUsers() {
  const qc = useQueryClient()
  const { data: users } = useQuery('admin-users', adminService.listUsers)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ email: '', password: '', fullName: '', role: 'USER' })

  const invalidate = () => { qc.invalidateQueries('admin-users'); qc.invalidateQueries('admin-stats') }

  const createMut = useMutation(
    () => adminService.createUser(form.email, form.password, form.fullName, form.role),
    { onSuccess: () => { invalidate(); setShowForm(false); setForm({ email: '', password: '', fullName: '', role: 'USER' }) } }
  )
  const approveMut = useMutation((id: string) => adminService.approve(id), { onSuccess: invalidate })
  const statusMut = useMutation(
    ({ id, status }: { id: string; status: string }) => adminService.setStatus(id, status),
    { onSuccess: invalidate })

  const statusBadge = (s: string) => {
    const map: Record<string, string> = {
      ACTIVE: 'text-emerald-400 bg-emerald-400/10',
      PENDING: 'text-yellow-400 bg-yellow-400/10',
      DISABLED: 'text-red-400 bg-red-400/10',
    }
    return <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${map[s]}`}>{s}</span>
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <span className="text-slate-400 text-sm">{users?.length ?? 0} users</span>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary flex items-center gap-2 text-sm">
          <UserPlus className="w-4 h-4" /> Create User
        </button>
      </div>

      {showForm && (
        <div className="card p-4 grid grid-cols-1 md:grid-cols-4 gap-3">
          <input className="input-base" placeholder="Email" value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <input className="input-base" placeholder="Full name" value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
          <input className="input-base" type="password" placeholder="Temp password (min 8)" value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <div className="flex gap-2">
            <select className="input-base flex-1" value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })}>
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <button onClick={() => createMut.mutate()} disabled={createMut.isLoading}
              className="btn-primary text-sm px-3">Add</button>
          </div>
        </div>
      )}

      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-700">
              {['Email', 'Name', 'Role', 'Status', 'Last Login', 'Actions'].map((h) => (
                <th key={h} className="px-4 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {users?.map((u: AdminUser) => (
              <tr key={u.id} className="border-b border-slate-700/50 hover:bg-slate-800/30">
                <td className="px-4 py-3 text-slate-200">{u.email}</td>
                <td className="px-4 py-3 text-slate-400">{u.fullName || '—'}</td>
                <td className="px-4 py-3">
                  <span className="inline-flex items-center gap-1 text-slate-300">
                    {u.role === 'ADMIN' ? <Shield className="w-3.5 h-3.5 text-accent" /> : <UserIcon className="w-3.5 h-3.5" />}
                    {u.role}
                  </span>
                </td>
                <td className="px-4 py-3">{statusBadge(u.status)}</td>
                <td className="px-4 py-3 text-slate-500 text-xs">
                  {u.lastLogin ? new Date(u.lastLogin).toLocaleString() : '—'}
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-1">
                    {u.status === 'PENDING' && (
                      <button onClick={() => approveMut.mutate(u.id)} title="Approve"
                        className="btn-ghost p-1.5 text-emerald-400"><Check className="w-4 h-4" /></button>
                    )}
                    {u.status === 'ACTIVE' && (
                      <button onClick={() => statusMut.mutate({ id: u.id, status: 'DISABLED' })} title="Disable"
                        className="btn-ghost p-1.5 text-red-400"><Ban className="w-4 h-4" /></button>
                    )}
                    {u.status === 'DISABLED' && (
                      <button onClick={() => statusMut.mutate({ id: u.id, status: 'ACTIVE' })} title="Re-enable"
                        className="btn-ghost p-1.5 text-emerald-400"><RotateCcw className="w-4 h-4" /></button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
