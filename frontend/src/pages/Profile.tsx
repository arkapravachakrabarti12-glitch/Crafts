import { useState, type FormEvent, type ReactNode } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  BookOpen,
  Briefcase,
  ExternalLink,
  GraduationCap,
  MapPin,
  MessageSquare,
  Pencil,
  Plus,
  Trash2,
  UserCheck,
  UserPlus,
} from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { Board, Page, PortfolioType, Post, Profile as ProfileT } from '../api/types'
import { useAuth, useMe } from '../auth/AuthContext'
import PostCard from '../components/PostCard'
import { Avatar, ErrorBox, Field, Modal, PageLoader, VerifiedBadge } from '../components/ui'
import { BOARDS, GRADE_OPTIONS, PORTFOLIO_TYPES, gradeRange, options } from '../lib/labels'
import { useStartConversation } from '../lib/useStartConversation'

type EditTarget = null | 'intro' | 'subject' | 'qualification' | 'experience' | 'portfolio'

function Section({
  title,
  icon,
  onAdd,
  children,
}: {
  title: string
  icon: ReactNode
  onAdd?: () => void
  children: ReactNode
}) {
  return (
    <section className="card p-5">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-lg font-semibold">
          {icon}
          {title}
        </h2>
        {onAdd && (
          <button className="btn-ghost px-2" onClick={onAdd} aria-label={`Add ${title}`}>
            <Plus className="h-5 w-5" />
          </button>
        )}
      </div>
      {children}
    </section>
  )
}

function DeleteButton({ onClick }: { onClick: () => void }) {
  return (
    <button className="shrink-0 text-slate-400 hover:text-red-600" onClick={onClick} aria-label="Remove">
      <Trash2 className="h-4 w-4" />
    </button>
  )
}

/** Generic modal form that POSTs/PUTs to the profile API and updates the cached profile. */
function useProfileMutation(userId: number, onDone: () => void) {
  const qc = useQueryClient()
  const { refresh } = useAuth()
  return useMutation({
    mutationFn: async ({ method, url, body }: { method: 'post' | 'put' | 'delete'; url: string; body?: unknown }) =>
      (await api.request<ProfileT>({ method, url, data: body })).data,
    onSuccess: (profile) => {
      qc.setQueryData(['profile', userId], profile)
      qc.invalidateQueries({ queryKey: ['posts'] })
      refresh()
      onDone()
    },
  })
}

function IntroForm({ profile, onClose }: { profile: ProfileT; onClose: () => void }) {
  const m = useProfileMutation(profile.userId, onClose)
  const [form, setForm] = useState({
    fullName: profile.fullName,
    headline: profile.headline ?? '',
    bio: profile.bio ?? '',
    city: profile.city ?? '',
    state: profile.state ?? '',
    yearsExperience: profile.yearsExperience,
    photoUrl: profile.photoUrl ?? '',
    openToWork: profile.openToWork,
  })
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm({ ...form, [k]: e.target.type === 'checkbox' ? (e.target as HTMLInputElement).checked : e.target.value })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    m.mutate({ method: 'put', url: '/api/profiles/me', body: { ...form, yearsExperience: Number(form.yearsExperience) } })
  }
  return (
    <Modal title="Edit intro" onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <Field label="Full name">
          <input className="input" required maxLength={150} value={form.fullName} onChange={set('fullName')} />
        </Field>
        <Field label="Headline" hint="e.g. Physics teacher · CBSE Class 11–12 · JEE mentor">
          <input className="input" maxLength={200} value={form.headline} onChange={set('headline')} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="City">
            <input className="input" maxLength={100} value={form.city} onChange={set('city')} />
          </Field>
          <Field label="State">
            <input className="input" maxLength={100} value={form.state} onChange={set('state')} />
          </Field>
        </div>
        <Field label="Years of teaching experience">
          <input className="input" type="number" min={0} max={60} value={form.yearsExperience} onChange={set('yearsExperience')} />
        </Field>
        <Field label="Photo URL" hint="Link to a profile photo (https://…)">
          <input className="input" type="url" maxLength={500} value={form.photoUrl} onChange={set('photoUrl')} />
        </Field>
        <Field label="About">
          <textarea className="input min-h-28" maxLength={5000} value={form.bio} onChange={set('bio')} />
        </Field>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={form.openToWork} onChange={set('openToWork')} className="h-4 w-4 accent-brand-600" />
          Open to new teaching opportunities
        </label>
        {m.isError && <ErrorBox message={errorMessage(m.error)} />}
        <div className="flex justify-end">
          <button className="btn-primary" disabled={m.isPending}>
            Save
          </button>
        </div>
      </form>
    </Modal>
  )
}

