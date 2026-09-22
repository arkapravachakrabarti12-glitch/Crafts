import { useNavigate } from 'react-router-dom'
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Bell, BadgeCheck, Briefcase, MessageCircle, ThumbsUp, UserPlus, Users } from 'lucide-react'
import { api } from '../api/client'
import type { Notification, NotificationType, Page } from '../api/types'
import { EmptyState, PageLoader } from '../components/ui'
import { timeAgo } from '../lib/labels'

const ICONS: Record<NotificationType, React.ReactNode> = {
  CONNECTION_REQUEST: <UserPlus className="h-5 w-5 text-brand-600" />,
  CONNECTION_ACCEPTED: <Users className="h-5 w-5 text-emerald-600" />,
  NEW_FOLLOWER: <Users className="h-5 w-5 text-sky-600" />,
  POST_LIKED: <ThumbsUp className="h-5 w-5 text-brand-600" />,
  POST_COMMENTED: <MessageCircle className="h-5 w-5 text-amber-600" />,
  JOB_APPLICATION: <Briefcase className="h-5 w-5 text-emerald-600" />,
  APPLICATION_STATUS: <Briefcase className="h-5 w-5 text-amber-600" />,
  NEW_MESSAGE: <MessageCircle className="h-5 w-5 text-brand-600" />,
  VERIFIED: <BadgeCheck className="h-5 w-5 text-brand-600" />,
}

export default function Notifications() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const q = useInfiniteQuery({
    queryKey: ['notifications'],
    queryFn: async ({ pageParam }) =>
      (await api.get<Page<Notification>>('/api/notifications', { params: { page: pageParam, size: 20 } })).data,
    initialPageParam: 0,
    getNextPageParam: (last) => (last.last ? undefined : last.page + 1),
  })
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['notifications'] })
    qc.invalidateQueries({ queryKey: ['notifications-unread'] })
  }
  const readAll = useMutation({ mutationFn: () => api.post('/api/notifications/read-all'), onSuccess: refresh })
  const read = useMutation({ mutationFn: (id: number) => api.post(`/api/notifications/${id}/read`), onSuccess: refresh })

  const items = q.data?.pages.flatMap((p) => p.content) ?? []
  const open = (n: Notification) => {
    if (!n.read) read.mutate(n.id)
    if (n.link) navigate(n.link)
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="card overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <h1 className="font-semibold">Notifications</h1>
          {items.some((n) => !n.read) && (
            <button className="text-sm font-medium text-brand-700 hover:underline" onClick={() => readAll.mutate()}>
              Mark all as read
            </button>
          )}
        </div>
        {q.isLoading && <PageLoader />}
        {!q.isLoading && items.length === 0 && (
          <div className="p-4">
            <EmptyState icon={<Bell className="h-10 w-10" />} title="You're all caught up" />
          </div>
        )}
        <ul className="divide-y divide-slate-100">
          {items.map((n) => (
            <li key={n.id}>
              <button
                onClick={() => open(n)}
                className={`flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-slate-50 ${n.read ? '' : 'bg-brand-50/60'}`}
              >
                <span className="mt-0.5">{ICONS[n.type]}</span>
                <span className="flex-1 text-sm">{n.message}</span>
                <span className="shrink-0 text-xs text-slate-400">{timeAgo(n.createdAt)}</span>
                {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-brand-600" />}
              </button>
            </li>
          ))}
        </ul>
        {q.hasNextPage && (
          <button className="btn-ghost w-full rounded-none border-t border-slate-100" onClick={() => q.fetchNextPage()}>
            Load more
          </button>
        )}
      </div>
    </div>
  )
}
