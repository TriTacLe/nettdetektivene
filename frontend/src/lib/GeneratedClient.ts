import { createClient } from '@hey-api/client-fetch'

const SESSION_KEY = 'auth'

function gettoken(): string | undefined {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as { token?: string }).token : undefined
  } catch {
    return undefined
  }
}

export const generatedClient = createClient({
  baseUrl: import.meta.env.VITE_API_URL as string,
})

generatedClient.interceptors.request.use((request) => {
  const token = gettoken()
  if (token) {
    request.headers.set('Authorization', `Bearer ${token}`)
  }
  return request
})
