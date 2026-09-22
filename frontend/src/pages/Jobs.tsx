import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Briefcase, Search } from 'lucide-react'
import { api } from '../api/client'
import type { Page, JobSummary } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { JobCard } from '../components/Cards'
import { Logo } from '../components/Layout'
import { EmptyState, PageLoader, Pager } from '../components/ui'
import { BOARDS, EMPLOYMENT_TYPES, options } from '../lib/labels'

export default function Jobs() {
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const [form, setForm] = useState(() => Object.fromEntries(params.entries()))
  useEffect(() => setForm(Object.fromEntries(params.entries())), [params])
  const page = Number(params.get('page') ?? 0)
  const filters = Object.fromEntries([...params.entries()].filter(([, v]) => v))

  const { data, isLoading } = useQuery({
    queryKey: ['jobs', 'search', filters],
    queryFn: async () => (await api.get<Page<JobSummary>>('/api/jobs', { params: filters })).data,
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    setParams(Object.fromEntries(Object.entries(form).filter(([k, v]) => v && k !== 'page')))
  }
  const field = (k: string) => ({
    value: form[k] ?? '',
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setForm({ ...form, [k]: e.target.value }),
  })

  const body = (
    <div className="space-y-4">
      <form onSubmit={submit} className="card grid gap-2 p-4 sm:grid-cols-2 lg:grid-cols-6">
        <input className="input lg:col-span-2" placeholder="Title, keyword or school" {...field('q')} />
        <input className="input" placeholder="Subject" {...field('subject')} />
        <input className="input" placeholder="City" {...field('city')} />
        <select className="input" {...field('board')}>
          <option value="">Any board</option>
          {options(BOARDS).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
        <select className="input" {...field('type')}>
          <option value="">Any type</option>
          {options(EMPLOYMENT_TYPES).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
        <button className="btn-primary sm:col-span-2 lg:col-span-6 lg:justify-self-end">
          <Search className="h-4 w-4" /> Search jobs
        </button>
      </form>

      {isLoading && <PageLoader />}
      {data && (
        <p className="px-1 text-sm text-slate-500">
          {data.totalElements} open position{data.totalElements === 1 ? '' : 's'}
        </p>
      )}
      {data?.content.length === 0 && (
        <EmptyState icon={<Briefcase className="h-10 w-10" />} title="No jobs match your search">
          Try a different subject or city.
        </EmptyState>
      )}
      <div className="grid gap-3 md:grid-cols-2">
        {data?.content.map((j) => <JobCard key={j.id} job={j} />)}
      </div>
      {data && (
        <Pager
          page={page}
          totalPages={data.totalPages}
          onChange={(p) => {
            const next = new URLSearchParams(params)
            next.set('page', String(p))
            setParams(next)
          }}
        />
      )}
    </div>
  )

  if (user) return body

  // Public (logged-out) view with its own header.
  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
          <Logo />
          <div className="flex gap-2">
            <Link to="/register" className="btn-ghost">Join now</Link>
            <Link to="/login" className="btn-outline">Sign in</Link>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-3 py-5 sm:px-4">
        <h1 className="mb-4 text-2xl font-semibold">Teaching jobs</h1>
        {body}
      </main>
    </div>
  )
}
