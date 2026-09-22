import { Link } from 'react-router-dom'
import { Briefcase, MapPin } from 'lucide-react'
import type { JobSummary, TeacherCard } from '../api/types'
import { BOARDS, EMPLOYMENT_TYPES, gradeRange, salaryRange, timeAgo } from '../lib/labels'
import { Avatar, VerifiedBadge } from './ui'

export function JobCard({ job, extra }: { job: JobSummary; extra?: React.ReactNode }) {
  const grades = gradeRange(job.gradeFrom, job.gradeTo)
  const salary = salaryRange(job.salaryMin, job.salaryMax)
  return (
    <div className="card flex gap-3 p-4 transition hover:border-brand-200">
      <Avatar name={job.institution.name} src={job.institution.logoUrl} square />
      <div className="min-w-0 flex-1">
        <Link to={`/jobs/${job.id}`} className="font-semibold text-brand-700 hover:underline">
          {job.title}
        </Link>
        <div className="flex items-center gap-1 text-sm text-slate-700">
          {job.institution.name}
          {job.institution.verified && <VerifiedBadge />}
        </div>
        <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
          {job.city && (
            <span className="inline-flex items-center gap-1">
              <MapPin className="h-3.5 w-3.5" /> {job.city}
            </span>
          )}
          <span className="inline-flex items-center gap-1">
            <Briefcase className="h-3.5 w-3.5" /> {EMPLOYMENT_TYPES[job.employmentType]}
          </span>
          {salary && <span>{salary}</span>}
        </div>
        <div className="mt-2 flex flex-wrap gap-1.5">
          <span className="chip">{job.subject}</span>
          {job.board && <span className="chip">{BOARDS[job.board]}</span>}
          {grades && <span className="chip">{grades}</span>}
          {job.status === 'CLOSED' && <span className="chip bg-red-100 text-red-700">Closed</span>}
        </div>
        <div className="mt-2 text-xs text-slate-400">Posted {timeAgo(job.createdAt)}</div>
        {extra}
      </div>
    </div>
  )
}

export function TeacherCardView({ teacher, action }: { teacher: TeacherCard; action?: React.ReactNode }) {
  return (
    <div className="card flex gap-3 p-4">
      <Avatar name={teacher.fullName} src={teacher.photoUrl} size="lg" />
      <div className="min-w-0 flex-1">
        <Link to={`/profile/${teacher.userId}`} className="inline-flex items-center gap-1 font-semibold hover:underline">
          {teacher.fullName}
          {teacher.verified && <VerifiedBadge />}
        </Link>
        {teacher.headline && <div className="text-sm text-slate-600">{teacher.headline}</div>}
        <div className="mt-1 flex flex-wrap gap-x-3 text-xs text-slate-500">
          {teacher.city && (
            <span className="inline-flex items-center gap-1">
              <MapPin className="h-3.5 w-3.5" /> {teacher.city}
            </span>
          )}
          <span>{teacher.yearsExperience} yrs experience</span>
          {teacher.openToWork && <span className="font-medium text-emerald-700">Open to work</span>}
        </div>
        {teacher.subjects.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {teacher.subjects.slice(0, 5).map((s) => (
              <span key={s} className="chip">
                {s}
              </span>
            ))}
          </div>
        )}
        {action && <div className="mt-3">{action}</div>}
      </div>
    </div>
  )
}
