import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageSquare, UserPlus, Users } from 'lucide-react'
import { api } from '../api/client'
import type { ConnectionItem, Page, UserSummary } from '../api/types'
import { Avatar, EmptyState, PageLoader, Pager, VerifiedBadge } from '../components/ui'
import { useStartConversation } from '../lib/useStartConversation'

function Person({ user, children }: { user: UserSummary; children?: React.ReactNode }) {
  return (
    <div className="flex items-center gap-3 py-3">
      <Avatar name={user.fullName} src={user.photoUrl} />
      <div className="min-w-0 flex-1">
        <Link to={`/profile/${user.id}`} className="inline-flex items-center gap-1 font-semibold hover:underline">
          {user.fullName}
          {user.verified && <VerifiedBadge />}
        </Link>
        {user.subtitle && <p className="truncate text-sm text-slate-500">{user.subtitle}</p>}
      </div>
      <div className="flex shrink-0 flex-wrap justify-end gap-2">{children}</div>
    </div>
  )
}

export default function Network() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const message = useStartConversation()
  const pending = useQuery({
    queryKey: ['pending'],
    queryFn: async () =>
      (await api.get<{ incoming: ConnectionItem[]; outgoing: ConnectionItem[] }>('/api/connections/pending')).data,
  })
  const connections = useQuery({
    queryKey: ['connections', page],
    queryFn: async () => (await api.get<Page<ConnectionItem>>('/api/connections', { params: { page, size: 20 } })).data,
  })
  const suggestions = useQuery({
    queryKey: ['suggestions', 'network'],
    queryFn: async () => (await api.get<UserSummary[]>('/api/connections/suggestions?limit=8')).data,
  })
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['pending'] })
    qc.invalidateQueries({ queryKey: ['connections'] })
    qc.invalidateQueries({ queryKey: ['suggestions'] })
    qc.invalidateQueries({ queryKey: ['posts'] })
  }
  const accept = useMutation({ mutationFn: (id: number) => api.post(`/api/connections/${id}/accept`), onSuccess: refresh })
  const remove = useMutation({ mutationFn: (id: number) => api.delete(`/api/connections/${id}`), onSuccess: refresh })
  const connect = useMutation({ mutationFn: (id: number) => api.post(`/api/connections/request/${id}`), onSuccess: refresh })

  const incoming = pending.data?.incoming ?? []
  const outgoing = pending.data?.outgoing ?? []

  return (
    <div className="mx-auto grid max-w-4xl gap-4">
      {incoming.length > 0 && (
        <section className="card px-5 py-3">
          <h2 className="py-2 font-semibold">Invitations ({incoming.length})</h2>
          <div className="divide-y divide-slate-100">
            {incoming.map((c) => (
              <Person key={c.connectionId} user={c.user}>
                <button className="btn-ghost" onClick={() => remove.mutate(c.connectionId)}>Ignore</button>
                <button className="btn-outline" onClick={() => accept.mutate(c.connectionId)}>Accept</button>
              </Person>
            ))}
          </div>
        </section>
      )}

      <section className="card px-5 py-3">
        <h2 className="py-2 font-semibold">
          My connections {connections.data ? `(${connections.data.totalElements})` : ''}
        </h2>
        {connections.isLoading && <PageLoader />}
        {connections.data?.content.length === 0 && (
          <EmptyState icon={<Users className="h-10 w-10" />} title="No connections yet">
            Connect with teachers you know, or find new ones below.
          </EmptyState>
        )}
        <div className="divide-y divide-slate-100">
          {connections.data?.content.map((c) => (
            <Person key={c.connectionId} user={c.user}>
              <button className="btn-outline" onClick={() => message.mutate(c.user.id)}>
                <MessageSquare className="h-4 w-4" /> Message
              </button>
            </Person>
          ))}
        </div>
        {connections.data && (
          <Pager page={page} totalPages={connections.data.totalPages} onChange={setPage} />
        )}
      </section>

      {(suggestions.data?.length ?? 0) > 0 && (
        <section className="card px-5 py-3">
          <h2 className="py-2 font-semibold">Teachers you may know</h2>
          <div className="grid gap-3 py-2 sm:grid-cols-2 lg:grid-cols-4">
            {suggestions.data!.map((u) => (
              <div key={u.id} className="flex flex-col items-center rounded-xl border border-slate-200 p-4 text-center">
                <Avatar name={u.fullName} src={u.photoUrl} size="lg" />
                <Link to={`/profile/${u.id}`} className="mt-2 font-semibold hover:underline">
                  {u.fullName}
                </Link>
                <p className="line-clamp-2 min-h-8 text-xs text-slate-500">{u.subtitle}</p>
                <button className="btn-outline mt-3 w-full" onClick={() => connect.mutate(u.id)} disabled={connect.isPending}>
                  <UserPlus className="h-4 w-4" /> Connect
                </button>
              </div>
            ))}
          </div>
        </section>
      )}

      {outgoing.length > 0 && (
        <section className="card px-5 py-3">
          <h2 className="py-2 font-semibold">Sent invitations ({outgoing.length})</h2>
          <div className="divide-y divide-slate-100">
            {outgoing.map((c) => (
              <Person key={c.connectionId} user={c.user}>
                <button className="btn-ghost" onClick={() => remove.mutate(c.connectionId)}>Withdraw</button>
              </Person>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
