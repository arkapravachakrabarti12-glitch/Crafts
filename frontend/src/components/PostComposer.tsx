import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ImageIcon } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import { useMe } from '../auth/AuthContext'
import { Avatar, ErrorBox } from './ui'

export default function PostComposer() {
  const me = useMe()
  const qc = useQueryClient()
  const [content, setContent] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [showImage, setShowImage] = useState(false)

  const create = useMutation({
    mutationFn: () => api.post('/api/posts', { content: content.trim(), imageUrl: imageUrl.trim() || null }),
    onSuccess: () => {
      setContent('')
      setImageUrl('')
      setShowImage(false)
      qc.invalidateQueries({ queryKey: ['posts'] })
    },
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (content.trim()) create.mutate()
  }

  return (
    <form onSubmit={submit} className="card p-4">
      <div className="flex gap-3">
        <Avatar name={me.summary.fullName} src={me.summary.photoUrl} square={!!me.summary.institutionId} />
        <textarea
          className="input min-h-[72px] resize-y"
          placeholder={
            me.role === 'INSTITUTION'
              ? 'Share news, achievements or openings at your institution…'
              : 'Share a teaching idea, a classroom win, or a question…'
          }
          value={content}
          maxLength={3000}
          onChange={(e) => setContent(e.target.value)}
        />
      </div>
      {showImage && (
        <input
          className="input mt-3"
          type="url"
          placeholder="Image URL (https://…)"
          value={imageUrl}
          onChange={(e) => setImageUrl(e.target.value)}
        />
      )}
      {create.isError && (
        <div className="mt-3">
          <ErrorBox message={errorMessage(create.error)} />
        </div>
      )}
      <div className="mt-3 flex items-center justify-between">
        <button type="button" className="btn-ghost px-2" onClick={() => setShowImage((s) => !s)}>
          <ImageIcon className="h-4 w-4 text-sky-600" /> Image
        </button>
        <button className="btn-primary" disabled={!content.trim() || create.isPending}>
          {create.isPending ? 'Posting…' : 'Post'}
        </button>
      </div>
    </form>
  )
}
