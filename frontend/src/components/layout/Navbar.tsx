import { Link, NavLink, useNavigate } from 'react-router-dom'
import { FileText, MessageSquare, LogOut, Cpu, FolderOpen, Shield, History, ClipboardList } from 'lucide-react'
import { useAuthStore } from '../../store/authStore'
import { authService } from '../../services/authService'

export default function Navbar() {
  const { user, clearAuth } = useAuthStore()
  const navigate = useNavigate()
  const isAdmin = user?.role === 'ADMIN'
  const needsOnboarding = !!user && !user.onboardingCompleted

  const logout = () => { clearAuth(); navigate('/login') }

  const logoutEverywhere = async () => {
    try { await authService.logoutAll() } catch { /* ignore */ }
    clearAuth(); navigate('/login')
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-2 px-3.5 py-2.5 rounded-xl text-[15px] font-semibold transition-colors ${
      isActive ? 'bg-slate-700/80 text-white border border-slate-600/80' : 'text-slate-400 hover:text-white hover:bg-slate-700/50 border border-transparent'
    }`

  return (
    <header className="h-20 bg-slate-900/70 backdrop-blur-xl border-b border-slate-700/60 flex items-center px-4 lg:px-6 gap-4 shrink-0 sticky top-0 z-20">
      <Link to="/" className="flex items-center gap-3 mr-3 rounded-2xl px-2 py-1.5 hover:bg-slate-800/50 transition-colors">
        <div className="w-10 h-10 rounded-2xl flex items-center justify-center shadow-lg"
          style={{ backgroundImage: 'linear-gradient(135deg, #ff8e53, #ff6e7f 48%, #6c8fff)' }}>
          <Cpu className="text-white w-5 h-5" />
        </div>
        <div className="hidden sm:block">
          <div className="font-display text-lg font-bold text-white leading-tight">Doc<span className="text-gradient">Intel</span></div>
          <div className="text-[11px] uppercase tracking-[0.22em] text-slate-500">Home</div>
        </div>
      </Link>

      <nav className="flex gap-1 overflow-x-auto no-scrollbar">
        {!needsOnboarding && (
          <>
            <NavLink to="/upload" className={linkClass}>
              <FileText className="w-4 h-4" /> Documents
            </NavLink>
            <NavLink to="/browse" className={linkClass}>
              <FolderOpen className="w-4 h-4" /> Browse
            </NavLink>
            <NavLink to="/chat" className={linkClass}>
              <MessageSquare className="w-4 h-4" /> Chat
            </NavLink>
            <NavLink to="/history" className={linkClass}>
              <History className="w-4 h-4" /> History
            </NavLink>
          </>
        )}
        <NavLink to="/onboarding" className={linkClass}>
          <ClipboardList className="w-4 h-4" /> {needsOnboarding ? 'Questionnaire' : 'Profile'}
        </NavLink>
        {isAdmin && !needsOnboarding && (
          <NavLink to="/admin" className={linkClass}>
            <Shield className="w-4 h-4" /> Admin
          </NavLink>
        )}
      </nav>

      <div className="ml-auto flex items-center gap-3">
        {isAdmin && (
          <span className="text-xs px-2.5 py-1 rounded-full bg-accent/15 text-accent border border-accent/30 font-semibold">
            ADMIN
          </span>
        )}
        {needsOnboarding && (
          <span className="hidden lg:inline-flex text-xs px-2.5 py-1 rounded-full bg-yellow-500/10 text-yellow-300 border border-yellow-500/20 font-semibold">
            Complete questionnaire
          </span>
        )}
        <span className="hidden xl:inline text-slate-400 text-sm">{user?.email}</span>
        <button onClick={logout} className="btn-ghost flex items-center gap-1.5 text-sm" title="Log out this device">
          <LogOut className="w-4 h-4" /> Logout
        </button>
        <button onClick={logoutEverywhere}
          className="btn-ghost text-sm text-red-400 hover:text-red-300"
          title="Invalidate all sessions on every device">
          All
        </button>
      </div>
    </header>
  )
}
