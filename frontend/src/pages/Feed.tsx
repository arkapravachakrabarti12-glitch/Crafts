import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Newspaper, UserPlus } from 'lucide-react'
import { api } from '../api/client'
import type { JobSummary, Page, Post, UserSummary } from '../api/types'
import { useMe } from '../auth/AuthContext'
import PostCard from '../components/PostCard'
import PostComposer from '../components/PostComposer'
import { Avatar, EmptyState, PageLoader, Spinner, VerifiedBadge } from '../components/ui'
import { userLink } from '../lib/labels'

type Scope = 'NETWORK' | 'ALL'

function ProfileSidebar() {
  const me = useMe()
  const link = userLink(me.summary) ?? '#'
  return (
    <aside className="card overflow-hidden">
      <div className="h-14 bg-gradient-to-r from-brand-600 to-sky-500" />
      <div className="-mt-7 px-4 pb-4 text-center">
        <div className="flex justify-center">
          <div className="rounded-full ring-4 ring-white">
            <Avatar name={me.summary.fullName} src={me.summary.photoUrl} size="lg" square={!!me.summary.institutionId} />
          </div>
        </div>
        <Link to={link} className="mt-2 inline-flex items-center gap-1 font-semibold hover:underline">
          {me.summary.fullName}
          {me.summary.verified && <VerifiedBadge />}
        </Link>
        {me.summary.subtitle && <p className="text-xs text-slate-500">{me.summary.subtitle}</p>}
      </div>
      <nav className="border-t border-slate-100 py-2 text-sm">
        {me.role === 'TEACHER' && (
          <>
            <Link to="/network" className="block px-4 py-1.5 hover:bg-slate-50">My network</Link>
            <Link to="/applications" className="block px-4 py-1.5 hover:bg-slate-50">My applications</Link>
            <Link to="/jobs" className="block px-4 py-1.5 hover:bg-slate-50">Find jobs</Link>
          </>
        )}
        {me.role === 'INSTITUTION' && (
          <>
            <Link to="/jobs/new" className="block px-4 py-1.5 hover:bg-slate-50">Post a job</Link>
            <Link to="/my-jobs" className="block px-4 py-1.5 hover:bg-slate-50">Manage jobs</Link>
            <Link to="/teachers" className="block px-4 py-1.5 hover:bg-slate-50">Search teachers</Link>
          </>
        )}
        {me.role === 'ADMIN' && (
          <Link to="/admin" className="block px-4 py-1.5 hover:bg-slate-50">Admin dashboard</Link>
        )}
      </nav>
    </aside>
  )
}