function SubjectForm({ userId, onClose }: { userId: number; onClose: () => void }) {
  const m = useProfileMutation(userId, onClose)
  const [subject, setSubject] = useState('')
  const [board, setBoard] = useState<Board>('CBSE')
  const [gradeFrom, setGradeFrom] = useState(6)
  const [gradeTo, setGradeTo] = useState(10)
  return (
    <Modal title="Add a subject you teach" onClose={onClose}>
      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          m.mutate({ method: 'post', url: '/api/profiles/me/subjects', body: { subject, board, gradeFrom, gradeTo } })
        }}
      >
        <Field label="Subject">
          <input className="input" required maxLength={100} placeholder="e.g. Mathematics" value={subject} onChange={(e) => setSubject(e.target.value)} />
        </Field>
        <Field label="Board / curriculum">
          <select className="input" value={board} onChange={(e) => setBoard(e.target.value as Board)}>
            {options(BOARDS).map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="From">
            <select className="input" value={gradeFrom} onChange={(e) => setGradeFrom(Number(e.target.value))}>
              {GRADE_OPTIONS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
            </select>
          </Field>
          <Field label="To">
            <select className="input" value={gradeTo} onChange={(e) => setGradeTo(Number(e.target.value))}>
              {GRADE_OPTIONS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
            </select>
          </Field>
        </div>
        {m.isError && <ErrorBox message={errorMessage(m.error)} />}
        <div className="flex justify-end">
          <button className="btn-primary" disabled={m.isPending}>Add</button>
        </div>
      </form>
    </Modal>
  )
}

function QualificationForm({ userId, onClose }: { userId: number; onClose: () => void }) {
  const m = useProfileMutation(userId, onClose)
  const [degree, setDegree] = useState('')
  const [institute, setInstitute] = useState('')
  const [year, setYear] = useState('')
  return (
    <Modal title="Add education or certification" onClose={onClose}>
      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          m.mutate({
            method: 'post',
            url: '/api/profiles/me/qualifications',
            body: { degree, institute, completionYear: year ? Number(year) : null },
          })
        }}
      >
        <Field label="Degree / certification" hint="e.g. B.Ed., M.Sc. Physics, CTET, TET">
          <input className="input" required maxLength={150} value={degree} onChange={(e) => setDegree(e.target.value)} />
        </Field>
        <Field label="Institute / issuing body">
          <input className="input" required maxLength={200} value={institute} onChange={(e) => setInstitute(e.target.value)} />
        </Field>
        <Field label="Year completed">
          <input className="input" type="number" min={1950} max={2100} value={year} onChange={(e) => setYear(e.target.value)} />
        </Field>
        {m.isError && <ErrorBox message={errorMessage(m.error)} />}
        <div className="flex justify-end">
          <button className="btn-primary" disabled={m.isPending}>Add</button>
        </div>
      </form>
    </Modal>
  )
}

function ExperienceForm({ userId, onClose }: { userId: number; onClose: () => void }) {
  const m = useProfileMutation(userId, onClose)
  const [f, setF] = useState({ title: '', organization: '', startYear: '', endYear: '', description: '' })
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setF({ ...f, [k]: e.target.value })
  return (
    <Modal title="Add teaching experience" onClose={onClose}>
      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          m.mutate({
            method: 'post',
            url: '/api/profiles/me/experiences',
            body: { ...f, startYear: Number(f.startYear), endYear: f.endYear ? Number(f.endYear) : null },
          })
        }}
      >
        <Field label="Role">
          <input className="input" required maxLength={150} placeholder="e.g. TGT Mathematics" value={f.title} onChange={set('title')} />
        </Field>
        <Field label="School / institution">
          <input className="input" required maxLength={200} value={f.organization} onChange={set('organization')} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Start year">
            <input className="input" type="number" required min={1950} max={2100} value={f.startYear} onChange={set('startYear')} />
          </Field>
          <Field label="End year" hint="Leave empty if current">
            <input className="input" type="number" min={1950} max={2100} value={f.endYear} onChange={set('endYear')} />
          </Field>
        </div>
        <Field label="Description">
          <textarea className="input min-h-20" maxLength={3000} value={f.description} onChange={set('description')} />
        </Field>
        {m.isError && <ErrorBox message={errorMessage(m.error)} />}
        <div className="flex justify-end">
          <button className="btn-primary" disabled={m.isPending}>Add</button>
        </div>
      </form>
    </Modal>
  )
}

