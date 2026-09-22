import type {
  ApplicationStatus,
  Board,
  EmploymentType,
  InstitutionType,
  PortfolioType,
  UserSummary,
} from '../api/types'

export const BOARDS: Record<Board, string> = {
  CBSE: 'CBSE',
  ICSE: 'ICSE',
  IB: 'IB',
  IGCSE: 'IGCSE / Cambridge',
  STATE_BOARD: 'State Board',
  UNIVERSITY: 'University',
  OTHER: 'Other',
}

export const INSTITUTION_TYPES: Record<InstitutionType, string> = {
  SCHOOL: 'School',
  COACHING_CENTRE: 'Coaching centre',
  COLLEGE: 'College',
  UNIVERSITY: 'University',
  EDTECH: 'EdTech',
  OTHER: 'Other',
}

export const EMPLOYMENT_TYPES: Record<EmploymentType, string> = {
  FULL_TIME: 'Full-time',
  PART_TIME: 'Part-time',
  CONTRACT: 'Contract',
  SUBSTITUTE: 'Substitute',
  TUTOR: 'Tutor',
}

export const PORTFOLIO_TYPES: Record<PortfolioType, string> = {
  DEMO_VIDEO: 'Demo lesson video',
  LESSON_PLAN: 'Lesson plan',
  STUDENT_RESULTS: 'Student results',
  CERTIFICATE: 'Certificate',
  ARTICLE: 'Article / blog',
  OTHER: 'Other',
}

export const APPLICATION_STATUS: Record<ApplicationStatus, { label: string; className: string }> = {
  APPLIED: { label: 'Applied', className: 'bg-slate-100 text-slate-700' },
  SHORTLISTED: { label: 'Shortlisted', className: 'bg-amber-100 text-amber-800' },
  REJECTED: { label: 'Not selected', className: 'bg-red-100 text-red-700' },
  HIRED: { label: 'Hired', className: 'bg-emerald-100 text-emerald-800' },
}

/** Grades are stored as 0..14: 0 = pre-primary, 1-12 = classes, 13 = UG, 14 = PG. */
export const GRADE_OPTIONS = Array.from({ length: 15 }, (_, g) => ({ value: g, label: gradeLabel(g) }))

export function gradeLabel(g: number): string {
  if (g === 0) return 'Pre-primary'
  if (g === 13) return 'Undergraduate'
  if (g === 14) return 'Postgraduate'
  return `Class ${g}`
}

export function gradeRange(from: number | null, to: number | null): string | null {
  if (from == null && to == null) return null
  if (from != null && to != null) {
    if (from === to) return gradeLabel(from)
    if (from >= 1 && to <= 12) return `Class ${from}–${to}`
    return `${gradeLabel(from)} – ${gradeLabel(to)}`
  }
  return gradeLabel((from ?? to) as number)
}

export function salaryRange(min: number | null, max: number | null): string | null {
  const fmt = (n: number) => '₹' + n.toLocaleString('en-IN')
  if (min != null && max != null) return `${fmt(min)} – ${fmt(max)} / month`
  if (min != null) return `From ${fmt(min)} / month`
  if (max != null) return `Up to ${fmt(max)} / month`
  return null
}

export function timeAgo(iso: string): string {
  const seconds = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000)
  if (seconds < 60) return 'just now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}d`
  if (days < 30) return `${Math.floor(days / 7)}w`
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

/** Where clicking on a user should go: their institution page or teacher profile. */
export function userLink(user: Pick<UserSummary, 'id' | 'institutionId' | 'role'>): string | null {
  if (user.institutionId) return `/institutions/${user.institutionId}`
  if (user.role === 'TEACHER') return `/profile/${user.id}`
  return null
}

export function options<T extends string>(record: Record<T, string>) {
  return (Object.keys(record) as T[]).map((value) => ({ value, label: record[value] }))
}
