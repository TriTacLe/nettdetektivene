import { client } from '@/generated-client/client.gen'
import { AUTH_ENDPOINT_PATTERN, readSession, refreshAccessToken } from '@/lib/TokenRefresh'

client.setConfig({ baseUrl: '' })

let pendingRetryClone: Request | null = null

client.interceptors.request.use((request) => {
  pendingRetryClone = request.clone()
  const { token } = readSession()
  if (token) request.headers.set('Authorization', `Bearer ${token}`)
  return request
})

client.interceptors.response.use(async (response, request) => {
  if (response.status !== 401 || AUTH_ENDPOINT_PATTERN.test(request.url)) {
    return response
  }

  const newToken = await refreshAccessToken()

  const retryRequest = pendingRetryClone!.clone()
  retryRequest.headers.set('Authorization', `Bearer ${newToken}`)
  return fetch(retryRequest)
})
