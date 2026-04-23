import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig
} from 'axios';

const SESSION_KEY = 'auth'

const HTTP_ERROR_MESSAGES: Record<number, string> = {
  400: 'Ugyldig forespørsel',
  401: 'Du er ikke innlogget',
  403: 'Du har ikke tilgang til denne ressursen',
  404: 'Ressursen ble ikke funnet',
  409: 'Konflikter med eksisterede data',
  422: 'Kunne ikke behandle dataene',
  500: 'En servergeil oppstod, prøv igjen senere',
  503: 'Tjenesten er midlertidig utilgjengelig'
} as const

interface AuthTokens {
  token: string
  refreshToken: string
}

interface PendingRequest {
  resolve: (token: string) => void
  reject: (reason: unknown) => void
}

function readSession(): Partial<AuthTokens> {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as Partial<AuthTokens>) : {}
  } catch {
    return {}
  }
}

function writeSessionTokens(tokens: AuthTokens): void {
  try {
    const current = readSession()
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({...current, ...tokens}))
  } catch {
    // sessionStorage unavailable
  }
}

function clearSession(): void {
  sessionStorage.removeItem(SESSION_KEY)
}

function toUserMessage(status: number | undefined): string {
  if (status !== undefined && Object.prototype.hasOwnProperty.call(HTTP_ERROR_MESSAGES, status)) {
    return HTTP_ERROR_MESSAGES[status] as string
  }
  return 'En ukjent feil oppsto. Prøv igjen'
}

let isRefreshing = false
const pendingQueue: PendingRequest[] = []

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
    const item: PendingRequest = { resolve, reject }
    pendingQueue.push(item)
  })
}

const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL as string,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10_000,
})

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const { token } = readSession()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use((response) => response, async (error: AxiosError) => {
  const original = error.config as AxiosRequestConfig & { _retry?: boolean }

  if (error.response?.status !== 401 || original._retry) {
    return Promise.reject(new Error(toUserMessage(error.response?.status)))
  }

  original._retry = true

  if (isRefreshing) {
    const newToken = await enqueue()
    original.headers = {
      ...original.headers,
      Authorization: `Bearer ${newToken}`
    }
    return apiClient(original)
  }

  isRefreshing = true
  const { refreshToken, token: expiredToken } = readSession()

  if (!refreshToken) {
    clearSession()
    window.location.href = '/login'
    return Promise.reject(new Error('Sesjonen er utløpt. Logg inn på nytt.'))
  }

  try {
    const { data } = await apiClient.post<AuthTokens>('/auth/refresh', { refreshToken, expiredToken })
    writeSessionTokens(data)
    flushQueue(null, data.token)
    original.headers = {
      ...original.headers,
      Authorization: `Bearer ${data.token}`
    }
    return apiClient(original)
  } catch (refreshError) {
    flushQueue(refreshError, null)
    clearSession()
    window.location.href = '/login'
    return Promise.reject(new Error('Sesjonen er utløpt. Logg inn på nytt. '))
  } finally {
    isRefreshing = false
  }
},)

export default apiClient
