import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from 'react-query'
import { ClipboardList, Plus, Save, Trash2 } from 'lucide-react'
import { adminService } from '../../services/adminService'
import type { QuestionnaireQuestion } from '../../types/admin'

const EMPTY_FORM = {
  questionKey: '',
  questionText: '',
  sortOrder: 1,
  active: true,
}

export default function AdminQuestionnaire() {
  const qc = useQueryClient()
  const { data: questions } = useQuery('admin-questionnaire-questions', adminService.listQuestionnaireQuestions)
  const [form, setForm] = useState(EMPTY_FORM)
  const [editingId, setEditingId] = useState<string | null>(null)

  const invalidate = () => qc.invalidateQueries('admin-questionnaire-questions')

  const createMut = useMutation(
    () => adminService.createQuestionnaireQuestion(form),
    { onSuccess: () => { invalidate(); setForm(EMPTY_FORM) } }
  )
  const updateMut = useMutation(
    ({ id, payload }: { id: string; payload: Omit<QuestionnaireQuestion, 'id'> }) =>
      adminService.updateQuestionnaireQuestion(id, payload),
    { onSuccess: () => { invalidate(); setEditingId(null); setForm(EMPTY_FORM) } }
  )
  const activeMut = useMutation(
    ({ id, active }: { id: string; active: boolean }) => adminService.setQuestionnaireQuestionActive(id, active),
    { onSuccess: invalidate }
  )
  const deleteMut = useMutation((id: string) => adminService.deleteQuestionnaireQuestion(id), { onSuccess: invalidate })

  const nextSortOrder = useMemo(
    () => ((questions?.reduce((max, q) => Math.max(max, q.sortOrder), 0) ?? 0) + 1),
    [questions]
  )

  const startCreate = () => {
    setEditingId(null)
    setForm({ ...EMPTY_FORM, sortOrder: nextSortOrder })
  }

  const startEdit = (q: QuestionnaireQuestion) => {
    setEditingId(q.id)
    setForm({
      questionKey: q.questionKey,
      questionText: q.questionText,
      sortOrder: q.sortOrder,
      active: q.active,
    })
  }

  const submit = () => {
    if (!form.questionKey.trim() || !form.questionText.trim()) return
    if (editingId) updateMut.mutate({ id: editingId, payload: form })
    else createMut.mutate()
  }

  return (
    <div className="space-y-4">
      <div className="card p-4 space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-white font-semibold">Questionnaire questions</h3>
            <p className="text-slate-400 text-sm">Manage which onboarding questions users see.</p>
          </div>
          <button onClick={startCreate} className="btn-primary flex items-center gap-2 text-sm">
            <Plus className="w-4 h-4" /> New question
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-3">
          <div>
            <label className="text-xs text-slate-400 block mb-1">Key</label>
            <input
              className="input-base w-full"
              value={form.questionKey}
              onChange={(e) => setForm((f) => ({ ...f, questionKey: e.target.value }))}
              placeholder="e.g. procurement_contact"
            />
          </div>
          <div className="lg:col-span-2">
            <label className="text-xs text-slate-400 block mb-1">Question</label>
            <input
              className="input-base w-full"
              value={form.questionText}
              onChange={(e) => setForm((f) => ({ ...f, questionText: e.target.value }))}
              placeholder="What is your procurement contact email?"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-slate-400 block mb-1">Sort order</label>
              <input
                className="input-base w-full"
                type="number"
                value={form.sortOrder}
                onChange={(e) => setForm((f) => ({ ...f, sortOrder: Number(e.target.value) || 1 }))}
              />
            </div>
            <div className="flex items-end">
              <label className="flex items-center gap-2 text-sm text-slate-300 h-11">
                <input
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))}
                />
                Active
              </label>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button onClick={submit} className="btn-primary flex items-center gap-2 text-sm">
            <Save className="w-4 h-4" /> {editingId ? 'Update question' : 'Create question'}
          </button>
          {editingId && (
            <button onClick={() => { setEditingId(null); setForm(EMPTY_FORM) }} className="btn-ghost text-sm">
              Cancel
            </button>
          )}
        </div>
      </div>

      <div className="card overflow-hidden divide-y divide-slate-700/50">
        {questions?.map((q) => (
          <div key={q.id} className="flex items-center gap-3 px-4 py-3 hover:bg-slate-800/30">
            <ClipboardList className="w-5 h-5 text-accent shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="text-slate-200 font-medium truncate">{q.questionText}</span>
                <span className={`text-xs px-2 py-0.5 rounded-full border ${
                  q.active
                    ? 'bg-emerald-400/10 text-emerald-300 border-emerald-400/20'
                    : 'bg-slate-700 text-slate-400 border-slate-600'
                }`}>
                  {q.active ? 'Active' : 'Inactive'}
                </span>
              </div>
              <div className="text-slate-500 text-xs mt-1">
                key: {q.questionKey} · order: {q.sortOrder}
              </div>
            </div>
            <button
              onClick={() => activeMut.mutate({ id: q.id, active: !q.active })}
              className="btn-ghost text-xs"
            >
              Mark {q.active ? 'inactive' : 'active'}
            </button>
            <button onClick={() => startEdit(q)} className="btn-ghost text-xs">
              Edit
            </button>
            <button
              onClick={() => deleteMut.mutate(q.id)}
              className="btn-ghost p-1.5 text-red-400"
              title="Delete question"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        ))}
      </div>
    </div>
  )
}
