import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Search as SearchIcon } from 'lucide-react'
import { api } from '../api/client'
import type { InstitutionCard, Page, TeacherCard } from '../api/types'
import { TeacherCardView } from '../components/Cards'
import { Avatar, EmptyState, PageLoader, Pager, VerifiedBadge } from '../components/ui'
import { BOARDS, INSTITUTION_TYPES, options } from '../lib/labels'

type Tab = 'teachers' | 'institutions'

export default function Search() {
  const [params, setParams] = useSearchParams()
  const tab = (params.get('tab') as Tab) || 'teachers'
  const page = Number(params.get('page') ?? 0)

  // Local form state mirrors the URL so filters are shareable/bookmarkable.
  const [form, setForm] = useState(() => Object.fromEntries(params.entries()))
  useEffect(() => setForm(Object.fromEntries(params.entries())), [params])

  const filters = Object.fromEntries([...params.entries()].filter(([k, v]) => v && k !== 'tab'))

  const teachers = useQuery({
    queryKey: ['search-teachers', filters],
    enabled: tab === 'teachers',
    queryFn: async () => (await api.get<Page<TeacherCard>>('/api/teachers', { params: filters })).data,
  })
  const institutions = useQuery({
    queryKey: ['search-institutions', filters],
    enabled: tab === 'institutions',
    queryFn: async () => (await api.get<Page<InstitutionCard>>('/api/institutions', { params: filters })).data,
  })

  const apply = (e?: FormEvent) => {
    e?.preventDefault()
    const next = new URLSearchParams()
    next.set('tab', tab)
    Object.entries(form).forEach(([k, v]) => {
      if (v && k !== 'tab' && k !== 'page') next.set(k, v)
    })
    setParams(next)
  }
  const setTab = (t: Tab) => setParams({ tab: t, ...(params.get('q') ? { q: params.get('q')! } : {}) })
  const setPage = (p: number) => {
    const next = new URLSearchParams(params)
    next.set('page', String(p))
    setParams(next)
  }
  const field = (k: string) => ({
    value: form[k] ?? '',
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setForm({ ...form, [k]: e.target.value }),
  })

  const data = tab === 'teachers' ? teachers : institutions

  return (
    <div className="grid gap-5 lg:grid-cols-[280px_minmax(0,1fr)]">
      <aside className="card h-fit p-4 lg:sticky lg:top-20">
        <div className="mb-4 grid grid-cols-2 gap-1 rounded-full bg-slate-100 p-1 text-sm font-medium">
          {(['teachers', 'institutions'] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`rounded-full py-1.5 capitalize ${tab === t ? 'bg-white text-brand-700 shadow' : 'text-slate-600'}`}
            >
              {t}
            </button>
          ))}
        </div>
        <form onSubmit={apply} className="space-y-3">
          <input className="input" placeholder={tab === 'teachers' ? 'Name or headline' : 'Institution name'} {...field('q')} />
          <input className="input" placeholder="City" {...field('city')} />
          {tab === 'teachers' ? (
            <>
              <input className="input" placeholder="Subject (e.g. Physics)" {...field('subject')} />
              <select className="input" {...field('board')}>
                <option value="">Any board</option>
                {options(BOARDS).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              <select className="input" {...field('minExperience')}>
                <option value="">Any experience</option>
                {[1, 3, 5, 10].map((n) => <option key={n} value={n}>{n}+ years</option>)}
              </select>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="h-4 w-4 accent-brand-600"
                  checked={form.openToWork === 'true'}
                  onChange={(e) => setForm({ ...form, openToWork: e.target.checked ? 'true' : '' })}
                />
                Open to work only
              </label>
            </>
          ) : (
            <select className="input" {...field('type')}>
              <option value="">Any type</option>
              {options(INSTITUTION_TYPES).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          )}
          <button className="btn-primary w-full">
            <SearchIcon className="h-4 w-4" /> Search
          </button>
        </form>
      </aside>

      <div className="space-y-3">
        {data.isLoading && <PageLoader />}
        {data.data && (
          <p className="px-1 text-sm text-slate-500">
            {data.data.totalElements} {tab === 'teachers' ? 'teacher' : 'institution'}
            {data.data.totalElements === 1 ? '' : 's'} found
          </p>
        )}
        {data.data?.content.length === 0 && (
          <EmptyState icon={<SearchIcon className="h-10 w-10" />} title="No results">
            Try removing some filters.
          </EmptyState>
        )}
        {tab === 'teachers' && teachers.data?.content.map((t) => <TeacherCardView key={t.userId} teacher={t} />)}
        {tab === 'institutions' &&
          institutions.data?.content.map((i) => (
            <Link key={i.id} to={`/institutions/${i.id}`} className="card flex items-center gap-3 p-4 hover:border-brand-200">
              <Avatar name={i.name} src={i.logoUrl} square />
              <div>
                <div className="flex items-center gap-1 font-semibold">
                  {i.name}
                  {i.verified && <VerifiedBadge />}
                </div>
                <div className="text-sm text-slate-500">
                  {INSTITUTION_TYPES[i.institutionType]}
                  {i.city && ` · ${i.city}`}
                </div>
              </div>
            </Link>
          ))}
        {data.data && <Pager page={page} totalPages={data.data.totalPages} onChange={setPage} />}
      </div>
    </div>
  )
}
