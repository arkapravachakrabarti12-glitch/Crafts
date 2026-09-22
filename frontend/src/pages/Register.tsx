import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { errorMessage } from '../api/client'
import type { InstitutionType } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { ErrorBox, Field } from '../components/ui'
import { INSTITUTION_TYPES, options } from '../lib/labels'
import { AuthShell } from './Login'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [accountType, setAccountType] = useState<'TEACHER' | 'INSTITUTION'>(
    params.get('type') === 'INSTITUTION' ? 'INSTITUTION' : 'TEACHER',
  )
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [city, setCity] = useState('')
  const [institutionType, setInstitutionType] = useState<InstitutionType>('SCHOOL')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const me = await register({
        email,
        password,
        fullName,
        accountType,
        city,
        institutionType: accountType === 'INSTITUTION' ? institutionType : undefined,
      })
      navigate(me.role === 'INSTITUTION' ? '/institution/edit?welcome=1' : `/profile/${me.id}?welcome=1`)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthShell title="Join TeachNet" subtitle="Create your free account in under a minute">
      <div className="mb-5 grid grid-cols-2 gap-2 rounded-full bg-slate-100 p-1 text-sm font-medium">
        {(['TEACHER', 'INSTITUTION'] as const).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setAccountType(t)}
            className={`rounded-full py-2 ${accountType === t ? 'bg-white text-brand-700 shadow' : 'text-slate-600'}`}
          >
            {t === 'TEACHER' ? "I'm a teacher" : "I'm hiring"}
          </button>
        ))}
      </div>
      <form onSubmit={submit} className="space-y-4">
        <Field label={accountType === 'TEACHER' ? 'Full name' : 'Institution name'}>
          <input className="input" required maxLength={150} value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </Field>
        {accountType === 'INSTITUTION' && (
          <Field label="Type of institution">
            <select className="input" value={institutionType} onChange={(e) => setInstitutionType(e.target.value as InstitutionType)}>
              {options(INSTITUTION_TYPES).map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </Field>
        )}
        <Field label="City">
          <input className="input" maxLength={100} value={city} onChange={(e) => setCity(e.target.value)} placeholder="e.g. Pune" />
        </Field>
        <Field label="Email">
          <input className="input" type="email" required autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        </Field>
        <Field label="Password" hint="At least 8 characters">
          <input
            className="input"
            type="password"
            required
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>
        {error && <ErrorBox message={error} />}
        <button className="btn-primary w-full py-2.5" disabled={busy}>
          {busy ? 'Creating account…' : 'Agree & join'}
        </button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-600">
        Already on TeachNet?{' '}
        <Link to="/login" className="font-semibold text-brand-700 hover:underline">
          Sign in
        </Link>
      </p>
    </AuthShell>
  )
}
