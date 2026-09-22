import axios, { AxiosError } from 'axios'

const TOKEN_KEY = 'teachnet.token'

export const tokenStore = {
  get(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set(token: string) {
    try {
      localStorage.setItem(TOKEN_KEY, token)
    } catch {
      /* storage unavailable (private mode); the session just won't persist */
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* ignore */
    }
  },
}

// Empty in local dev (Vite proxies /api); the Render URL in production.
const baseURL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

export const api = axios.create({ baseURL })

api.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let onUnauthorized: (() => void) | null = null

/** Registered by AuthProvider so an expired token logs the user out everywhere. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

api.interceptors.response.use(
  (res) => res,
  (error: AxiosError) => {
    if (error.response?.status === 401 && tokenStore.get()) {
      onUnauthorized?.()
    }
    return Promise.reject(error)
  },
)

interface ApiErrorBody {
  message?: string
  fieldErrors?: Record<string, string> | null
}

/** Turns any API error into a sentence that can be shown to the user. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiErrorBody | undefined
    if (body?.fieldErrors && Object.keys(body.fieldErrors).length > 0) {
      return Object.entries(body.fieldErrors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join(', ')
    }
    if (body?.message) return body.message
    if (!error.response) return 'Cannot reach the server. Is the backend running?'
  }
  return 'Something went wrong. Please try again.'
}
