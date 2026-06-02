import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Cpu, Eye, EyeOff, FileText, Search, Shield, ArrowLeft } from 'lucide-react'
import { authService } from '../services/authService'
import { useAuthStore } from '../store/authStore'

export default function LoginPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [tab, setTab] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [showPw, setShowPw] = useState(false)
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setInfo('')
    setLoading(true)
    try {
      if (tab === 'login') {
        const data = await authService.login(email, password)
        setAuth(data.token, data.refreshToken, {
          email: data.email,
          fullName: data.fullName,
          role: data.role,
          onboardingCompleted: data.onboardingCompleted,
        })
        if (!data.onboardingCompleted) navigate('/onboarding')
        else navigate(data.role === 'ADMIN' ? '/admin' : '/upload')
      } else {
        const res = await authService.register(email, password, fullName)
        setInfo(res.message || 'Registration successful. You can sign in right away.')
        setTab('login')
        setPassword('')
      }
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex">
      <div
        className="hidden lg:flex lg:w-1/2 relative overflow-hidden flex-col justify-between p-14"
        style={{ backgroundImage: 'linear-gradient(150deg, #1a2348 0%, #221a44 55%, #0b1020 100%)' }}
      >
        <div
          className="absolute inset-0 opacity-40"
          style={{ backgroundImage: 'radial-gradient(600px circle at 20% 10%, rgba(108,143,255,0.35), transparent 50%), radial-gradient(500px circle at 90% 80%, rgba(167,139,250,0.25), transparent 50%)' }}
        />

        <div className="relative">
          <Link to="/" className="flex items-center gap-2.5 w-fit">
            <div
              className="w-10 h-10 rounded-2xl flex items-center justify-center shadow-lg"
              style={{ backgroundImage: 'linear-gradient(135deg, #6c8fff, #a78bfa)' }}
            >
              <Cpu className="text-white w-5 h-5" />
            </div>
            <span className="font-display font-bold text-white text-xl tracking-tight">
              Doc<span className="text-gradient">Intel</span>
            </span>
          </Link>
        </div>

        <div className="relative">
          <h2 className="font-display text-4xl xl:text-5xl font-extrabold text-white leading-[1.02]">
            Turn documents into
            <br />
            <span className="text-gradient">cited answers.</span>
          </h2>
          <p className="text-slate-300/80 text-lg mt-4 max-w-lg leading-relaxed">
            An agentic RAG platform that searches your private documents and answers in plain English,
            grounded in your content and backed by sources.
          </p>
          <div className="mt-8 space-y-4">
            <Highlight icon={FileText} color="text-accent" chip="bg-accent/15 border-accent/25" title="Multi-source ingestion" desc="S3, Azure, SharePoint, and local files parsed and indexed automatically." />
            <Highlight icon={Search} color="text-purple-300" chip="bg-purple-400/15 border-purple-400/25" title="Semantic search" desc="Finds meaning, not just keywords, across all your files." />
            <Highlight icon={Shield} color="text-emerald-300" chip="bg-emerald-400/15 border-emerald-400/25" title="Secure by design" desc="JWT auth, RBAC, and gated onboarding for first-time setup." />
          </div>
       
        </div>

          <div className="mt-8 inline-flex flex-col items-start gap-1 rounded-2xl border border-accent/25 bg-accent/10 px-4 py-3 text-sm text-slate-200">
            <span className="font-semibold text-white">Java - Spring Boot - Spring AI - pgvector - LLM tool-calling - React</span>
            <span className="font-semibold text-white">Built by Shivam Jain</span>
            <span className="text-slate-400">7889868193</span>
          </div>
      </div>

      <div className="flex-1 flex items-center justify-center p-6 lg:p-10">
        <div className="w-full max-w-md animate-fade-in">
          <Link to="/" className="lg:hidden flex items-center gap-2 justify-center mb-8">
            <div
              className="w-10 h-10 rounded-2xl flex items-center justify-center"
              style={{ backgroundImage: 'linear-gradient(135deg, #6c8fff, #a78bfa)' }}
            >
              <Cpu className="text-white w-5 h-5" />
            </div>
            <span className="font-display font-bold text-white text-xl">Doc<span className="text-gradient">Intel</span></span>
          </Link>

          <h1 className="font-display text-4xl font-bold text-white">
            {tab === 'login' ? 'Welcome back' : 'Create your account'}
          </h1>
          <p className="text-slate-400 text-base mt-2 mb-6">
            {tab === 'login' ? 'Sign in to access your documents' : 'Register, then sign in immediately'}
          </p>

          <div className="flex gap-1 p-1 bg-slate-800/80 rounded-lg mb-6">
            {(['login', 'register'] as const).map((t) => (
              <button
                key={t}
                onClick={() => {
                  setTab(t)
                  setError('')
                  setInfo('')
                }}
                className={`flex-1 py-2 rounded-md text-sm font-medium transition-colors capitalize ${
                  tab === t ? 'bg-slate-700 text-white shadow' : 'text-slate-400 hover:text-white'
                }`}
              >
                {t === 'login' ? 'Sign in' : 'Register'}
              </button>
            ))}
          </div>

          <form onSubmit={submit} className="flex flex-col gap-4">
            {tab === 'register' && (
              <input
                className="input-base w-full"
                placeholder="Full name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
            )}
            <input
              className="input-base w-full"
              type="email"
              placeholder="Email address"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <div className="relative">
              <input
                className="input-base w-full pr-10"
                type={showPw ? 'text' : 'password'}
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <button
                type="button"
                onClick={() => setShowPw(!showPw)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200"
              >
                {showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {error && <p className="text-red-400 text-sm bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">{error}</p>}
            {info && <p className="text-emerald-400 text-sm bg-emerald-400/10 border border-emerald-400/20 rounded-lg px-3 py-2">{info}</p>}
            <button type="submit" disabled={loading} className="btn-primary w-full">
              {loading ? 'Please wait...' : tab === 'login' ? 'Sign in' : 'Create account'}
            </button>
          </form>

          <Link to="/" className="mt-6 inline-flex items-center gap-1.5 text-slate-500 hover:text-slate-300 text-sm">
            <ArrowLeft className="w-4 h-4" /> Back to home
          </Link>
        </div>
      </div>
    </div>
  )
}

function Highlight({ icon: Icon, color, chip, title, desc }: {
  icon: typeof Cpu
  color: string
  chip: string
  title: string
  desc: string
}) {
  return (
    <div className="flex items-start gap-3">
      <div className={`icon-chip ${chip} shrink-0`}><Icon className={`w-5 h-5 ${color}`} /></div>
      <div>
        <div className="text-white font-medium text-base">{title}</div>
        <div className="text-slate-400/80 text-sm leading-relaxed">{desc}</div>
      </div>
    </div>
  )
}
