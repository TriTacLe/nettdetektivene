import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig
} from 'axios';
import { AUTH_ENDPOINT_PATTERN, readSession, refreshAccessToken } from '@/lib/TokenRefresh'

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

function toUserMessage(status: number | undefined): string {
  if (status !== undefined && Object.prototype.hasOwnProperty.call(HTTP_ERROR_MESSAGES, status)) {
    return HTTP_ERROR_MESSAGES[status] as string
  }
  return 'En ukjent feil oppsto. Prøv igjen'
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
  const isAuthRequest = typeof original?.url === 'string' && AUTH_ENDPOINT_PATTERN.test(original.url)

  if (error.response?.status !== 401 || original._retry || isAuthRequest) {
    const wrapped = new Error(toUserMessage(error.response?.status)) as Error & { status?: number }
    wrapped.status = error.response?.status
    return Promise.reject(wrapped)
  }

  original._retry = true

  const newToken = await refreshAccessToken()
  original.headers = {
    ...original.headers,
    Authorization: `Bearer ${newToken}`
  }
  return apiClient(original)
},)

export default apiClient
