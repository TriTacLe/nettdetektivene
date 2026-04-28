import { client } from '@/generated-client/client.gen'

const SESSION_KEY = 'auth'

function getToken(): string | undefined {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as { token?: string }).token : undefined
  } catch {
    return undefined
  }
}

client.setConfig({ baseUrl: '' })

client.interceptors.request.use((request) => {
  const token = getToken()
  if (token) request.headers.set('Authorization', `Bearer ${token}`)
  return request
})
