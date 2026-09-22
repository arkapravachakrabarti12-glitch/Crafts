import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Briefcase, Plus, Users } from 'lucide-react'
import { api } from '../api/client'
import type { MyJobRow } from '../api/types'
import { JobCard } from '../components/Cards'
import { EmptyState, PageLoader } from '../components/ui'

export default function MyJobs() {
  const { data, isLoading } = useQuery({
    queryKey: ['my-jobs'],
    queryFn: async () => (await api.get<MyJobRow[]>('/api/jobs/mine')).data,
  })
  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Your job postings</h1>
        <Link to="/jobs/new" className="btn-primary">
          <Plus className="h-4 w-4" /> Post a job
        </Link>
      </div>
      {isLoading && <PageLoader />}
      {data?.length === 0 && (
        <EmptyState icon={<Briefcase className="h-10 w-10" />} title="No jobs posted yet">
          Post your first opening. Teachers matching the subject and city will see it in their recommendations.
        </EmptyState>
      )}
      {data?.map(({ job, applicationCount }) => (
        <JobCard
          key={job.id}
          job={job}
          extra={
            <Link to={`/jobs/${job.id}/applicants`} className="btn-outline mt-3 px-3 py-1 text-xs">
              <Users className="h-3.5 w-3.5" /> {applicationCount} applicant{applicationCount === 1 ? '' : 's'}
            </Link>
          }
        />
      ))}
    </div>
  )
}
