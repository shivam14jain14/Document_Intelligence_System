import { useState } from 'react'
import { LayoutDashboard, Users, FolderCog, ScrollText, ClipboardList } from 'lucide-react'
import AdminDashboard from '../components/admin/AdminDashboard'
import AdminUsers from '../components/admin/AdminUsers'
import AdminCategories from '../components/admin/AdminCategories'
import AdminAudit from '../components/admin/AdminAudit'
import AdminQuestionnaire from '../components/admin/AdminQuestionnaire'

type Tab = 'dashboard' | 'users' | 'categories' | 'questionnaire' | 'audit'

const TABS: { id: Tab; label: string; icon: typeof Users }[] = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'users', label: 'Users', icon: Users },
  { id: 'categories', label: 'Categories', icon: FolderCog },
  { id: 'questionnaire', label: 'Questionnaire', icon: ClipboardList },
  { id: 'audit', label: 'Audit Log', icon: ScrollText },
]

export default function AdminPage() {
  const [tab, setTab] = useState<Tab>('dashboard')

  return (
    <div className="page-shell">
      <div className="page-hero">
        <span className="page-kicker">Control Center</span>
        <div>
          <h2 className="section-title mb-1">Admin Panel</h2>
          <p className="section-subtitle mt-3 max-w-3xl">Manage users, categories, onboarding questions, and platform activity.</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-1.5 p-1.5 bg-slate-900/70 border border-slate-700/70 rounded-2xl mb-6 w-fit">
        {TABS.map((t) => {
          const Icon = t.icon
          return (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`flex items-center gap-2 px-4 py-3 rounded-xl text-[15px] font-semibold transition-colors ${
                tab === t.id ? 'bg-slate-700 text-white border border-slate-600/80' : 'text-slate-400 hover:text-white'}`}>
              <Icon className="w-4 h-4" /> {t.label}
            </button>
          )
        })}
      </div>

      {tab === 'dashboard' && <AdminDashboard />}
      {tab === 'users' && <AdminUsers />}
      {tab === 'categories' && <AdminCategories />}
      {tab === 'questionnaire' && <AdminQuestionnaire />}
      {tab === 'audit' && <AdminAudit />}
    </div>
  )
}
