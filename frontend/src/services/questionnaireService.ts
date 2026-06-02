import api from './api'

export interface Question {
  id: string
  questionKey: string
  questionText: string
  active: boolean
  sortOrder: number
}
export interface QuestionnaireData {
  questions: Question[]
  answers: Record<string, string>
  answerSources: Record<string, string>
  completed: boolean
}

export interface QuestionnaireSubmitPayload {
  answers: Record<string, string>
  answerSources: Record<string, string>
}

export const questionnaireService = {
  get: () => api.get<QuestionnaireData>('/questionnaire').then((r) => r.data),

  submit: (payload: QuestionnaireSubmitPayload) =>
    api.post('/questionnaire', payload),

  autofill: (files: File[]) => {
    const form = new FormData()
    files.forEach((f) => form.append('files', f))
    return api.post<Record<string, string>>('/questionnaire/autofill', form).then((r) => r.data)
  },
}
