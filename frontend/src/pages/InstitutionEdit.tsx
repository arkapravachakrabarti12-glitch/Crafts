import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, errorMessage } from '../api/client'
import type { Board, Institution, InstitutionType } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ErrorBox, Field, PageLoader } from '../components/ui'
import { BOARDS, INSTITUTION_TYPES, options } from '../lib/labels'

export default function InstitutionEdit() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const qc = useQueryClient()
  const { refresh } = useAuth()
  const { data, isLoading, error } = useQuery({
    queryKey: ['institution', 'me'],
    queryFn: async () => (await api.get<Institution>('/api/institutions/me')).data,
  })
  const [form, setForm] = useState({
    name: '',
    institutionType: 'SCHOOL' as InstitutionType,
    board: '' as Board | '',
    city: '',
    state: '',
    about: '',
    website: '',
    logoUrl: '',
  })
  useEffect(() => {
    if (data) {
      setForm({
        name: data.name,
        institutionType: data.institutionType,
        board: data.board ?? '',
        city: data.city ?? '',
        state: data.state ?? '',
        about: data.about ?? '',
        website: data.website ?? '',
        logoUrl: data.logoUrl ?? '',
      })
    }
  }, [data])

  const save = useMutation({
    mutationFn: async () =>
      (await api.put<Institution>('/api/institutions/me', { ...form, board: form.board || null })).data,
    onSuccess: async (inst) => {
      qc.setQueryData(['institution', inst.id], inst)
      await refresh()
      navigate(`/institutions/${inst.id}`)
    },
  })

  if (isLoading) return <PageLoader />
  if (error) return <ErrorBox message={errorMessage(error)} />

  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setForm({ ...form, [k]: e.target.value })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    save.mutate()
  }

  return (
    <div className="mx-auto max-w-2xl">
      {params.get('welcome') && (
        <div className="mb-4 rounded-xl border border-brand-200 bg-brand-50 p-4 text-sm text-brand-800">
          👋 Welcome! Set up your institution page, then post your first job opening.
        </div>
      )}
      <form onSubmit={submit} className="card space-y-4 p-6">
        <h1 className="text-xl font-semibold">Institution page</h1>
        <Field label="Name">
          <input className="input" required maxLength={200} value={form.name} onChange={set('name')} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Type">
            <select className="input" value={form.institutionType} onChange={set('institutionType')}>
              {options(INSTITUTION_TYPES).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </Field>
          <Field label="Board / curriculum">
            <select className="input" value={form.board} onChange={set('board')}>
              <option value="">—</option>
              {options(BOARDS).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </Field>
          <Field label="City">
            <input className="input" maxLength={100} value={form.city} onChange={set('city')} />
          </Field>
          <Field label="State">
            <input className="input" maxLength={100} value={form.state} onChange={set('state')} />
          </Field>
        </div>
        <Field label="Website">
          <input className="input" type="url" maxLength={300} placeholder="https://" value={form.website} onChange={set('website')} />
        </Field>
        <Field label="Logo URL">
          <input className="input" type="url" maxLength={500} placeholder="https://" value={form.logoUrl} onChange={set('logoUrl')} />
        </Field>
        <Field label="About">
          <textarea className="input min-h-32" maxLength={5000} value={form.about} onChange={set('about')} />
        </Field>
        {save.isError && <ErrorBox message={errorMessage(save.error)} />}
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-ghost" onClick={() => navigate(-1)}>Cancel</button>
          <button className="btn-primary" disabled={save.isPending}>Save</button>
        </div>
      </form>
    </div>
  )
}
