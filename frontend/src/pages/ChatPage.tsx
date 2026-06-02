import { useState, useRef, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { Plus, Trash2, Send, Bot, User, FileText, ChevronDown } from 'lucide-react'
import { chatService } from '../services/chatService'
import { documentService } from '../services/documentService'
import type { ChatMessage, ChatSession } from '../types/chat'
import ReactMarkdown from 'react-markdown'

export default function ChatPage() {
  const qc = useQueryClient()
  const [activeSession, setActiveSession] = useState<string | null>(null)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [category, setCategory] = useState('all')
  const [streaming, setStreaming] = useState(false)
  const [streamText, setStreamText] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)

  const { data: sessions } = useQuery('sessions', chatService.listSessions)
  const { data: categories } = useQuery('categories', documentService.categories)

  const { data: history } = useQuery(
    ['messages', activeSession],
    () => chatService.getMessages(activeSession!),
    { enabled: !!activeSession, onSuccess: setMessages }
  )

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, streamText])

  const createSession = async () => {
    const s = await chatService.createSession('New Chat')
    qc.invalidateQueries('sessions')
    setActiveSession(s.id)
    setMessages([])
  }

  const deleteSession = useMutation(
    (id: string) => chatService.deleteSession(id),
    { onSuccess: () => { qc.invalidateQueries('sessions'); setActiveSession(null); setMessages([]) } }
  )

  const sendMessage = async () => {
    if (!input.trim() || !activeSession || streaming) return
    const text = input.trim()
    setInput('')

    const userMsg: ChatMessage = {
      id: Date.now().toString(), role: 'USER', content: text,
      sourceChunks: [], createdAt: new Date().toISOString(),
    }
    setMessages((prev) => [...prev, userMsg])
    setStreaming(true)
    setStreamText('')

    try {
      const data = await chatService.sendMessage(activeSession, text, category === 'all' ? undefined : category)
      const aiMsg: ChatMessage = {
        id: data.messageId, role: 'ASSISTANT', content: data.answer,
        sourceChunks: data.sourceChunks, createdAt: new Date().toISOString(),
      }
      setMessages((prev) => [...prev, aiMsg])
    } catch (e) {
      setMessages((prev) => [...prev, {
        id: Date.now().toString(), role: 'ASSISTANT',
        content: 'Something went wrong. Please try again.',
        sourceChunks: [], createdAt: new Date().toISOString(),
      }])
    } finally { setStreaming(false); setStreamText('') }
  }

  return (
    <div className="flex-1 flex overflow-hidden m-4 lg:m-6 rounded-[28px] border border-slate-700/70 bg-slate-950/25 shadow-2xl backdrop-blur-sm">
      {/* Sidebar */}
      <aside className="w-72 bg-slate-900/70 border-r border-slate-700/70 flex flex-col shrink-0">
        <div className="p-4 border-b border-slate-700/70">
          <div className="mb-4">
            <div className="text-[11px] uppercase tracking-[0.22em] text-slate-500 font-semibold">Workspace Chat</div>
            <div className="font-display text-xl font-bold text-white mt-1">Ask the indexed library</div>
          </div>
          <button onClick={createSession}
            className="btn-primary w-full flex items-center justify-center gap-2 text-sm">
            <Plus className="w-4 h-4" /> New Chat
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {!sessions?.length && (
            <p className="text-slate-500 text-xs text-center py-4">No chats yet</p>
          )}
          {sessions?.map((s: ChatSession) => (
            <div key={s.id}
              onClick={() => setActiveSession(s.id)}
              className={`group flex items-center gap-2 px-3 py-2.5 rounded-xl cursor-pointer transition-colors ${
                activeSession === s.id ? 'bg-slate-700/80 text-white border border-slate-600/70' : 'text-slate-400 hover:bg-slate-800/70 hover:text-white border border-transparent'}`}>
              <Bot className="w-4 h-4 shrink-0" />
              <span className="flex-1 text-sm truncate">{s.title}</span>
              <button onClick={(e) => { e.stopPropagation(); deleteSession.mutate(s.id) }}
                className="opacity-0 group-hover:opacity-100 text-slate-500 hover:text-red-400 transition-opacity">
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          ))}
        </div>
      </aside>

      {/* Main */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {!activeSession ? (
          <div className="flex-1 flex flex-col items-center justify-center text-slate-500 gap-4">
            <Bot className="w-14 h-14 opacity-30" />
            <p className="font-display text-2xl font-bold text-slate-300">Start a conversation</p>
            <p className="text-base text-slate-500">Create a new chat or select one from the sidebar</p>
            <button onClick={createSession} className="btn-primary flex items-center gap-2">
              <Plus className="w-4 h-4" /> New Chat
            </button>
          </div>
        ) : (
          <>
            {/* Category filter bar */}
            <div className="px-5 py-3 border-b border-slate-700/70 flex items-center gap-3 bg-slate-900/35">
              <span className="text-slate-400 text-xs font-semibold uppercase tracking-[0.18em]">Filter</span>
              <div className="flex gap-1.5 flex-wrap">
                {['all', ...(categories?.map((c) => c.name) ?? [])].map((cat) => (
                  <button key={cat} onClick={() => setCategory(cat)}
                    className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors capitalize ${
                      category === cat
                        ? 'bg-accent text-white'
                        : 'bg-slate-800 text-slate-400 hover:text-white'}`}>
                    {cat}
                  </button>
                ))}
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-5 lg:p-6 space-y-4">
              {messages.length === 0 && !streaming && (
                <div className="flex flex-col items-center justify-center h-full text-slate-500 gap-3">
                  <FileText className="w-10 h-10 opacity-30" />
                  <p className="text-base">Ask a question about your documents</p>
                </div>
              )}

              {messages.map((msg) => (
                <MessageBubble key={msg.id} msg={msg} />
              ))}

              {streaming && (
                <div className="flex gap-3 animate-fade-in">
                  <div className="w-8 h-8 rounded-full flex items-center justify-center shrink-0 shadow-lg"
                    style={{ backgroundImage: 'linear-gradient(135deg, #34d399, #10b981)' }}>
                    <Bot className="w-4 h-4 text-white" />
                  </div>
                  <div className="bg-slate-800/90 border border-slate-700/60 rounded-2xl rounded-tl-none px-4 py-3 max-w-[80%] shadow-md">
                    <ThinkingIndicator />
                  </div>
                </div>
              )}
              <div ref={bottomRef} />
            </div>

            {/* Input */}
            <div className="p-5 border-t border-slate-700/70 bg-slate-900/35">
              <div className="flex gap-2 items-end">
                <textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage() } }}
                  placeholder="Ask a question about your documents…"
                  rows={1}
                  className="input-base flex-1 resize-none min-h-[48px] max-h-32"
                  style={{ height: 'auto' }}
                />
                <button onClick={sendMessage} disabled={streaming || !input.trim()}
                  className="btn-primary h-11 w-11 flex items-center justify-center shrink-0 rounded-xl">
                  <Send className="w-4 h-4" />
                </button>
              </div>
              <p className="text-slate-600 text-xs mt-1.5 text-center">Enter to send · Shift+Enter for new line</p>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function MessageBubble({ msg }: { msg: ChatMessage }) {
  const isUser = msg.role === 'USER'
  return (
    <div className={`flex gap-3 animate-fade-in ${isUser ? 'flex-row-reverse' : ''}`}>
      <div className="w-8 h-8 rounded-full flex items-center justify-center shrink-0 shadow-lg"
        style={{ backgroundImage: isUser
          ? 'linear-gradient(135deg, #6c8fff, #8b7bff)'
          : 'linear-gradient(135deg, #34d399, #10b981)' }}>
        {isUser ? <User className="w-4 h-4 text-white" /> : <Bot className="w-4 h-4 text-white" />}
      </div>
      <div className={`max-w-[80%] space-y-2 ${isUser ? 'items-end flex flex-col' : ''}`}>
        <div className={`px-4 py-3 rounded-2xl text-sm leading-relaxed shadow-md ${
          isUser
            ? 'text-white rounded-tr-none'
            : 'bg-slate-800/90 border border-slate-700/60 text-slate-200 rounded-tl-none'}`}
          style={isUser ? { backgroundImage: 'linear-gradient(135deg, #6c8fff, #7c83ff)' } : undefined}>
          {isUser
            ? <p>{msg.content}</p>
            : <ReactMarkdown className="prose prose-sm prose-invert max-w-none">{msg.content}</ReactMarkdown>}
        </div>
        {msg.sourceChunks?.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {msg.sourceChunks.map((s, i) => (
              s.documentId ? (
                <button key={i} onClick={() => documentService.download(s.documentId, s.documentName)}
                  className="inline-flex items-center gap-1 px-2 py-0.5 bg-accent/15 text-accent border border-accent/30 rounded-full text-xs hover:bg-accent/25 transition-colors"
                  title="Download source document">
                  <FileText className="w-3 h-3" /> {s.documentName}
                </button>
              ) : (
                <span key={i}
                  className="inline-flex items-center gap-1 px-2 py-0.5 bg-slate-700 text-slate-300 rounded-full text-xs">
                  <FileText className="w-3 h-3" /> {s.documentName}
                </span>
              )
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function StreamingDots() {
  return (
    <div className="flex gap-1.5 items-center">
      {[0, 1, 2].map((i) => (
        <div key={i}
          className="w-2 h-2 bg-accent rounded-full animate-bounce"
          style={{ animationDelay: `${i * 0.15}s` }} />
      ))}
    </div>
  )
}

/** Cycles through pipeline-stage labels while the agent works, for perceived progress. */
function ThinkingIndicator() {
  const stages = [
    'Searching your documents…',
    'Reading the most relevant sections…',
    'Reasoning over the sources…',
    'Composing a cited answer…',
  ]
  const [i, setI] = useState(0)
  useEffect(() => {
    const t = setInterval(() => setI((p) => Math.min(p + 1, stages.length - 1)), 2200)
    return () => clearInterval(t)
  }, [])
  return (
    <div className="flex items-center gap-3">
      <StreamingDots />
      <span className="text-slate-400 text-sm">{stages[i]}</span>
    </div>
  )
}
