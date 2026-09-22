import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, setUnauthorizedHandler, tokenStore } from '../api/client'
import type { AuthResponse, Me } from '../api/types'

interface AuthState {
  user: Me | null
  loading: boolean
  login: (email: string, password: string) => Promise<Me>
  register: (payload: RegisterPayload) => Promise<Me>
  logout: () => void
  refresh: () => Promise<void>
}

export interface RegisterPayload {
  email: string
  password: string
  fullName: string
  accountType: 'TEACHER' | 'INSTITUTION'
  institutionType?: string
  city?: string
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Me | null>(null)
  const [loading, setLoading] = useState(true)
  const queryClient = useQueryClient()

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    queryClient.clear()
  }, [queryClient])

  const refresh = useCallback(async () => {
    const { data } = await api.get<Me>('/api/auth/me')
    setUser(data)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!tokenStore.get()) {
      setLoading(false)
      return
    }
    refresh()
      .catch(() => logout())
      .finally(() => setLoading(false))
  }, [logout, refresh])

  const handleAuth = useCallback((data: AuthResponse) => {
    tokenStore.set(data.token)
    queryClient.clear()
    setUser(data.user)
    return data.user
  }, [queryClient])

  const login = useCallback(
    async (email: string, password: string) => {
      const { data } = await api.post<AuthResponse>('/api/auth/login', { email, password })
      return handleAuth(data)
    },
    [handleAuth],
  )

  const register = useCallback(
    async (payload: RegisterPayload) => {
      const { data } = await api.post<AuthResponse>('/api/auth/register', payload)
      return handleAuth(data)
    },
    [handleAuth],
  )

  const value = useMemo(
    () => ({ user, loading, login, register, logout, refresh }),
    [user, loading, login, register, logout, refresh],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

/** For pages that are only rendered behind RequireAuth. */
export function useMe(): Me {
  const { user } = useAuth()
  if (!user) throw new Error('useMe used outside an authenticated route')
  return user
}
