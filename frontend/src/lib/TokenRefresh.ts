const SESSION_KEY = 'auth'
const AUTH_ENDPOINT_PATTERN = /\/auth\/(login|refresh|register|feide-login|feide)\b/
const REFRESH_URL = (import.meta.env.VITE_API_URL as string) + '/auth/refresh'

interface AuthTokens {
  token: string
  refreshToken: string
}

interface PendingRequest {
  resolve: (token: string) => void
  reject: (reason: unknown) => void
}

let isRefreshing = false
const pendingQueue: PendingRequest[] = []

function readSession(): Partial<AuthTokens> {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as Partial<AuthTokens>) : {}
  } catch {
    return {}
  }
}

function flushQueue(error: unknown, token: string | null): void {
  const drained = pendingQueue.splice(0, pendingQueue.length)
  for (const item of drained) {
    if (error !== null && error !== undefined) {
      item.reject(error)
    } else if (token !== null) {
      item.resolve(token)
    }
  }
}

function enqueue(): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    pendingQueue.push({ resolve, reject })
  })
}

async function doRefresh(): Promise<string> {
  const { refreshToken, token: expiredToken } = readSession()

  if (!refreshToken) {
    throw new Error('No refresh token')
  }

  const response = await fetch(REFRESH_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken, expiredToken }),
  })

  if (!response.ok) {
    throw new Error(`Refresh failed: ${response.status}`)
  }

  const data = (await response.json()) as AuthTokens

  const current = JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? '{}')
  sessionStorage.setItem(SESSION_KEY, JSON.stringify({ ...current, ...data }))

  const { useAuthStore } = await import('@/stores/AuthStore')
  const authStore = useAuthStore()
  authStore.token = data.token
  authStore.refreshToken = data.refreshToken

  return data.token
}

export { AUTH_ENDPOINT_PATTERN, readSession }

export async function refreshAccessToken(): Promise<string> {
  if (isRefreshing) {
    return enqueue()
  }

  isRefreshing = true
  try {
    const newToken = await doRefresh()
    flushQueue(null, newToken)
    return newToken
  } catch (error) {
    flushQueue(error, null)
    sessionStorage.removeItem(SESSION_KEY)
    window.location.href = '/start'
    throw error
  } finally {
    isRefreshing = false
  }
}
