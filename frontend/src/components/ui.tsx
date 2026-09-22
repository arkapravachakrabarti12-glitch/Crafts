import type { ReactNode } from 'react'
import { BadgeCheck, Loader2 } from 'lucide-react'

export function Spinner({ className = '' }: { className?: string }) {
  return <Loader2 className={`h-5 w-5 animate-spin text-brand-600 ${className}`} aria-label="Loading" />
}

export function PageLoader() {
  return (
    <div className="flex justify-center py-16">
      <Spinner className="h-7 w-7" />
    </div>
  )
}

export function ErrorBox({ message }: { message: string }) {
  return <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{message}</div>
}

export function EmptyState({ icon, title, children }: { icon?: ReactNode; title: string; children?: ReactNode }) {
  return (
    <div className="card flex flex-col items-center px-6 py-10 text-center">
      {icon && <div className="mb-3 text-slate-400">{icon}</div>}
      <h3 className="font-semibold text-slate-800">{title}</h3>
      {children && <div className="mt-1 max-w-md text-sm text-slate-500">{children}</div>}
    </div>
  )
}

export function VerifiedBadge({ className = '' }: { className?: string }) {
  return (
    <span title="Verified" className={`inline-flex ${className}`}>
      <BadgeCheck className="h-4 w-4 text-brand-600" aria-label="Verified" />
    </span>
  )
}

const COLORS = ['bg-indigo-500', 'bg-emerald-500', 'bg-amber-500', 'bg-rose-500', 'bg-sky-500', 'bg-violet-500']

export function Avatar({
  name,
  src,
  size = 'md',
  square = false,
}: {
  name: string
  src?: string | null
  size?: 'sm' | 'md' | 'lg' | 'xl'
  square?: boolean
}) {
  const dims = { sm: 'h-8 w-8 text-xs', md: 'h-11 w-11 text-sm', lg: 'h-16 w-16 text-xl', xl: 'h-28 w-28 text-4xl' }[size]
  const shape = square ? 'rounded-lg' : 'rounded-full'
  if (src) {
    return <img src={src} alt={name} className={`${dims} ${shape} shrink-0 object-cover`} />
  }
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase())
    .join('')
  const color = COLORS[(name.charCodeAt(0) || 0) % COLORS.length]
  return (
    <div className={`${dims} ${shape} ${color} flex shrink-0 items-center justify-center font-semibold text-white`}>
      {initials || '?'}
    </div>
  )
}

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
    </label>
  )
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/40 p-0 sm:items-center sm:p-4"
      onMouseDown={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="max-h-[92vh] w-full overflow-y-auto rounded-t-2xl bg-white p-5 shadow-xl sm:max-w-lg sm:rounded-2xl">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">{title}</h2>
          <button className="btn-ghost px-2 py-1" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function Pager({
  page,
  totalPages,
  onChange,
}: {
  page: number
  totalPages: number
  onChange: (page: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-center gap-3 py-2 text-sm">
      <button className="btn-ghost" disabled={page === 0} onClick={() => onChange(page - 1)}>
        ← Previous
      </button>
      <span className="text-slate-500">
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn-ghost" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Next →
      </button>
    </div>
  )
}
