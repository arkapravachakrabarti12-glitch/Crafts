import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, errorMessage } from '../api/client'
import type { Board, EmploymentType, JobDetail } from '../api/types'
import { ErrorBox, Field, PageLoader } from '../components/ui'
import { BOARDS, EMPLOYMENT_TYPES, GRADE_OPTIONS, options } from '../lib/labels'

const EMPTY = {
  title: '',
  description: '',
  subject: '',
  board: '' as Board | '',
  gradeFrom: '',
  gradeTo: '',
  employmentType: 'FULL_TIME' as EmploymentType,
  salaryMin: '',
  salaryMax: '',
  city: '',
}

export default function JobForm() {
  const { id } = useParams()
  const editing = !!id
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [form, setForm] = useState(EMPTY)

  const existing = useQuery({
    queryKey: ['job', Number(id)],
    enabled: editing,
    queryFn: async () => (await api.get<JobDetail>(`/api/jobs/${id}`)).data,
  })
  useEffect(() => {
    const d = existing.data
    if (d) {
      const j = d.job
      setForm({
        title: j.title,
        description: d.description,
        subject: j.subject,
        board: j.board ?? '',
        gradeFrom: j.gradeFrom?.toString() ?? '',
        gradeTo: j.gradeTo?.toString() ?? '',
        employmentType: j.employmentType,
        salaryMin: j.salaryMin?.toString() ?? '',
        salaryMax: j.salaryMax?.toString() ?? '',
        city: j.city ?? '',
      })
    }
  }, [existing.data])

  const save = useMutation({
    mutationFn: async () => {
      const num = (v: string) => (v === '' ? null : Number(v))
      const body = {
        ...form,
        board: form.board || null,
        gradeFrom: num(form.gradeFrom),
        gradeTo: num(form.gradeTo),
        salaryMin: num(form.salaryMin),
        salaryMax: num(form.salaryMax),
      }
      const res = editing ? await api.put<JobDetail>(`/api/jobs/${id}`, body) : await api.post<JobDetail>('/api/jobs', body)
      return res.data
    },
    onSuccess: (d) => {
      qc.invalidateQueries({ queryKey: ['jobs'] })
      qc.invalidateQueries({ queryKey: ['my-jobs'] })
      qc.setQueryData(['job', d.job.id], d)
      navigate(`/jobs/${d.job.id}`)
    },
  })

  if (editing && existing.isLoading) return <PageLoader />

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setForm({ ...form, [k]: e.target.value })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    save.mutate()
  }

  return (
    <form onSubmit={submit} className="card mx-auto max-w-2xl space-y-4 p-6">
      <h1 className="text-xl font-semibold">{editing ? 'Edit job' : 'Post a teaching job'}</h1>
      <Field label="Job title">
        <input className="input" required maxLength={200} placeholder="e.g. PGT Physics" value={form.title} onChange={set('title')} />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Subject">
          <input className="input" required maxLength={100} value={form.subject} onChange={set('subject')} />
        </Field>
        <Field label="Employment type">
          <select className="input" value={form.employmentType} onChange={set('employmentType')}>
            {options(EMPLOYMENT_TYPES).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </Field>
        <Field label="Board / curriculum">
          <select className="input" value={form.board} onChange={set('board')}>
            <option value="">Any</option>
            {options(BOARDS).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </Field>
        <Field label="City" hint="Defaults to your institution's city">
          <input className="input" maxLength={100} value={form.city} onChange={set('city')} />
        </Field>
        <Field label="Grades from">
          <select className="input" value={form.gradeFrom} onChange={set('gradeFrom')}>
            <option value="">—</option>
            {GRADE_OPTIONS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
          </select>
        </Field>
        <Field label="Grades to">
          <select className="input" value={form.gradeTo} onChange={set('gradeTo')}>
            <option value="">—</option>
            {GRADE_OPTIONS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
          </select>
        </Field>
        <Field label="Salary from (₹ / month)">
          <input className="input" type="number" min={0} value={form.salaryMin} onChange={set('salaryMin')} />
        </Field>
        <Field label="Salary to (₹ / month)">
          <input className="input" type="number" min={0} value={form.salaryMax} onChange={set('salaryMax')} />
        </Field>
      </div>
      <Field label="Description" hint="Responsibilities, required qualifications (B.Ed., CTET…), timings, benefits">
        <textarea className="input min-h-40" required maxLength={10000} value={form.description} onChange={set('description')} />
      </Field>
      {save.isError && <ErrorBox message={errorMessage(save.error)} />}
      <div className="flex justify-end gap-2">
        <button type="button" className="btn-ghost" onClick={() => navigate(-1)}>Cancel</button>
        <button className="btn-primary" disabled={save.isPending}>{editing ? 'Save changes' : 'Post job'}</button>
      </div>
    </form>
  )
}
