import { CheckCircle, Clock, XCircle, AlertCircle } from 'lucide-react'

interface Props { status: string }

const config = {
  INDEXED:     { label: 'Indexed',    icon: CheckCircle,   cls: 'text-emerald-400 bg-emerald-400/10' },
  PROCESSING:  { label: 'Processing', icon: Clock,         cls: 'text-yellow-400 bg-yellow-400/10 animate-pulse' },
  FAILED:      { label: 'Failed',     icon: XCircle,       cls: 'text-red-400 bg-red-400/10' },
  NEEDS_OCR:   { label: 'Needs OCR',  icon: AlertCircle,   cls: 'text-orange-400 bg-orange-400/10' },
} as const

export default function StatusBadge({ status }: Props) {
  const cfg = config[status as keyof typeof config] ?? config.PROCESSING
  const Icon = cfg.icon
  return (
    <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium ${cfg.cls}`}>
      <Icon className="w-3 h-3" /> {cfg.label}
    </span>
  )
}
