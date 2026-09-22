import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import type { Conversation } from '../api/types'

/** Opens (or creates) a 1:1 conversation with a user and navigates to it. */
export function useStartConversation() {
  const navigate = useNavigate()
  return useMutation({
    mutationFn: async (userId: number) => (await api.post<Conversation>(`/api/conversations/with/${userId}`)).data,
    onSuccess: (c) => navigate(`/messages/${c.id}`),
  })
}
