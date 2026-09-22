import { Link, useParams } from 'react-router-dom'
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Globe, MapPin, MessageSquare, Pencil, Plus } from 'lucide-react'
import { api, errorMessage, tokenStore } from '../api/client'
import type { Institution, JobSummary, Page, Post } from '../api/types'
import { JobCard } from '../components/Cards'
import PostCard from '../components/PostCard'
import { Avatar, ErrorBox, PageLoader, VerifiedBadge } from '../components/ui'
import { BOARDS, INSTITUTION_TYPES } from '../lib/labels'
import { useStartConversation } from '../lib/useStartConversation'

export default function InstitutionPage() {
  const { id } = useParams()
  const qc = useQueryClient()
  const message = useStartConversation()
  const key = ['institution', Number(id)]
  const { data: inst, isLoading, error } = useQuery({
    queryKey: key,
    queryFn: async () => (await api.get<Institution>(`/api/institutions/${id}`)).data,
  })
  const jobs = useQuery({
    queryKey: ['jobs', 'institution', Number(id)],
    queryFn: async () => (await api.get<Page<JobSummary>>('/api/jobs', { params: { institutionId: id, size: 10 } })).data,
  })
  const posts = useInfiniteQuery({
    queryKey: ['posts', 'user', inst?.ownerUserId],
    enabled: !!inst && !!tokenStore.get(),
    queryFn: async ({ pageParam }) =>
      (await api.get<Page<Post>>(`/api/posts/user/${inst!.ownerUserId}`, { params: { page: pageParam, size: 5 } })).data,
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  })
  const follow = useMutation({
    mutationFn: (following: boolean) =>
      following ? api.delete(`/api/institutions/${id}/follow`) : api.post(`/api/institutions/${id}/follow`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: key })
      qc.invalidateQueries({ queryKey: ['posts', 'feed'] })
    },
  })

  if (isLoading) return <PageLoader />
  if (error || !inst) return <ErrorBox message={errorMessage(error)} />

  return (
    <div className="mx-auto grid max-w-4xl gap-4">
      <section className="card overflow-hidden">
        <div className="h-28 bg-gradient-to-r from-emerald-600 via-teal-500 to-sky-400" />
        <div className="px-5 pb-5">
          <div className="-mt-10 flex items-end justify-between">
            <div className="rounded-xl bg-white p-1 shadow">
              <Avatar name={inst.name} src={inst.logoUrl} size="xl" square />
            </div>
            {inst.owner && (
              <Link to="/institution/edit" className="btn-ghost" aria-label="Edit page">
                <Pencil className="h-5 w-5" />
              </Link>
            )}
          </div>
          <h1 className="mt-3 flex items-center gap-2 text-2xl font-semibold">
            {inst.name}
            {inst.verified && <VerifiedBadge className="[&>svg]:h-5 [&>svg]:w-5" />}
          </h1>
          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-slate-500">
            <span>{INSTITUTION_TYPES[inst.institutionType]}</span>
            {inst.board && <span>{BOARDS[inst.board]}</span>}
            {(inst.city || inst.state) && (
              <span className="inline-flex items-center gap-1">
                <MapPin className="h-4 w-4" />
                {[inst.city, inst.state].filter(Boolean).join(', ')}
              </span>
            )}
            <span>{inst.followerCount} followers</span>
          </div>
          {inst.website && (
            <a href={inst.website} target="_blank" rel="noopener noreferrer" className="mt-1 inline-flex items-center gap-1 text-sm text-brand-700 hover:underline">
              <Globe className="h-4 w-4" /> {inst.website.replace(/^https?:\/\//, '')}
            </a>
          )}
          <div className="mt-4 flex flex-wrap gap-2">
            {!inst.owner && tokenStore.get() && (
              <>
                <button
                  className={inst.following ? 'btn-outline' : 'btn-primary'}
                  onClick={() => follow.mutate(inst.following)}
                  disabled={follow.isPending}
                >
                  {inst.following ? '✓ Following' : '+ Follow'}
                </button>
                <button className="btn-outline" onClick={() => message.mutate(inst.ownerUserId)}>
                  <MessageSquare className="h-4 w-4" /> Message
                </button>
              </>
            )}
            {inst.owner && (
              <Link to="/jobs/new" className="btn-primary">
                <Plus className="h-4 w-4" /> Post a job
              </Link>
            )}
          </div>
        </div>
      </section>

      {inst.about && (
        <section className="card p-5">
          <h2 className="mb-2 text-lg font-semibold">About</h2>
          <p className="whitespace-pre-wrap text-sm text-slate-700">{inst.about}</p>
        </section>
      )}

      <section className="space-y-3">
        <h2 className="px-1 text-lg font-semibold">Open positions ({inst.openJobCount})</h2>
        {jobs.data?.content.length === 0 && <p className="px-1 text-sm text-slate-500">No open positions right now.</p>}
        {jobs.data?.content.map((j) => <JobCard key={j.id} job={j} />)}
      </section>

      {(posts.data?.pages[0]?.content.length ?? 0) > 0 && (
        <section className="space-y-3">
          <h2 className="px-1 text-lg font-semibold">Posts</h2>
          {posts.data!.pages.flatMap((p) => p.content).map((p) => <PostCard key={p.id} post={p} />)}
          {posts.hasNextPage && (
            <button className="btn-ghost w-full" onClick={() => posts.fetchNextPage()}>Show more posts</button>
          )}
        </section>
      )}
    </div>
  )
}
