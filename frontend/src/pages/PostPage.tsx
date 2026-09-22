import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api, errorMessage } from '../api/client'
import type { Post } from '../api/types'
import PostCard from '../components/PostCard'
import { ErrorBox, PageLoader } from '../components/ui'

export default function PostPage() {
  const { id } = useParams()
  const { data, isLoading, error } = useQuery({
    queryKey: ['post', Number(id)],
    queryFn: async () => (await api.get<Post>(`/api/posts/${id}`)).data,
  })
  return (
    <div className="mx-auto max-w-2xl">
      {isLoading && <PageLoader />}
      {error && <ErrorBox message={errorMessage(error)} />}
      {data && <PostCard post={data} defaultOpen />}
    </div>
  )
}
