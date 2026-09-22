import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { AdminInstitution, AdminStats, TeacherCard } from '../api/types'
import { PageLoader, VerifiedBadge } from '../components/ui'
import { INSTITUTION_TYPES } from '../lib/labels'

function Stat({ label, value }: { label: string; value: number | undefined }) {
  return (
    <div className="card p-4">
      <div className="text-2xl font-semibold">{value ?? '–'}</div>
      <div className="text-sm text-slate-500">{label}</div>
    </div>
  )
}

export default function Admin() {
  const qc = useQueryClient()
  const [tab, setTab] = useState<'teachers' | 'institutions'>('teachers')
  const stats = useQuery({ queryKey: ['admin', 'stats'], queryFn: async () => (await api.get<AdminStats>('/api/admin/stats')).data })
  const teachers = useQuery({
    queryKey: ['admin', 'teachers'],
    queryFn: async () => (await api.get<TeacherCard[]>('/api/admin/teachers')).data,
  })
  const institutions = useQuery({
    queryKey: ['admin', 'institutions'],
    queryFn: async () => (await api.get<AdminInstitution[]>('/api/admin/institutions')).data,
  })
  const refresh = () => qc.invalidateQueries({ queryKey: ['admin'] })
  const verifyTeacher = useMutation({
    mutationFn: ({ id, verified }: { id: number; verified: boolean }) =>
      api.post(`/api/admin/teachers/${id}/verify`, null, { params: { verified } }),
    onSuccess: refresh,
  })
  const verifyInstitution = useMutation({
    mutationFn: ({ id, verified }: { id: number; verified: boolean }) =>
      api.post(`/api/admin/institutions/${id}/verify`, null, { params: { verified } }),
    onSuccess: refresh,
  })
  const setEnabled = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) =>
      api.post(`/api/admin/users/${id}/enabled`, null, { params: { enabled } }),
    onSuccess: refresh,
  })

  const s = stats.data
  return (
    <div className="space-y-5">
      <h1 className="text-xl font-semibold">Admin dashboard</h1>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-6">
        <Stat label="Teachers" value={s?.teachers} />
        <Stat label="Institutions" value={s?.institutions} />
        <Stat label="Posts" value={s?.posts} />
        <Stat label="Open jobs" value={s?.openJobs} />
        <Stat label="Unverified teachers" value={s?.unverifiedTeachers} />
        <Stat label="Unverified institutions" value={s?.unverifiedInstitutions} />
      </div>

      <div className="flex gap-2">
        {(['teachers', 'institutions'] as const).map((t) => (
          <button
            key={t}
            className={`rounded-full px-4 py-1.5 text-sm font-medium capitalize ${
              tab === t ? 'bg-slate-900 text-white' : 'border border-slate-300 bg-white'
            }`}
            onClick={() => setTab(t)}
          >
            {t}
          </button>
        ))}
      </div>

      <div className="card overflow-x-auto">
        {tab === 'teachers' && (
          <>
            {teachers.isLoading && <PageLoader />}
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-4 py-2">Teacher</th>
                  <th className="px-4 py-2">City</th>
                  <th className="px-4 py-2">Experience</th>
                  <th className="px-4 py-2 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {teachers.data?.map((t) => (
                  <tr key={t.userId}>
                    <td className="px-4 py-2">
                      <Link to={`/profile/${t.userId}`} className="inline-flex items-center gap-1 font-medium hover:underline">
                        {t.fullName}
                        {t.verified && <VerifiedBadge />}
                      </Link>
                      <div className="text-xs text-slate-500">{t.headline}</div>
                    </td>
                    <td className="px-4 py-2">{t.city ?? '–'}</td>
                    <td className="px-4 py-2">{t.yearsExperience} yrs</td>
                    <td className="space-x-2 whitespace-nowrap px-4 py-2 text-right">
                      <button
                        className={t.verified ? 'btn-ghost px-3 py-1 text-xs' : 'btn-primary px-3 py-1 text-xs'}
                        onClick={() => verifyTeacher.mutate({ id: t.userId, verified: !t.verified })}
                      >
                        {t.verified ? 'Unverify' : 'Verify'}
                      </button>
                      <button
                        className="btn-danger px-3 py-1 text-xs"
                        onClick={() => confirm(`Disable ${t.fullName}'s account?`) && setEnabled.mutate({ id: t.userId, enabled: false })}
                      >
                        Disable
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
        {tab === 'institutions' && (
          <>
            {institutions.isLoading && <PageLoader />}
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-4 py-2">Institution</th>
                  <th className="px-4 py-2">Type</th>
                  <th className="px-4 py-2">City</th>
                  <th className="px-4 py-2 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {institutions.data?.map((i) => (
                  <tr key={i.id} className={i.ownerEnabled ? '' : 'opacity-50'}>
                    <td className="px-4 py-2">
                      <Link to={`/institutions/${i.id}`} className="inline-flex items-center gap-1 font-medium hover:underline">
                        {i.name}
                        {i.verified && <VerifiedBadge />}
                      </Link>
                    </td>
                    <td className="px-4 py-2">{INSTITUTION_TYPES[i.type]}</td>
                    <td className="px-4 py-2">{i.city ?? '–'}</td>
                    <td className="space-x-2 whitespace-nowrap px-4 py-2 text-right">
                      <button
                        className={i.verified ? 'btn-ghost px-3 py-1 text-xs' : 'btn-primary px-3 py-1 text-xs'}
                        onClick={() => verifyInstitution.mutate({ id: i.id, verified: !i.verified })}
                      >
                        {i.verified ? 'Unverify' : 'Verify'}
                      </button>
                      <button
                        className={i.ownerEnabled ? 'btn-danger px-3 py-1 text-xs' : 'btn-outline px-3 py-1 text-xs'}
                        onClick={() => setEnabled.mutate({ id: i.ownerUserId, enabled: !i.ownerEnabled })}
                      >
                        {i.ownerEnabled ? 'Disable' : 'Enable'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </div>
    </div>
  )
}