function Suggestions() {
  const qc = useQueryClient()
  const { data } = useQuery({
    queryKey: ['suggestions'],
    queryFn: async () => (await api.get<UserSummary[]>('/api/connections/suggestions?limit=5')).data,
  })
  const [sent, setSent] = useState<number[]>([])
  const connect = useMutation({
    mutationFn: (id: number) => api.post(`/api/connections/request/${id}`),
    onSuccess: (_d, id) => {
      setSent((s) => [...s, id])
      qc.invalidateQueries({ queryKey: ['pending'] })
    },
  })
  if (!data || data.length === 0) return null
  return (
    <div className="card p-4">
      <h3 className="mb-3 font-semibold">Teachers you may know</h3>
      <ul className="space-y-3">
        {data.map((u) => (
          <li key={u.id} className="flex items-start gap-2">
            <Avatar name={u.fullName} src={u.photoUrl} size="sm" />
            <div className="min-w-0 flex-1">
              <Link to={`/profile/${u.id}`} className="block truncate text-sm font-semibold hover:underline">
                {u.fullName}
              </Link>
              {u.subtitle && <p className="line-clamp-2 text-xs text-slate-500">{u.subtitle}</p>}
              <button
                className="btn-outline mt-1 px-3 py-1 text-xs"
                disabled={sent.includes(u.id) || connect.isPending}
                onClick={() => connect.mutate(u.id)}
              >
                <UserPlus className="h-3.5 w-3.5" /> {sent.includes(u.id) ? 'Pending' : 'Connect'}
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}

function RecommendedJobs() {
  const { data } = useQuery({
    queryKey: ['recommended-jobs'],
    queryFn: async () => (await api.get<JobSummary[]>('/api/jobs/recommended?limit=4')).data,
  })
  if (!data || data.length === 0) return null
  return (
    <div className="card p-4">
      <h3 className="mb-3 font-semibold">Jobs for you</h3>
      <ul className="space-y-3">
        {data.map((j) => (
          <li key={j.id}>
            <Link to={`/jobs/${j.id}`} className="text-sm font-semibold text-brand-700 hover:underline">
              {j.title}
            </Link>
            <p className="text-xs text-slate-500">
              {j.institution.name}
              {j.city && ` · ${j.city}`}
            </p>
          </li>
        ))}
      </ul>
      <Link to="/jobs" className="mt-3 block text-sm font-medium text-slate-600 hover:underline">
        See all jobs →
      </Link>
    </div>
  )
}

export default function Feed() {
  const me = useMe()
  const [scope, setScope] = useState<Scope>('NETWORK')
  const query = useInfiniteQuery({
    queryKey: ['posts', 'feed', scope],
    queryFn: async ({ pageParam }) =>
      (await api.get<Page<Post>>('/api/posts/feed', { params: { scope, page: pageParam, size: 10 } })).data,
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  })
  const posts = query.data?.pages.flatMap((p) => p.content) ?? []

  return (
    <div className="grid gap-5 lg:grid-cols-[225px_minmax(0,1fr)_300px]">
      <div className="hidden lg:block">
        <div className="sticky top-20">
          <ProfileSidebar />
        </div>
      </div>

      <div className="space-y-4">
        <PostComposer />
        <div className="flex items-center gap-2 text-sm">
          <span className="text-slate-500">Show:</span>
          {(['NETWORK', 'ALL'] as Scope[]).map((s) => (
            <button
              key={s}
              onClick={() => setScope(s)}
              className={`rounded-full px-3 py-1 font-medium ${
                scope === s ? 'bg-emerald-700 text-white' : 'border border-slate-300 bg-white text-slate-600'
              }`}
            >
              {s === 'NETWORK' ? 'My network' : 'Everyone'}
            </button>
          ))}
        </div>
        {query.isLoading && <PageLoader />}
        {!query.isLoading && posts.length === 0 && (
          <EmptyState icon={<Newspaper className="h-10 w-10" />} title="Your feed is quiet">
            {scope === 'NETWORK' ? (
              <>
                Connect with teachers or follow institutions to see their posts here, or{' '}
                <button className="font-medium text-brand-700 hover:underline" onClick={() => setScope('ALL')}>
                  see what everyone is sharing
                </button>
                .
              </>
            ) : (
              'Be the first to share something!'
            )}
          </EmptyState>
        )}
        {posts.map((p) => (
          <PostCard key={p.id} post={p} />
        ))}
        {query.hasNextPage && (
          <div className="flex justify-center">
            <button className="btn-outline" onClick={() => query.fetchNextPage()} disabled={query.isFetchingNextPage}>
              {query.isFetchingNextPage ? <Spinner className="h-4 w-4" /> : 'Load more'}
            </button>
          </div>
        )}
      </div>

      <div className="hidden space-y-4 lg:block">
        {me.role === 'TEACHER' && <RecommendedJobs />}
        {me.role === 'TEACHER' && <Suggestions />}
        {me.role !== 'TEACHER' && (
          <div className="card p-4 text-sm text-slate-600">
            <h3 className="mb-1 font-semibold text-slate-800">Looking for teachers?</h3>
            Search teachers by subject, board and city.
            <Link to="/teachers" className="btn-outline mt-3 w-full">
              Search teachers
            </Link>
          </div>
        )}
      </div>
    </div>
  )
}
