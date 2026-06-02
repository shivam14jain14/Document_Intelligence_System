import { CheckCircle, XCircle, Info, X } from 'lucide-react'
import { useToastStore, type ToastType } from '../../store/toastStore'

const config: Record<ToastType, { icon: typeof Info; cls: string; iconCls: string }> = {
  success: { icon: CheckCircle, cls: 'border-emerald-500/30', iconCls: 'text-emerald-400' },
  error:   { icon: XCircle,     cls: 'border-red-500/30',     iconCls: 'text-red-400' },
  info:    { icon: Info,        cls: 'border-accent/30',      iconCls: 'text-accent' },
}

export default function Toaster() {
  const { toasts, remove } = useToastStore()

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 w-80 max-w-[calc(100vw-2.5rem)]">
      {toasts.map((t) => {
        const c = config[t.type]
        const Icon = c.icon
        return (
          <div key={t.id}
            className={`card ${c.cls} px-4 py-3 flex items-start gap-3 shadow-xl animate-fade-in`}>
            <Icon className={`w-5 h-5 shrink-0 mt-0.5 ${c.iconCls}`} />
            <span className="text-sm text-slate-200 flex-1">{t.message}</span>
            <button onClick={() => remove(t.id)} className="text-slate-500 hover:text-slate-300 shrink-0">
              <X className="w-4 h-4" />
            </button>
          </div>
        )
      })}
    </div>
  )
}