function PortfolioForm({ userId, onClose }: { userId: number; onClose: () => void }) {
  const m = useProfileMutation(userId, onClose)
  const [f, setF] = useState({ itemType: 'DEMO_VIDEO' as PortfolioType, title: '', url: '', description: '' })
  return (
    <Modal title="Add to portfolio" onClose={onClose}>
      <form
        className="space-y-3"
        onSubmit={(e) => {
          e.preventDefault()
          m.mutate({ method: 'post', url: '/api/profiles/me/portfolio', body: f })
        }}
      >
        <Field label="Type">
          <select className="input" value={f.itemType} onChange={(e) => setF({ ...f, itemType: e.target.value as PortfolioType })}>
            {options(PORTFOLIO_TYPES).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </Field>
        <Field label="Title">
          <input className="input" required maxLength={200} value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} />
        </Field>
        <Field label="Link" hint="YouTube, Google Drive, website… (https://)">
          <input className="input" type="url" required maxLength={500} value={f.url} onChange={(e) => setF({ ...f, url: e.target.value })} />
        </Field>
        <Field label="Description">
          <textarea className="input min-h-20" maxLength={2000} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} />
        </Field>
        {m.isError && <ErrorBox message={errorMessage(m.error)} />}
        <div className="flex justify-end">
          <button className="btn-primary" disabled={m.isPending}>Add</button>
        </div>
      </form>
    </Modal>
  )
}

function ConnectionActions({ profile }: { profile: ProfileT }) {
  const me = useMe()
  const qc = useQueryClient()
  const message = useStartConversation()
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['profile', profile.userId] })
    qc.invalidateQueries({ queryKey: ['pending'] })
    qc.invalidateQueries({ queryKey: ['connections'] })
  }
  const request = useMutation({ mutationFn: () => api.post(`/api/connections/request/${profile.userId}`), onSuccess: refresh })
  const accept = useMutation({ mutationFn: () => api.post(`/api/connections/${profile.connectionId}/accept`), onSuccess: refresh })
  const remove = useMutation({ mutationFn: () => api.delete(`/api/connections/${profile.connectionId}`), onSuccess: refresh })

  const messageBtn = (
    <button className="btn-outline" onClick={() => message.mutate(profile.userId)} disabled={message.isPending}>
      <MessageSquare className="h-4 w-4" /> Message
    </button>
  )
  if (me.role !== 'TEACHER') return <div className="flex gap-2">{messageBtn}</div>

  return (
    <div className="flex flex-wrap gap-2">
      {profile.connectionState === 'NONE' && (
        <button className="btn-primary" onClick={() => request.mutate()} disabled={request.isPending}>
          <UserPlus className="h-4 w-4" /> Connect
        </button>
      )}
      {profile.connectionState === 'PENDING_SENT' && (
        <button className="btn-outline" onClick={() => remove.mutate()} disabled={remove.isPending} title="Withdraw request">
          Pending · Withdraw
        </button>
      )}
      {profile.connectionState === 'PENDING_RECEIVED' && (
        <>
          <button className="btn-primary" onClick={() => accept.mutate()} disabled={accept.isPending}>
            Accept request
          </button>
          <button className="btn-ghost" onClick={() => remove.mutate()} disabled={remove.isPending}>
            Ignore
          </button>
        </>
      )}
      {profile.connectionState === 'CONNECTED' && (
        <button
          className="btn-outline"
          onClick={() => confirm(`Remove ${profile.fullName} from your connections?`) && remove.mutate()}
        >
          <UserCheck className="h-4 w-4" /> Connected
        </button>
      )}
      {messageBtn}
    </div>
  )
}

