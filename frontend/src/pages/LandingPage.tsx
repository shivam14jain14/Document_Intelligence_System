import { useNavigate } from 'react-router-dom'
import {
  Cpu, ArrowRight, Upload, MessageSquare, FileText,
  Database, Shield, Zap, FolderOpen, Search, Layers, Quote,
} from 'lucide-react'
import { useAuthStore } from '../store/authStore'

export default function LandingPage() {
  const navigate = useNavigate()
  const token = useAuthStore((s) => s.token)

  const launch = () => navigate(token ? '/upload' : '/login')

  return (
    <div className="min-h-screen text-slate-200">
      <header className="sticky top-0 z-20 backdrop-blur border-b border-slate-800/60 bg-slate-900/40">
        <div className="max-w-[1440px] mx-auto px-5 lg:px-8 h-20 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div
              className="w-11 h-11 rounded-2xl flex items-center justify-center shadow-lg"
              style={{ backgroundImage: 'linear-gradient(135deg, #6c8fff, #a78bfa)' }}
            >
              <Cpu className="text-white w-5 h-5" />
            </div>
            <span className="font-display font-bold text-white text-xl lg:text-2xl tracking-tight">
              Document<span className="text-gradient">Intelligence System</span>
            </span>
          </div>
          <div className="flex items-center gap-3">
            <button onClick={() => navigate('/login')} className="btn-ghost text-sm">Sign in</button>
            <button onClick={launch} className="btn-primary text-sm flex items-center gap-1.5">
              {token ? 'Go to app' : 'Launch app'} <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      <section className="max-w-[1440px] mx-auto px-5 lg:px-8 pt-16 lg:pt-20 pb-14 grid lg:grid-cols-[1.1fr_0.9fr] gap-8 lg:gap-12 items-center">
        <div className="animate-fade-in">
          <h1 className="font-display text-[3rem] lg:text-[4.7rem] font-extrabold text-white leading-[0.96] tracking-tight">
            Ask your documents.
            <br />
            Get <span className="text-gradient">cited answers.</span>
          </h1>
          <p className="text-slate-300/85 text-xl mt-5 max-w-2xl leading-relaxed">
            Upload your enterprise documents from any source, then ask questions in plain English.
            An AI agent retrieves the exact passages, reasons across them, and answers -
            <span className="text-slate-200"> always citing the source.</span>
          </p>
          <div className="flex flex-wrap items-center gap-3 mt-8">
            <button onClick={launch} className="btn-primary flex items-center gap-2">
              Get started <ArrowRight className="w-4 h-4" />
            </button>
            <button onClick={() => navigate('/login')} className="btn-ghost border border-slate-700">
              Sign in
            </button>
          </div>
          <div className="mt-5 inline-flex items-center gap-2 rounded-full border border-accent/25 bg-accent/10 px-4 py-2 text-sm text-slate-200">
            <span className="font-semibold text-white">Built by Shivam Jain</span>
            <span className="text-slate-400">7889868193</span>
          </div>
          <div className="flex flex-wrap gap-x-6 gap-y-2 mt-8 text-base text-slate-500">
            <span className="flex items-center gap-1.5"><FolderOpen className="w-4 h-4 text-accent" /> S3 - Local - Multi-source</span>
            <span className="flex items-center gap-1.5"><FileText className="w-4 h-4 text-emerald-400" /> PDF - DOCX - XLSX - PPTX</span>
          </div>
        </div>

        <div className="card p-7 lg:p-8 animate-fade-in shadow-2xl">
          <div className="flex items-center gap-2 text-slate-400 text-base mb-5">
            <MessageSquare className="w-4 h-4 text-accent" /> Document Q and A
          </div>
          <div className="space-y-4">
            <div className="flex justify-end">
              <div
                className="px-4 py-3 rounded-2xl rounded-tr-none text-white text-base max-w-[80%]"
                style={{ backgroundImage: 'linear-gradient(135deg, #6c8fff, #7c83ff)' }}
              >
                What&apos;s the penalty for a 3-week late delivery by Vendor X?
              </div>
            </div>
            <div className="flex gap-3">
              <div
                className="w-8 h-8 rounded-full flex items-center justify-center shrink-0 shadow"
                style={{ backgroundImage: 'linear-gradient(135deg, #34d399, #10b981)' }}
              >
                <Cpu className="w-4 h-4 text-white" />
              </div>
              <div className="bg-slate-800/90 border border-slate-700/60 rounded-2xl rounded-tl-none px-4 py-3.5 text-base text-slate-200">
                Per <strong className="text-white">Section 4.2</strong>, a 2% per week penalty applies, so the total is 6%,
                within the 10% cap defined in Section 4.3.
                <div className="mt-2">
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-accent/15 text-accent border border-accent/30 rounded-full text-xs">
                    <FileText className="w-3 h-3" /> Vendor_X_Contract.pdf
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="max-w-[1440px] mx-auto px-5 lg:px-8 py-14">
        <h2 className="text-center font-display text-4xl lg:text-5xl font-bold text-white mb-3">How it works</h2>
        <p className="text-center text-slate-400 text-lg mb-12">Three steps from raw documents to grounded answers</p>
        <div className="grid md:grid-cols-3 gap-6">
          <Step
            n={1}
            icon={Upload}
            color="text-accent"
            chip="bg-accent/10 border-accent/20"
            title="Upload"
            desc="Drop documents from S3, Azure, SharePoint, or local. They are parsed, chunked, and embedded automatically."
          />
          <Step
            n={2}
            icon={Search}
            color="text-purple-400"
            chip="bg-purple-400/10 border-purple-400/20"
            title="Ask"
            desc="Ask in natural language. The agent searches your content semantically, meaning not just keywords."
          />
          <Step
            n={3}
            icon={MessageSquare}
            color="text-emerald-400"
            chip="bg-emerald-400/10 border-emerald-400/20"
            title="Get cited answers"
            desc="Claude reasons across the retrieved passages and answers, linking back to the exact source documents."
          />
        </div>
      </section>

      <section className="max-w-[1440px] mx-auto px-5 lg:px-8 py-14">
        <h2 className="text-center font-display text-4xl lg:text-5xl font-bold text-white mb-3">Built like a production system</h2>
        <p className="text-center text-slate-400 text-lg mb-12">Not a toy. Real architecture under the hood.</p>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          <Feature icon={Cpu} color="text-accent" chip="bg-accent/10 border-accent/20" title="Agentic RAG" desc="LLM tool-calling with multi-hop retrieval across documents." />
          <Feature icon={Database} color="text-blue-400" chip="bg-blue-400/10 border-blue-400/20" title="Vector search" desc="pgvector semantic similarity over OpenAI embeddings." />
          <Feature icon={FolderOpen} color="text-emerald-400" chip="bg-emerald-400/10 border-emerald-400/20" title="Multi-source storage" desc="S3, Azure Blob, SharePoint, and local through one abstraction." />
          <Feature icon={Shield} color="text-rose-400" chip="bg-rose-400/10 border-rose-400/20" title="Secure by design" desc="JWT with refresh tokens, RBAC, and server-side revocation." />
          <Feature icon={Zap} color="text-yellow-400" chip="bg-yellow-400/10 border-yellow-400/20" title="Two-tier caching" desc="Separate caches for retrieval results and final LLM answers." />
          <Feature icon={Layers} color="text-purple-400" chip="bg-purple-400/10 border-purple-400/20" title="Admin console" desc="Onboarding, categories, dashboard, and audit log." />
        </div>
      </section>

      <section className="max-w-[1440px] mx-auto px-5 lg:px-8 py-14">
        <div className="card p-10 lg:p-12 text-center relative overflow-hidden">
          <div
            className="absolute inset-0 opacity-20"
            style={{ backgroundImage: 'radial-gradient(600px circle at 50% 0%, #6c8fff, transparent 60%)' }}
          />
          <div className="relative">
            <Quote className="w-8 h-8 text-accent mx-auto mb-4" />
            <h2 className="font-display text-4xl lg:text-5xl font-bold text-white mb-3">Turn your documents into answers</h2>
            <p className="text-slate-400 text-lg mb-6 max-w-2xl mx-auto">
              Stop scrolling through long search results. Ask once and get a cited answer in seconds.
            </p>
            <button onClick={launch} className="btn-primary inline-flex items-center gap-2">
              {token ? 'Go to app' : 'Get started'} <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </section>

      <footer className="border-t border-slate-800/60 py-8">
        <div className="max-w-[1440px] mx-auto px-5 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-3 text-sm text-slate-500">
          <div className="flex items-center gap-2">
            <Cpu className="w-4 h-4 text-accent" /> Document Intelligence Platform
          </div>
          <div className="flex flex-col items-center sm:items-end gap-1">
            <span>Built with Java, Spring Boot, Spring AI, and React</span>
            <span>Built by Shivam Jain 7889868193</span>
          </div>
        </div>
      </footer>
    </div>
  )
}

function Step({ n, icon: Icon, color, chip, title, desc }: {
  n: number
  icon: typeof Cpu
  color: string
  chip: string
  title: string
  desc: string
}) {
  return (
    <div className="card card-hover p-6 relative">
      <span className="absolute top-4 right-5 text-5xl font-extrabold text-slate-700/40">{n}</span>
      <div className={`icon-chip ${chip} mb-4`}><Icon className={`w-5 h-5 ${color}`} /></div>
      <h3 className="font-semibold text-white text-xl mb-2">{title}</h3>
      <p className="text-slate-400 text-base leading-relaxed">{desc}</p>
    </div>
  )
}

function Feature({ icon: Icon, color, chip, title, desc }: {
  icon: typeof Cpu
  color: string
  chip: string
  title: string
  desc: string
}) {
  return (
    <div className="card card-hover p-6">
      <div className={`icon-chip ${chip} mb-4`}><Icon className={`w-5 h-5 ${color}`} /></div>
      <h3 className="font-semibold text-white text-xl mb-2">{title}</h3>
      <p className="text-slate-400 text-base leading-relaxed">{desc}</p>
    </div>
  )
}
