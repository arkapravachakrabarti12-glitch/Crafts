import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageCircle, ThumbsUp, Trash2 } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { Comment, Page, Post, UserSummary } from '../api/types'
import { useMe } from '../auth/AuthContext'
import { timeAgo, userLink } from '../lib/labels'
import { Avatar, ErrorBox, Spinner, VerifiedBadge } from './ui'

function AuthorLine({ user, createdAt, compact = false }: { user: UserSummary; createdAt: string; compact?: boolean }) {
  const link = userLink(user)
  const name = (
    <span className="font-semibold text-slate-900 hover:underline">
      {user.fullName}
      {user.verified && <VerifiedBadge className="ml-1 align-[-2px]" />}
    </span>
  )
  return (
    <div className="flex min-w-0 items-start gap-3">
      <Avatar name={user.fullName} src={user.photoUrl} size={compact ? 'sm' : 'md'} square={!!user.institutionId} />
      <div className="min-w-0 leading-tight">
        {link ? <Link to={link}>{name}</Link> : name}
        {user.subtitle && <div className="truncate text-xs text-slate-500">{user.subtitle}</div>}
        <div className="text-xs text-slate-400">{timeAgo(createdAt)}</div>
      </div>
    </div>
  )
}

function Comments({ post }: { post: Post }) {
  const me = useMe()
  const qc = useQueryClient()
  const [text, setText] = useState('')
  const key = ['comments', post.id]
  const { data, isLoading } = useQuery({
    queryKey: key,
    queryFn: async () => (await api.get<Page<Comment>>(`/api/posts/${post.id}/comments`)).data,
  })
  const invalidate = () => {
    qc.invalidateQueries({ queryKey: key })
    qc.invalidateQueries({ queryKey: ['posts'] })
    qc.invalidateQueries({ queryKey: ['post', post.id] })
  }
  const add = useMutation({
    mutationFn: (content: string) => api.post(`/api/posts/${post.id}/comments`, { content }),
    onSuccess: () => {
      setText('')
      invalidate()
    },
  })
  const remove = useMutation({
    mutationFn: (id: number) => api.delete(`/api/posts/${post.id}/comments/${id}`),
    onSuccess: invalidate,
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (text.trim()) add.mutate(text.trim())
  }

  return (
    <div className="border-t border-slate-100 px-4 py-3">
      <form onSubmit={submit} className="mb-3 flex items-center gap-2">
        <Avatar name={me.summary.fullName} src={me.summary.photoUrl} size="sm" />
        <input
          className="input rounded-full"
          placeholder="Add a comment…"
          value={text}
          maxLength={1000}
          onChange={(e) => setText(e.target.value)}
        />
        <button className="btn-primary px-3 py-1.5" disabled={!text.trim() || add.isPending}>
          Post
        </button>
      </form>
      {add.isError && <ErrorBox message={errorMessage(add.error)} />}
      {isLoading && <Spinner />}
      <ul className="space-y-3">
        {data?.content.map((c) => (
          <li key={c.id} className="flex gap-2">
            <div className="min-w-0 flex-1 rounded-lg bg-slate-50 px-3 py-2">
              <div className="flex items-start justify-between gap-2">
                <AuthorLine user={c.author} createdAt={c.createdAt} compact />
                {c.canDelete && (
                  <button
                    className="text-slate-400 hover:text-red-600"
                    onClick={() => remove.mutate(c.id)}
                    aria-label="Delete comment"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                )}
              </div>
              <p className="mt-1 whitespace-pre-wrap text-sm">{c.content}</p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default function PostCard({ post, defaultOpen = false }: { post: Post; defaultOpen?: boolean }) {
  const qc = useQueryClient()
  const [showComments, setShowComments] = useState(defaultOpen)
  const [expanded, setExpanded] = useState(false)
  const [liked, setLiked] = useState(post.likedByMe)
  const [likes, setLikes] = useState(post.likeCount)

  const like = useMutation({
    mutationFn: (next: boolean) =>
      next ? api.post(`/api/posts/${post.id}/like`) : api.delete(`/api/posts/${post.id}/like`),
    onMutate: (next) => {
      setLiked(next)
      setLikes((n) => n + (next ? 1 : -1))
    },
    onError: (_e, next) => {
      setLiked(!next)
      setLikes((n) => n + (next ? -1 : 1))
    },
  })
  const remove = useMutation({
    mutationFn: () => api.delete(`/api/posts/${post.id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['posts'] }),
  })

  const long = post.content.length > 400
  const text = long && !expanded ? post.content.slice(0, 400) + '…' : post.content

  return (
    <article className="card">
      <div className="flex items-start justify-between gap-2 px-4 pt-4">
        <AuthorLine user={post.author} createdAt={post.createdAt} />
        {post.canDelete && (
          <button
            className="text-slate-400 hover:text-red-600"
            onClick={() => confirm('Delete this post?') && remove.mutate()}
            aria-label="Delete post"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        )}
      </div>
      <div className="px-4 py-3">
        <p className="whitespace-pre-wrap break-words text-[15px]">{text}</p>
        {long && (
          <button className="mt-1 text-sm font-medium text-slate-500 hover:underline" onClick={() => setExpanded((e) => !e)}>
            {expanded ? 'Show less' : 'See more'}
          </button>
        )}
      </div>
      {post.imageUrl && (
        <img src={post.imageUrl} alt="" className="max-h-[480px] w-full border-y border-slate-100 object-cover" />
      )}
      <div className="flex items-center justify-between px-4 py-2 text-xs text-slate-500">
        <span>{likes > 0 && `👍 ${likes}`}</span>
        <button className="hover:underline" onClick={() => setShowComments(true)}>
          {post.commentCount > 0 && `${post.commentCount} comment${post.commentCount === 1 ? '' : 's'}`}
        </button>
      </div>
      <div className="flex border-t border-slate-100 px-2 py-1">
        <button
          className={`btn-ghost flex-1 rounded-lg ${liked ? 'text-brand-700' : ''}`}
          onClick={() => like.mutate(!liked)}
          disabled={like.isPending}
        >
          <ThumbsUp className={`h-4 w-4 ${liked ? 'fill-brand-600' : ''}`} /> Like
        </button>
        <button className="btn-ghost flex-1 rounded-lg" onClick={() => setShowComments((s) => !s)}>
          <MessageCircle className="h-4 w-4" /> Comment
        </button>
      </div>
      {showComments && <Comments post={post} />}
    </article>
  )
}
