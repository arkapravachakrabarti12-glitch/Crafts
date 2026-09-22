import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Bell, Briefcase, GraduationCap, Home, MessageSquare, Search, Shield, Users } from 'lucide-react'
import { api } from '../api/client'
import { useAuth, useMe } from '../auth/AuthContext'
import { Avatar } from './ui'

function useCount(key: string, url: string) {
  return useQuery({
    queryKey: [key],
    queryFn: async () => (await api.get<{ count: number }>(url)).data.count,
    refetchInterval: 30_000,
  }).data
}

function NavItem({
  to,
  icon,
  label,
  badge,
  className = '',
}: {
  to: string
  icon: ReactNode
  label: string
  badge?: number
  className?: string
}) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `relative flex flex-col items-center px-2 py-1 text-[11px] sm:px-3 ${className} ${
          isActive ? 'text-slate-900' : 'text-slate-500 hover:text-slate-900'
        }`
      }
    >
      {({ isActive }) => (
        <>
          <span className="relative">
            {icon}
            {!!badge && badge > 0 && (
              <span className="absolute -right-2 -top-1.5 min-w-4 rounded-full bg-red-600 px-1 text-center text-[10px] font-bold leading-4 text-white">
                {badge > 99 ? '99+' : badge}
              </span>
            )}
          </span>
          <span className="hidden md:block">{label}</span>
          {isActive && <span className="absolute inset-x-1 -bottom-[9px] h-0.5 bg-slate-900" />}
        </>
      )}
    </NavLink>
  )
}

function MeMenu() {
  const me = useMe()
  const { logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const location = useLocation()

  useEffect(() => setOpen(false), [location.pathname])
  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  const profileLink =
    me.role === 'INSTITUTION' ? `/institutions/${me.summary.institutionId}` : me.role === 'TEACHER' ? `/profile/${me.id}` : null

  return (
    <div className="relative" ref={ref}>
      <button onClick={() => setOpen((o) => !o)} className="flex flex-col items-center px-2 text-[11px] text-slate-500">
        <Avatar name={me.summary.fullName} src={me.summary.photoUrl} size="sm" />
        <span className="hidden md:block">Me ▾</span>
      </button>
      {open && (
        <div className="absolute right-0 top-12 z-40 w-64 rounded-xl border border-slate-200 bg-white py-2 shadow-lg">
          <div className="flex items-center gap-3 border-b border-slate-100 px-4 pb-3">
            <Avatar name={me.summary.fullName} src={me.summary.photoUrl} />
            <div className="min-w-0">
              <div className="truncate font-semibold">{me.summary.fullName}</div>
              <div className="truncate text-xs text-slate-500">{me.email}</div>
            </div>
          </div>
          {profileLink && (
            <Link to={profileLink} className="block px-4 py-2 text-sm hover:bg-slate-50">
              {me.role === 'INSTITUTION' ? 'View institution page' : 'View profile'}
            </Link>
          )}
          {me.role === 'TEACHER' && (
            <Link to="/applications" className="block px-4 py-2 text-sm hover:bg-slate-50">
              My job applications
            </Link>
          )}
          {me.role === 'INSTITUTION' && (
            <>
              <Link to="/institution/edit" className="block px-4 py-2 text-sm hover:bg-slate-50">
                Edit institution page
              </Link>
              <Link to="/my-jobs" className="block px-4 py-2 text-sm hover:bg-slate-50">
                Manage job postings
              </Link>
            </>
          )}
          {me.role === 'ADMIN' && (
            <Link to="/admin" className="block px-4 py-2 text-sm hover:bg-slate-50">
              Admin dashboard
            </Link>
          )}
          <button
            className="block w-full border-t border-slate-100 px-4 py-2 text-left text-sm text-slate-600 hover:bg-slate-50"
            onClick={() => {
              logout()
              navigate('/')
            }}
          >
            Sign out
          </button>
        </div>
      )}
    </div>
  )
}

export function Logo() {
  return (
    <Link to="/" className="flex items-center gap-1.5 font-bold text-brand-700">
      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-700 text-white">
        <GraduationCap className="h-5 w-5" />
      </span>
      <span className="hidden text-lg sm:inline">TeachNet</span>
    </Link>
  )
}

export default function Layout() {
  const me = useMe()
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const notifications = useCount('notifications-unread', '/api/notifications/unread-count')
  const messages = useCount('messages-unread', '/api/conversations/unread-count')

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    navigate(`/teachers?q=${encodeURIComponent(q.trim())}`)
  }

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-14 max-w-6xl items-center gap-2 px-3 sm:px-4">
          <Logo />
          <form onSubmit={onSearch} className="relative ml-1 hidden min-w-0 flex-1 sm:block sm:max-w-xs">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Search teachers by name or subject"
              className="w-full rounded-md bg-slate-100 py-1.5 pl-9 pr-3 text-sm focus:bg-white focus:outline-none focus:ring-2 focus:ring-brand-200"
            />
          </form>
          <nav className="ml-auto flex items-center">
            <NavItem to="/feed" icon={<Home className="h-5 w-5" />} label="Home" />
            <NavItem to="/teachers" icon={<Search className="h-5 w-5" />} label="Search" className="sm:hidden" />
            {me.role === 'TEACHER' && <NavItem to="/network" icon={<Users className="h-5 w-5" />} label="Network" />}
            <NavItem
              to={me.role === 'INSTITUTION' ? '/my-jobs' : '/jobs'}
              icon={<Briefcase className="h-5 w-5" />}
              label="Jobs"
            />
            <NavItem to="/messages" icon={<MessageSquare className="h-5 w-5" />} label="Messages" badge={messages} />
            <NavItem to="/notifications" icon={<Bell className="h-5 w-5" />} label="Alerts" badge={notifications} />
            {me.role === 'ADMIN' && <NavItem to="/admin" icon={<Shield className="h-5 w-5" />} label="Admin" />}
            <MeMenu />
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-3 py-5 sm:px-4">
        <Outlet />
      </main>
    </div>
  )
}
