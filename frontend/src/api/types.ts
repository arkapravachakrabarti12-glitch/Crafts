export type Role = 'TEACHER' | 'INSTITUTION' | 'ADMIN'
export type Board = 'CBSE' | 'ICSE' | 'IB' | 'IGCSE' | 'STATE_BOARD' | 'UNIVERSITY' | 'OTHER'
export type InstitutionType = 'SCHOOL' | 'COACHING_CENTRE' | 'COLLEGE' | 'UNIVERSITY' | 'EDTECH' | 'OTHER'
export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'SUBSTITUTE' | 'TUTOR'
export type JobStatus = 'OPEN' | 'CLOSED'
export type ApplicationStatus = 'APPLIED' | 'SHORTLISTED' | 'REJECTED' | 'HIRED'
export type PortfolioType = 'DEMO_VIDEO' | 'LESSON_PLAN' | 'STUDENT_RESULTS' | 'CERTIFICATE' | 'ARTICLE' | 'OTHER'
export type ConnectionState = 'SELF' | 'NONE' | 'PENDING_SENT' | 'PENDING_RECEIVED' | 'CONNECTED'
export type NotificationType =
  | 'CONNECTION_REQUEST'
  | 'CONNECTION_ACCEPTED'
  | 'NEW_FOLLOWER'
  | 'POST_LIKED'
  | 'POST_COMMENTED'
  | 'JOB_APPLICATION'
  | 'APPLICATION_STATUS'
  | 'NEW_MESSAGE'
  | 'VERIFIED'

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface UserSummary {
  id: number
  fullName: string
  role: Role
  subtitle: string | null
  photoUrl: string | null
  verified: boolean
  institutionId: number | null
}

export interface Me {
  id: number
  email: string
  role: Role
  summary: UserSummary
}

export interface AuthResponse {
  token: string
  user: Me
}

export interface Subject {
  id: number
  subject: string
  board: Board
  gradeFrom: number
  gradeTo: number
}

export interface Qualification {
  id: number
  degree: string
  institute: string
  completionYear: number | null
  verified: boolean
}

export interface Experience {
  id: number
  title: string
  organization: string
  startYear: number
  endYear: number | null
  description: string | null
}

export interface PortfolioItem {
  id: number
  itemType: PortfolioType
  title: string
  url: string
  description: string | null
}

export interface Profile {
  userId: number
  fullName: string
  headline: string | null
  bio: string | null
  city: string | null
  state: string | null
  yearsExperience: number
  photoUrl: string | null
  openToWork: boolean
  verified: boolean
  subjects: Subject[]
  qualifications: Qualification[]
  experiences: Experience[]
  portfolio: PortfolioItem[]
  connectionCount: number
  connectionState: ConnectionState
  connectionId: number | null
}

export interface TeacherCard {
  userId: number
  fullName: string
  headline: string | null
  city: string | null
  yearsExperience: number
  photoUrl: string | null
  verified: boolean
  openToWork: boolean
  subjects: string[]
}

export interface InstitutionCard {
  id: number
  name: string
  institutionType: InstitutionType
  city: string | null
  logoUrl: string | null
  verified: boolean
}

export interface Institution {
  id: number
  ownerUserId: number
  name: string
  institutionType: InstitutionType
  board: Board | null
  city: string | null
  state: string | null
  about: string | null
  website: string | null
  logoUrl: string | null
  verified: boolean
  followerCount: number
  openJobCount: number
  following: boolean
  owner: boolean
}

export interface Post {
  id: number
  author: UserSummary
  content: string
  imageUrl: string | null
  likeCount: number
  commentCount: number
  likedByMe: boolean
  canDelete: boolean
  createdAt: string
}

export interface Comment {
  id: number
  author: UserSummary
  content: string
  canDelete: boolean
  createdAt: string
}

export interface ConnectionItem {
  connectionId: number
  user: UserSummary
  status: 'PENDING' | 'ACCEPTED'
  createdAt: string
}

export interface JobSummary {
  id: number
  title: string
  subject: string
  board: Board | null
  gradeFrom: number | null
  gradeTo: number | null
  employmentType: EmploymentType
  salaryMin: number | null
  salaryMax: number | null
  city: string | null
  status: JobStatus
  createdAt: string
  institution: InstitutionCard
}

export interface JobDetail {
  job: JobSummary
  description: string
  canManage: boolean
  canApply: boolean
  myApplicationStatus: ApplicationStatus | null
  applicationCount: number | null
}

export interface MyJobRow {
  job: JobSummary
  applicationCount: number
}

export interface Applicant {
  applicationId: number
  teacher: TeacherCard
  coverNote: string | null
  status: ApplicationStatus
  appliedAt: string
}

export interface MyApplication {
  applicationId: number
  job: JobSummary
  status: ApplicationStatus
  appliedAt: string
}

export interface Conversation {
  id: number
  otherUser: UserSummary
  lastMessage: string | null
  unreadCount: number
  updatedAt: string
}

export interface Message {
  id: number
  conversationId: number
  senderId: number
  body: string
  createdAt: string
  readAt: string | null
  mine: boolean
}

export interface Notification {
  id: number
  type: NotificationType
  message: string
  link: string | null
  read: boolean
  createdAt: string
}

export interface AdminStats {
  teachers: number
  institutions: number
  posts: number
  openJobs: number
  unverifiedTeachers: number
  unverifiedInstitutions: number
}

export interface AdminInstitution {
  id: number
  ownerUserId: number
  name: string
  type: InstitutionType
  city: string | null
  verified: boolean
  ownerEnabled: boolean
}
