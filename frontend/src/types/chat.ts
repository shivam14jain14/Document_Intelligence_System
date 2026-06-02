export interface ChatSession {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface SourceChunk {
  documentId: string
  documentName: string
  category: string
  excerpt: string
}

export interface ChatMessage {
  id: string
  role: 'USER' | 'ASSISTANT'
  content: string
  sourceChunks: SourceChunk[]
  createdAt: string
}

export interface ChatResponse {
  messageId: string
  sessionId: string
  answer: string
  sourceChunks: SourceChunk[]
}
