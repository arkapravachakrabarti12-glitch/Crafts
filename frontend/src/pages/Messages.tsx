import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, MessageSquare, Send } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { Conversation, Message, Page } from '../api/types'
import { Avatar, EmptyState, ErrorBox, Spinner, VerifiedBadge } from '../components/ui'
import { timeAgo, userLink } from '../lib/labels'

function Thread({ conversation }: { conversation: Conversation }) {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [text, setText] = useState('')
  const bottom = useRef<HTMLDivElement>(null)
  const key = ['messages', conversation.id]
  // Polling keeps the thread fresh without WebSockets; good enough for an MVP.
  const { data, isLoading } = useQuery({
    queryKey: key,
    queryFn: async () =>
      (await api.get<Page<Message>>(`/api/conversations/${conversation.id}/messages`, { params: { size: 50 } })).data,
    refetchInterval: 5000,
  })
  const messages = [...(data?.content ?? [])].reverse()
  const lastId = messages[messages.length - 1]?.id

  useEffect(() => {
    bottom.current?.scrollIntoView({ block: 'end' })
    qc.invalidateQueries({ queryKey: ['messages-unread'] })
    qc.invalidateQueries({ queryKey: ['conversations'] })
  }, [lastId, qc])

  const send = useMutation({
    mutationFn: (body: string) => api.post(`/api/conversations/${conversation.id}/messages`, { body }),
    onSuccess: () => {
      setText('')
      qc.invalidateQueries({ queryKey: key })
      qc.invalidateQueries({ queryKey: ['conversations'] })
    },
  })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (text.trim()) send.mutate(text.trim())
  }
  const other = conversation.otherUser
  const link = userLink(other)

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center gap-3 border-b border-slate-200 px-4 py-3">
        <button className="btn-ghost px-1 md:hidden" onClick={() => navigate('/messages')} aria-label="Back">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <Avatar name={other.fullName} src={other.photoUrl} size="sm" square={!!other.institutionId} />
        <div className="min-w-0">
          {link ? (
            <Link to={link} className="inline-flex items-center gap-1 font-semibold hover:underline">
              {other.fullName}
              {other.verified && <VerifiedBadge />}
            </Link>
          ) : (
            <span className="font-semibold">{other.fullName}</span>
          )}
          {other.subtitle && <p className="truncate text-xs text-slate-500">{other.subtitle}</p>}
        </div>
      </div>
      <div className="flex-1 space-y-2 overflow-y-auto px-4 py-4">
        {isLoading && <Spinner />}
        {!isLoading && messages.length === 0 && (
          <p className="py-8 text-center text-sm text-slate-400">Say hello 👋</p>
        )}
        {messages.map((m) => (
          <div key={m.id} className={`flex ${m.mine ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[80%] rounded-2xl px-3 py-2 text-sm ${
                m.mine ? 'rounded-br-sm bg-brand-600 text-white' : 'rounded-bl-sm bg-slate-100 text-slate-800'
              }`}
            >
              <p className="whitespace-pre-wrap break-words">{m.body}</p>
              <p className={`mt-0.5 text-[10px] ${m.mine ? 'text-brand-100' : 'text-slate-400'}`}>{timeAgo(m.createdAt)}</p>
            </div>
          </div>
        ))}
        <div ref={bottom} />
      </div>
      {send.isError && <div className="px-4"><ErrorBox message={errorMessage(send.error)} /></div>}
      <form onSubmit={submit} className="flex gap-2 border-t border-slate-200 p-3">
        <input
          className="input rounded-full"
          placeholder="Write a message…"
          maxLength={4000}
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <button className="btn-primary px-3" disabled={!text.trim() || send.isPending} aria-label="Send">
          <Send className="h-4 w-4" />
        </button>
      </form>
    </div>
  )
}

export default function Messages() {
  const { conversationId } = useParams()
  const activeId = conversationId ? Number(conversationId) : null
  const { data, isLoading } = useQuery({
    queryKey: ['conversations'],
    queryFn: async () => (await api.get<Conversation[]>('/api/conversations')).data,
    refetchInterval: 15000,
  })
  const active = data?.find((c) => c.id === activeId)

  return (
    <div className="card grid h-[calc(100vh-7.5rem)] overflow-hidden md:grid-cols-[320px_minmax(0,1fr)]">
      <aside className={`border-r border-slate-200 ${activeId ? 'hidden md:block' : ''} overflow-y-auto`}>
        <h1 className="border-b border-slate-200 px-4 py-3 font-semibold">Messaging</h1>
        {isLoading && <div className="p-4"><Spinner /></div>}
        {data?.length === 0 && (
          <p className="p-4 text-sm text-slate-500">
            No conversations yet. Open a teacher's profile or an institution page and click "Message".
          </p>
        )}
        <ul>
          {data?.map((c) => (
            <li key={c.id}>
              <Link
                to={`/messages/${c.id}`}
                className={`flex gap-3 border-l-4 px-4 py-3 hover:bg-slate-50 ${
                  c.id === activeId ? 'border-brand-600 bg-brand-50/50' : 'border-transparent'
                }`}
              >
                <Avatar name={c.otherUser.fullName} src={c.otherUser.photoUrl} square={!!c.otherUser.institutionId} />
                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline justify-between gap-2">
                    <span className={`truncate text-sm ${c.unreadCount ? 'font-bold' : 'font-medium'}`}>
                      {c.otherUser.fullName}
                    </span>
                    <span className="shrink-0 text-[11px] text-slate-400">{timeAgo(c.updatedAt)}</span>
                  </div>
                  <p className={`truncate text-xs ${c.unreadCount ? 'font-semibold text-slate-800' : 'text-slate-500'}`}>
                    {c.lastMessage ?? 'No messages yet'}
                  </p>
                </div>
                {c.unreadCount > 0 && (
                  <span className="mt-1 h-5 min-w-5 rounded-full bg-brand-600 px-1.5 text-center text-[11px] font-bold leading-5 text-white">
                    {c.unreadCount}
                  </span>
                )}
              </Link>
            </li>
          ))}
        </ul>
      </aside>
      <section className={`${activeId ? '' : 'hidden md:block'} min-h-0`}>
        {active ? (
          <Thread key={active.id} conversation={active} />
        ) : (
          <div className="flex h-full items-center justify-center p-6">
            <EmptyState icon={<MessageSquare className="h-10 w-10" />} title="Select a conversation">
              {activeId && !isLoading ? 'This conversation could not be found.' : 'Choose a conversation from the list.'}
            </EmptyState>
          </div>
        )}
      </section>
    </div>
  )
}
