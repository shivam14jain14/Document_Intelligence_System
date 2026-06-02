import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDropzone } from 'react-dropzone'
import { useQuery } from 'react-query'
import { Sparkles, FileText, RefreshCw, ClipboardList, ArrowRight, AlertCircle, BadgeCheck, PencilLine } from 'lucide-react'
import { questionnaireService } from '../services/questionnaireService'
import { useAuthStore } from '../store/authStore'
import { toast } from '../store/toastStore'

const SOURCE_AUTO_DOCUMENT = 'AUTO_DOCUMENT'
const SOURCE_MANUAL = 'MANUAL'
const draftKeyFor = (email?: string) => `docint-questionnaire-draft:${email ?? 'anonymous'}`

export default function OnboardingPage() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const setOnboardingCompleted = useAuthStore((s) => s.setOnboardingCompleted)

  const { data, isLoading } = useQuery('questionnaire', questionnaireService.get)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [answerSources, setAnswerSources] = useState<Record<string, string>>({})
  const [aiFilled, setAiFilled] = useState<Set<string>>(new Set())
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [autofilling, setAutofilling] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!data) return
    let draftAnswers: Record<string, string> = {}
    let draftSources: Record<string, string> = {}
    try {
      const rawDraft = localStorage.getItem(draftKeyFor(user?.email))
      if (rawDraft) {
        const parsed = JSON.parse(rawDraft)
        draftAnswers = parsed.answers ?? {}
        draftSources = parsed.answerSources ?? {}
      }
    } catch {
      // Ignore invalid local drafts and fall back to server state.
    }

    const nextAnswers = { ...(data.answers ?? {}), ...draftAnswers }
    const nextSources = { ...(data.answerSources ?? {}), ...draftSources }
    Object.keys(nextAnswers).forEach((key) => {
      if (!nextSources[key]) nextSources[key] = SOURCE_MANUAL
    })
    setAnswers(nextAnswers)
    setAnswerSources(nextSources)
    setAiFilled(new Set(
      Object.entries(nextSources)
        .filter(([, source]) => source === SOURCE_AUTO_DOCUMENT)
        .map(([key]) => key)
    ))
  }, [data, user?.email])

  useEffect(() => {
    if (!user?.email) return
    localStorage.setItem(
      draftKeyFor(user.email),
      JSON.stringify({ answers, answerSources })
    )
  }, [answers, answerSources, user?.email])

  const setAnswer = (key: string, val: string) => {
    setAnswers((prev) => ({ ...prev, [key]: val }))
    setAnswerSources((prev) => ({ ...prev, [key]: SOURCE_MANUAL }))
    setAiFilled((prev) => {
      const next = new Set(prev)
      next.delete(key)
      return next
    })
    setErrors((prev) => {
      const next = { ...prev }
      delete next[key]
      return next
    })
  }

  const onDrop = useCallback(async (files: File[]) => {
    if (!files.length) return
    setAutofilling(true)
    try {
      const suggested = await questionnaireService.autofill(files)
      const keys = Object.keys(suggested)
      if (keys.length === 0) {
        toast.info("Couldn't extract answers from that document. Please fill them manually below.")
      } else {
        const newlyFilledKeys = keys.filter((key) => !(answers[key] ?? '').trim() && suggested[key]?.trim())
        setAnswers((prev) => {
          const next = { ...prev }
          newlyFilledKeys.forEach((key) => {
            next[key] = suggested[key]
          })
          return next
        })
        setAnswerSources((prev) => {
          const next = { ...prev }
          newlyFilledKeys.forEach((key) => {
            next[key] = SOURCE_AUTO_DOCUMENT
          })
          return next
        })
        setAiFilled((prev) => {
          const next = new Set(prev)
          newlyFilledKeys.forEach((key) => next.add(key))
          return next
        })
        setErrors((prev) => {
          const next = { ...prev }
          newlyFilledKeys.forEach((key) => delete next[key])
          return next
        })
        if (newlyFilledKeys.length === 0) {
          toast.info('We found possible details in the document, but your existing answers were already filled. Review them below.')
        } else {
          toast.success(`Document auto-fill completed. ${newlyFilledKeys.length} question${newlyFilledKeys.length === 1 ? '' : 's'} filled from your file.`)
        }
      }
    } catch {
      toast.error('Auto-fill failed. Please try again or fill manually.')
    } finally {
      setAutofilling(false)
    }
  }, [answers])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    multiple: true,
    accept: {
      'application/pdf': ['.pdf'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
      'text/plain': ['.txt'],
    },
  })

  const submit = async () => {
    const missing = (data?.questions ?? [])
      .filter((q) => !answers[q.questionKey]?.trim())
      .reduce<Record<string, string>>((acc, q) => {
        acc[q.questionKey] = 'This field is required.'
        return acc
      }, {})

    if (Object.keys(missing).length > 0) {
      setErrors(missing)
      toast.error('Please complete all required questions before continuing.')
      return
    }

    setSubmitting(true)
    try {
      await questionnaireService.submit({ answers, answerSources })
      if (user?.email) {
        localStorage.removeItem(draftKeyFor(user.email))
      }
      setOnboardingCompleted(true)
      toast.success('Profile completed. Welcome to the workspace.')
      navigate(user?.role === 'ADMIN' ? '/admin' : '/upload')
    } catch (err: any) {
      toast.error(err?.response?.data?.detail || 'Could not save. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  const answeredCount = data ? data.questions.filter((q) => answers[q.questionKey]?.trim()).length : 0
  const total = data?.questions.length ?? 0

  return (
    <div className="page-shell">
      <div className="page-hero">
        <span className="page-kicker">Profile Setup</span>
        <div className="flex flex-col xl:flex-row xl:items-start xl:justify-between gap-4">
          <div className="flex items-start gap-3">
            <div className="icon-chip bg-accent/10 border-accent/20">
              <ClipboardList className="w-5 h-5 text-accent" />
            </div>
            <div>
              <h2 className="section-title">Complete your profile</h2>
              <p className="section-subtitle mt-3 max-w-3xl">
                Add the user details we did not collect during registration so departments, workspace defaults,
                and future personalization can be tailored to you.
                {total > 0 && <span className="text-slate-200 font-medium"> {answeredCount}/{total} answered.</span>}
              </p>
            </div>
          </div>
          <div className="shrink-0 rounded-2xl border border-slate-700/70 bg-slate-900/45 px-5 py-4 xl:max-w-sm">
            <p className="text-slate-200 text-sm font-semibold">Required before entering the platform</p>
            <p className="text-slate-400 text-sm mt-1">
              Until this is completed, the rest of the app stays locked. Once saved, you will be routed into your
              normal user or admin workspace.
            </p>
          </div>
        </div>
      </div>

      <div className="card p-6 mb-5">
        <div className="flex flex-col md:flex-row md:items-center gap-2 mb-3">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-accent" />
            <span className="text-white font-semibold text-base">Auto-fill with AI</span>
          </div>
          <span className="text-slate-500 text-sm">
            Upload a resume, profile sheet, or internal intro doc and we will fill what we can. Any missing fields must still be completed manually.
          </span>
        </div>
        <div
          {...getRootProps()}
          className={`border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition-colors ${
            isDragActive ? 'border-accent bg-accent/5' : 'border-slate-600 hover:border-slate-500 hover:bg-slate-800/40'
          }`}
        >
          <input {...getInputProps()} />
          {autofilling ? (
            <div className="flex items-center justify-center gap-2 text-accent text-base">
              <RefreshCw className="w-5 h-5 animate-spin" /> Reading your document and extracting answers...
            </div>
          ) : (
            <div className="text-slate-300 text-base flex items-center justify-center gap-2">
              <FileText className="w-4 h-4" /> Drop a PDF, DOCX, XLSX, or TXT file, or click to browse
            </div>
          )}
        </div>
      </div>

      {isLoading ? (
        <div className="text-slate-500">Loading questions...</div>
      ) : (
        <div className="grid grid-cols-1 xl:grid-cols-2 2xl:grid-cols-3 gap-4">
          {data?.questions.map((q) => (
            <div key={q.id} className={`card p-5 min-h-[210px] ${errors[q.questionKey] ? 'border-rose-400/60 shadow-[0_0_0_1px_rgba(251,113,133,0.18)]' : ''}`}>
              <div className="flex items-start justify-between gap-3 mb-3">
                <label className="text-base font-medium text-slate-100 leading-snug">{q.questionText}</label>
                {answers[q.questionKey]?.trim() && (
                  <span className={`inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full border ${
                    answerSources[q.questionKey] === SOURCE_AUTO_DOCUMENT
                      ? 'text-cyan-200 bg-cyan-400/12 border-cyan-300/30'
                      : 'text-amber-200 bg-amber-400/12 border-amber-300/30'
                  }`}>
                    {answerSources[q.questionKey] === SOURCE_AUTO_DOCUMENT ? (
                      <>
                        <BadgeCheck className="w-3 h-3" /> Autofilled
                      </>
                    ) : (
                      <>
                        <PencilLine className="w-3 h-3" /> Manual
                      </>
                    )}
                  </span>
                )}
              </div>
              <textarea
                value={answers[q.questionKey] ?? ''}
                onChange={(e) => setAnswer(q.questionKey, e.target.value)}
                rows={3}
                placeholder="Enter details here..."
                className="input-base w-full resize-y"
              />
              {errors[q.questionKey] ? (
                <div className="mt-3 inline-flex items-center gap-2 text-rose-300 text-sm">
                  <AlertCircle className="w-4 h-4" /> {errors[q.questionKey]}
                </div>
              ) : aiFilled.has(q.questionKey) ? (
                <div className="mt-3 inline-flex items-center gap-2 text-cyan-200 text-sm">
                  <Sparkles className="w-4 h-4" /> Autofilled from your uploaded document. Review before saving.
                </div>
              ) : answers[q.questionKey]?.trim() ? (
                <div className="mt-3 inline-flex items-center gap-2 text-amber-200 text-sm">
                  <PencilLine className="w-4 h-4" /> Entered manually.
                </div>
              ) : (
                <div className="mt-3 text-slate-500 text-sm">Required field.</div>
              )}
            </div>
          ))}

          {!data?.completed && (
            <div className="xl:col-span-2 2xl:col-span-3 flex items-center justify-end gap-3 pt-2 pb-8">
              <button onClick={submit} disabled={submitting} className="btn-primary flex items-center gap-2">
                {submitting ? 'Saving...' : <>Save and continue <ArrowRight className="w-4 h-4" /></>}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
