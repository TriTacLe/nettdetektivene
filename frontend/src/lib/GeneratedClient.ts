import { client } from '@/generated-client/client.gen'
import { AUTH_ENDPOINT_PATTERN, readSession, refreshAccessToken } from '@/lib/TokenRefresh'

client.setConfig({ baseUrl: '' })

client.interceptors.request.use((request) => {
  const { token } = readSession()
  if (token) request.headers.set('Authorization', `Bearer ${token}`)
  return request
})

client.interceptors.response.use(async (response, request) => {
  if (response.status !== 401 || AUTH_ENDPOINT_PATTERN.test(request.url)) {
    return response
  }

  const newToken = await refreshAccessToken()

  const retryRequest = new Request(request, {
    headers: new Headers(request.headers),
  })
  retryRequest.headers.set('Authorization', `Bearer ${newToken}`)
  return fetch(retryRequest)
})
