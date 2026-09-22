import { useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Briefcase, Calendar, IndianRupee, MapPin, Users } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { JobDetail as JobDetailT } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Logo } from '../components/Layout'
import { Avatar, ErrorBox, Field, Modal, PageLoader, VerifiedBadge } from '../components/ui'
import { APPLICATION_STATUS, BOARDS, EMPLOYMENT_TYPES, gradeRange, salaryRange, timeAgo } from '../lib/labels'

function ApplyModal({ jobId, onClose }: { jobId: number; onClose: () => void }) {
  const qc = useQueryClient()
  const [coverNote, setCoverNote] = useState('')
  const apply = useMutation({
    mutationFn: () => api.post(`/api/jobs/${jobId}/apply`, { coverNote }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['job', jobId] })
      qc.invalidateQueries({ queryKey: ['my-applications'] })
      qc.invalidateQueries({ queryKey: ['recommended-jobs'] })
      onClose()
    },
  })
  return (
    <Modal title="Apply for this job" onClose={onClose}>
      <p className="mb-3 text-sm text-slate-600">
        The school will see your TeachNet profile. Make sure your subjects, experience and portfolio are up to date.
      </p>
      <Field label="Cover note (optional)">
        <textarea
          className="input min-h-32"
          maxLength={3000}
          placeholder="Why are you a great fit for this role?"
          value={coverNote}
          onChange={(e) => setCoverNote(e.target.value)}
        />
      </Field>
      {apply.isError && <div className="mt-3"><ErrorBox message={errorMessage(apply.error)} /></div>}
      <div className="mt-4 flex justify-end gap-2">
        <button className="btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn-primary" onClick={() => apply.mutate()} disabled={apply.isPending}>
          Submit application
        </button>
      </div>
    </Modal>
  )
}

export default function JobDetail() {
  const { id } = useParams()
  const jobId = Number(id)
  const { user } = useAuth()
  const location = useLocation()
  const qc = useQueryClient()
  const [applying, setApplying] = useState(false)
  const { data, isLoading, error } = useQuery({
    queryKey: ['job', jobId],
    queryFn: async () => (await api.get<JobDetailT>(`/api/jobs/${jobId}`)).data,
  })
  const setStatus = useMutation({
    mutationFn: (status: 'OPEN' | 'CLOSED') => api.put(`/api/jobs/${jobId}/status`, { status }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['job', jobId] })
      qc.invalidateQueries({ queryKey: ['my-jobs'] })
    },
  })

  const content = (() => {
    if (isLoading) return <PageLoader />
    if (error || !data) return <ErrorBox message={errorMessage(error)} />
    const { job } = data
    const grades = gradeRange(job.gradeFrom, job.gradeTo)
    const salary = salaryRange(job.salaryMin, job.salaryMax)
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <section className="card p-6">
          <Link to={`/institutions/${job.institution.id}`} className="flex items-center gap-3">
            <Avatar name={job.institution.name} src={job.institution.logoUrl} square />
            <span className="inline-flex items-center gap-1 font-medium hover:underline">
              {job.institution.name}
              {job.institution.verified && <VerifiedBadge />}
            </span>
          </Link>
          <h1 className="mt-4 text-2xl font-semibold">{job.title}</h1>
          <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm text-slate-600">
            {job.city && <span className="inline-flex items-center gap-1"><MapPin className="h-4 w-4" /> {job.city}</span>}
            <span className="inline-flex items-center gap-1"><Briefcase className="h-4 w-4" /> {EMPLOYMENT_TYPES[job.employmentType]}</span>
            {salary && <span className="inline-flex items-center gap-1"><IndianRupee className="h-4 w-4" /> {salary}</span>}
            <span className="inline-flex items-center gap-1"><Calendar className="h-4 w-4" /> Posted {timeAgo(job.createdAt)}</span>
          </div>
          <div className="mt-3 flex flex-wrap gap-1.5">
            <span className="chip">{job.subject}</span>
            {job.board && <span className="chip">{BOARDS[job.board]}</span>}
            {grades && <span className="chip">{grades}</span>}
            {job.status === 'CLOSED' && <span className="chip bg-red-100 text-red-700">No longer accepting applications</span>}
          </div>

          <div className="mt-5 flex flex-wrap gap-2">
            {!user && (
              <Link to="/login" state={{ from: location.pathname }} className="btn-primary">
                Sign in to apply
              </Link>
            )}
            {data.canApply && (
              <button className="btn-primary" onClick={() => setApplying(true)}>
                Apply now
              </button>
            )}
            {data.myApplicationStatus && (
              <span className={`rounded-full px-3 py-1.5 text-sm font-medium ${APPLICATION_STATUS[data.myApplicationStatus].className}`}>
                Your application: {APPLICATION_STATUS[data.myApplicationStatus].label}
              </span>
            )}
            {data.canManage && (
              <>
                <Link to={`/jobs/${job.id}/applicants`} className="btn-primary">
                  <Users className="h-4 w-4" /> View applicants ({data.applicationCount ?? 0})
                </Link>
                <Link to={`/jobs/${job.id}/edit`} className="btn-outline">Edit</Link>
                <button
                  className={job.status === 'OPEN' ? 'btn-danger' : 'btn-outline'}
                  onClick={() => setStatus.mutate(job.status === 'OPEN' ? 'CLOSED' : 'OPEN')}
                  disabled={setStatus.isPending}
                >
                  {job.status === 'OPEN' ? 'Close job' : 'Reopen job'}
                </button>
              </>
            )}
          </div>
        </section>

        <section className="card p-6">
          <h2 className="mb-2 text-lg font-semibold">About the job</h2>
          <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-700">{data.description}</p>
        </section>
        {applying && <ApplyModal jobId={job.id} onClose={() => setApplying(false)} />}
      </div>
    )
  })()

  if (user) return content
  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
          <Logo />
          <Link to="/jobs" className="btn-ghost">All jobs</Link>
        </div>
      </header>
      <main className="px-3 py-5 sm:px-4">{content}</main>
    </div>
  )
}
