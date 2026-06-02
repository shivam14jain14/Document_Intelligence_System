import { useQuery } from 'react-query'
import { useNavigate } from 'react-router-dom'
import { History, MessageSquare, ArrowRight } from 'lucide-react'
import { chatService } from '../services/chatService'

export default function HistoryPage() {
  const navigate = useNavigate()
  const { data, isLoading } = useQuery('query-history', () => chatService.queryHistory(100))

  return (
    <div className="page-shell">
      <div className="page-hero">
        <span className="page-kicker">Activity</span>
        <div className="flex items-center gap-3">
          <div className="icon-chip bg-accent/10 border-accent/20"><History className="w-5 h-5 text-accent" /></div>
          <div>
            <h2 className="section-title">Your Query History</h2>
            <p className="section-subtitle mt-3">Every question you have asked, newest first.</p>
          </div>
        </div>
      </div>

      {isLoading ? (
        <div className="text-slate-500">Loading…</div>
      ) : !data?.length ? (
        <div className="card p-8 text-center text-slate-500">
          <MessageSquare className="w-8 h-8 mx-auto mb-2 opacity-40" />
          You haven't asked any questions yet.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {data.map((item) => (
            <button key={item.messageId}
              onClick={() => navigate('/chat')}
              className="card w-full text-left p-5 hover:border-accent/40 transition-colors group">
              <div className="flex items-start gap-3">
                <MessageSquare className="w-4 h-4 text-slate-500 mt-0.5 shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-slate-200 text-base leading-relaxed">{item.question}</p>
                  <div className="flex items-center gap-2 mt-2 text-sm text-slate-500">
                    <span className="truncate">{item.sessionTitle}</span>
                    <span>·</span>
                    <span>{new Date(item.askedAt).toLocaleString()}</span>
                  </div>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-600 group-hover:text-accent transition-colors shrink-0" />
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
