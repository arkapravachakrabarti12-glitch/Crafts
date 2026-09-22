import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { FileText } from 'lucide-react'
import { api } from '../api/client'
import type { MyApplication } from '../api/types'
import { JobCard } from '../components/Cards'
import { EmptyState, PageLoader } from '../components/ui'
import { APPLICATION_STATUS, timeAgo } from '../lib/labels'

export default function MyApplications() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['my-applications'],
    queryFn: async () => (await api.get<MyApplication[]>('/api/applications/mine')).data,
  })
  const withdraw = useMutation({
    mutationFn: (id: number) => api.delete(`/api/applications/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['my-applications'] }),
  })
  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <h1 className="text-xl font-semibold">My job applications</h1>
      {isLoading && <PageLoader />}
      {data?.length === 0 && (
        <EmptyState icon={<FileText className="h-10 w-10" />} title="You haven't applied to any jobs yet">
          <Link to="/jobs" className="font-medium text-brand-700 hover:underline">Browse open teaching jobs →</Link>
        </EmptyState>
      )}
      {data?.map((a) => (
        <JobCard
          key={a.applicationId}
          job={a.job}
          extra={
            <div className="mt-3 flex flex-wrap items-center gap-2 text-xs">
              <span className={`rounded-full px-2.5 py-1 font-medium ${APPLICATION_STATUS[a.status].className}`}>
                {APPLICATION_STATUS[a.status].label}
              </span>
              <span className="text-slate-500">Applied {timeAgo(a.appliedAt)}</span>
              {a.status === 'APPLIED' && (
                <button
                  className="ml-auto text-slate-500 hover:text-red-600 hover:underline"
                  onClick={() => confirm('Withdraw this application?') && withdraw.mutate(a.applicationId)}
                >
                  Withdraw
                </button>
              )}
            </div>
          }
        />
      ))}
    </div>
  )
}
