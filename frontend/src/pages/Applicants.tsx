import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageSquare, Users } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { Applicant, ApplicationStatus, JobDetail } from '../api/types'
import { TeacherCardView } from '../components/Cards'
import { EmptyState, ErrorBox, PageLoader } from '../components/ui'
import { APPLICATION_STATUS, timeAgo } from '../lib/labels'
import { useStartConversation } from '../lib/useStartConversation'

export default function Applicants() {
  const { id } = useParams()
  const jobId = Number(id)
  const qc = useQueryClient()
  const message = useStartConversation()
  const job = useQuery({
    queryKey: ['job', jobId],
    queryFn: async () => (await api.get<JobDetail>(`/api/jobs/${jobId}`)).data,
  })
  const { data, isLoading, error } = useQuery({
    queryKey: ['applicants', jobId],
    queryFn: async () => (await api.get<Applicant[]>(`/api/jobs/${jobId}/applications`)).data,
  })
  const update = useMutation({
    mutationFn: ({ applicationId, status }: { applicationId: number; status: ApplicationStatus }) =>
      api.put(`/api/applications/${applicationId}/status`, { status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['applicants', jobId] }),
  })

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div>
        <Link to={`/jobs/${jobId}`} className="text-sm text-brand-700 hover:underline">← Back to job</Link>
        <h1 className="mt-1 text-xl font-semibold">Applicants{job.data && ` for ${job.data.job.title}`}</h1>
      </div>
      {isLoading && <PageLoader />}
      {error && <ErrorBox message={errorMessage(error)} />}
      {data?.length === 0 && (
        <EmptyState icon={<Users className="h-10 w-10" />} title="No applications yet">
          Share the job link or search for teachers and message them directly.
        </EmptyState>
      )}
      {data?.map((a) => (
        <TeacherCardView
          key={a.applicationId}
          teacher={a.teacher}
          action={
            <div className="space-y-3">
              {a.coverNote && (
                <p className="whitespace-pre-wrap rounded-lg bg-slate-50 p-3 text-sm text-slate-700">{a.coverNote}</p>
              )}
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-xs text-slate-500">Applied {timeAgo(a.appliedAt)} ·</span>
                <select
                  className="input w-auto py-1 text-xs"
                  value={a.status}
                  onChange={(e) =>
                    update.mutate({ applicationId: a.applicationId, status: e.target.value as ApplicationStatus })
                  }
                >
                  {(Object.keys(APPLICATION_STATUS) as ApplicationStatus[]).map((s) => (
                    <option key={s} value={s}>{APPLICATION_STATUS[s].label}</option>
                  ))}
                </select>
                <button className="btn-outline px-3 py-1 text-xs" onClick={() => message.mutate(a.teacher.userId)}>
                  <MessageSquare className="h-3.5 w-3.5" /> Message
                </button>
              </div>
            </div>
          }
        />
      ))}
    </div>
  )
}