function Activity({ userId }: { userId: number }) {
  const q = useInfiniteQuery({
    queryKey: ['posts', 'user', userId],
    queryFn: async ({ pageParam }) =>
      (await api.get<Page<Post>>(`/api/posts/user/${userId}`, { params: { page: pageParam, size: 5 } })).data,
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  })
  const posts = q.data?.pages.flatMap((p) => p.content) ?? []
  if (posts.length === 0) return null
  return (
    <div className="space-y-3">
      <h2 className="px-1 text-lg font-semibold">Activity</h2>
      {posts.map((p) => <PostCard key={p.id} post={p} />)}
      {q.hasNextPage && (
        <button className="btn-ghost w-full" onClick={() => q.fetchNextPage()}>Show more posts</button>
      )}
    </div>
  )
}

export default function Profile() {
  const { userId } = useParams()
  const id = Number(userId)
  const [params] = useSearchParams()
  const [edit, setEdit] = useState<EditTarget>(null)
  const { data: profile, isLoading, error } = useQuery({
    queryKey: ['profile', id],
    queryFn: async () => (await api.get<ProfileT>(`/api/profiles/${id}`)).data,
  })
  const remove = useProfileMutation(id, () => {})

  if (isLoading) return <PageLoader />
  if (error || !profile) return <ErrorBox message={errorMessage(error)} />

  const self = profile.connectionState === 'SELF'
  const add = (t: EditTarget) => (self ? () => setEdit(t) : undefined)
  const del = (path: string) => remove.mutate({ method: 'delete', url: `/api/profiles/me/${path}` })
  const incomplete = self && (profile.subjects.length === 0 || !profile.headline)

  return (
    <div className="mx-auto grid max-w-4xl gap-4">
      {params.get('welcome') && self && (
        <div className="rounded-xl border border-brand-200 bg-brand-50 p-4 text-sm text-brand-800">
          👋 Welcome to TeachNet! Complete your profile (headline, subjects and experience) so schools can find you.
        </div>
      )}

      <section className="card overflow-hidden">
        <div className="h-28 bg-gradient-to-r from-brand-700 via-brand-500 to-sky-400 sm:h-36" />
        <div className="px-5 pb-5">
          <div className="-mt-14 flex items-end justify-between">
            <div className="rounded-full ring-4 ring-white">
              <Avatar name={profile.fullName} src={profile.photoUrl} size="xl" />
            </div>
            {self && (
              <button className="btn-ghost" onClick={() => setEdit('intro')} aria-label="Edit intro">
                <Pencil className="h-5 w-5" />
              </button>
            )}
          </div>
          <h1 className="mt-3 flex items-center gap-2 text-2xl font-semibold">
            {profile.fullName}
            {profile.verified && <VerifiedBadge className="[&>svg]:h-5 [&>svg]:w-5" />}
          </h1>
          {profile.headline && <p className="text-slate-700">{profile.headline}</p>}
          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-slate-500">
            {(profile.city || profile.state) && (
              <span className="inline-flex items-center gap-1">
                <MapPin className="h-4 w-4" />
                {[profile.city, profile.state].filter(Boolean).join(', ')}
              </span>
            )}
            <span>{profile.yearsExperience} years teaching</span>
            <span className="font-medium text-brand-700">{profile.connectionCount} connections</span>
          </div>
          {profile.openToWork && (
            <div className="mt-3 inline-block rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
              <strong>Open to work</strong> · available for new teaching roles
            </div>
          )}
          {!self && (
            <div className="mt-4">
              <ConnectionActions profile={profile} />
            </div>
          )}
          {incomplete && (
            <div className="mt-4 flex flex-wrap gap-2">
              {!profile.headline && <button className="btn-outline" onClick={() => setEdit('intro')}>Add a headline</button>}
              {profile.subjects.length === 0 && (
                <button className="btn-outline" onClick={() => setEdit('subject')}>Add subjects you teach</button>
              )}
            </div>
          )}
        </div>
      </section>

      {(profile.bio || self) && (
        <Section title="About" icon={null}>
          {profile.bio ? (
            <p className="whitespace-pre-wrap text-sm text-slate-700">{profile.bio}</p>
          ) : (
            <p className="text-sm text-slate-400">Tell schools and teachers about your teaching style. Click the pencil above.</p>
          )}
        </Section>
      )}

      <Section title="Subjects & grades" icon={<BookOpen className="h-5 w-5 text-brand-600" />} onAdd={add('subject')}>
        {profile.subjects.length === 0 && <p className="text-sm text-slate-400">No subjects added yet.</p>}
        <ul className="flex flex-wrap gap-2">
          {profile.subjects.map((s) => (
            <li key={s.id} className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm">
              <div>
                <div className="font-medium">{s.subject}</div>
                <div className="text-xs text-slate-500">
                  {BOARDS[s.board]} · {gradeRange(s.gradeFrom, s.gradeTo)}
                </div>
              </div>
              {self && <DeleteButton onClick={() => del(`subjects/${s.id}`)} />}
            </li>
          ))}
        </ul>
      </Section>

      <Section title="Experience" icon={<Briefcase className="h-5 w-5 text-brand-600" />} onAdd={add('experience')}>
        {profile.experiences.length === 0 && <p className="text-sm text-slate-400">No experience added yet.</p>}
        <ul className="divide-y divide-slate-100">
          {profile.experiences.map((e) => (
            <li key={e.id} className="flex gap-3 py-3 first:pt-0 last:pb-0">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-500">
                <Briefcase className="h-5 w-5" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="font-medium">{e.title}</div>
                <div className="text-sm text-slate-700">{e.organization}</div>
                <div className="text-xs text-slate-500">
                  {e.startYear} – {e.endYear ?? 'Present'}
                </div>
                {e.description && <p className="mt-1 whitespace-pre-wrap text-sm text-slate-600">{e.description}</p>}
              </div>
              {self && <DeleteButton onClick={() => del(`experiences/${e.id}`)} />}
            </li>
          ))}
        </ul>
      </Section>

      <Section
        title="Education & certifications"
        icon={<GraduationCap className="h-5 w-5 text-brand-600" />}
        onAdd={add('qualification')}
      >
        {profile.qualifications.length === 0 && <p className="text-sm text-slate-400">No qualifications added yet.</p>}
        <ul className="divide-y divide-slate-100">
          {profile.qualifications.map((q) => (
            <li key={q.id} className="flex gap-3 py-3 first:pt-0 last:pb-0">
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-1 font-medium">
                  {q.degree}
                  {q.verified && <VerifiedBadge />}
                </div>
                <div className="text-sm text-slate-700">{q.institute}</div>
                {q.completionYear && <div className="text-xs text-slate-500">{q.completionYear}</div>}
              </div>
              {self && <DeleteButton onClick={() => del(`qualifications/${q.id}`)} />}
            </li>
          ))}
        </ul>
      </Section>

      <Section title="Teaching portfolio" icon={<ExternalLink className="h-5 w-5 text-brand-600" />} onAdd={add('portfolio')}>
        {profile.portfolio.length === 0 && (
          <p className="text-sm text-slate-400">
            {self ? 'Add demo lesson videos, lesson plans or results to stand out.' : 'Nothing here yet.'}
          </p>
        )}
        <ul className="grid gap-3 sm:grid-cols-2">
          {profile.portfolio.map((p) => (
            <li key={p.id} className="flex gap-2 rounded-lg border border-slate-200 p-3">
              <div className="min-w-0 flex-1">
                <span className="chip mb-1">{PORTFOLIO_TYPES[p.itemType]}</span>
                <a href={p.url} target="_blank" rel="noopener noreferrer" className="block font-medium text-brand-700 hover:underline">
                  {p.title} ↗
                </a>
                {p.description && <p className="mt-1 text-sm text-slate-600">{p.description}</p>}
              </div>
              {self && <DeleteButton onClick={() => del(`portfolio/${p.id}`)} />}
            </li>
          ))}
        </ul>
      </Section>

      {remove.isError && <ErrorBox message={errorMessage(remove.error)} />}

      <Activity userId={profile.userId} />

      {edit === 'intro' && <IntroForm profile={profile} onClose={() => setEdit(null)} />}
      {edit === 'subject' && <SubjectForm userId={id} onClose={() => setEdit(null)} />}
      {edit === 'qualification' && <QualificationForm userId={id} onClose={() => setEdit(null)} />}
      {edit === 'experience' && <ExperienceForm userId={id} onClose={() => setEdit(null)} />}
      {edit === 'portfolio' && <PortfolioForm userId={id} onClose={() => setEdit(null)} />}
    </div>
  )
}
