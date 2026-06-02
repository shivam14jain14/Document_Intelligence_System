import api from './api'
import type { ChatSession, ChatMessage, ChatResponse } from '../types/chat'
import { useAuthStore } from '../store/authStore'

export interface QueryHistoryItem {
  messageId: string
  sessionId: string
  sessionTitle: string
  question: string
  askedAt: string
}

export const chatService = {
  createSession: (title?: string) =>
    api.post<ChatSession>('/chat/sessions', { title }).then((r) => r.data),

  listSessions: () =>
    api.get<ChatSession[]>('/chat/sessions').then((r) => r.data),

  getMessages: (sessionId: string) =>
    api.get<ChatMessage[]>(`/chat/sessions/${sessionId}/messages`).then((r) => r.data),

  queryHistory: (limit = 50) =>
    api.get<QueryHistoryItem[]>('/chat/history', { params: { limit } }).then((r) => r.data),

  deleteSession: (sessionId: string) =>
    api.delete(`/chat/sessions/${sessionId}`),

  sendMessage: (sessionId: string, message: string, categoryFilter?: string) =>
    api.post<ChatResponse>(`/chat/sessions/${sessionId}/messages`, {
      message, categoryFilter,
    }).then((r) => r.data),

  streamMessage: (
    sessionId: string,
    message: string,
    category: string | undefined,
    onToken: (token: string) => void,
    onDone: () => void,
    onError: (err: string) => void
  ) => {
    const token = useAuthStore.getState().token
    const params = new URLSearchParams({ message })
    if (category) params.set('category', category)

    const es = new EventSource(
      `/api/chat/sessions/${sessionId}/stream?${params.toString()}`
    )

    // EventSource doesn't support custom headers natively
    // We use a workaround: pass token as query param (set in SecurityConfig to allow)
    es.onmessage = (e) => onToken(e.data)
    es.onerror = () => { es.close(); onDone() }
    return () => es.close()
  },
}
