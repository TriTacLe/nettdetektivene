import { client } from '@/generated-client/client.gen'
import { AUTH_ENDPOINT_PATTERN, readSession, refreshAccessToken } from '@/lib/TokenRefresh'

client.setConfig({ baseUrl: '' })

const requestClones = new WeakMap<Request, Request>()

client.interceptors.request.use((request) => {
  const clone = request.clone()
  const { token } = readSession()
  if (token) request.headers.set('Authorization', `Bearer ${token}`)
  requestClones.set(request, clone)
  return request
})

client.interceptors.response.use(async (response, request) => {
  if (response.status !== 401 || AUTH_ENDPOINT_PATTERN.test(request.url)) {
    return response
  }

  const newToken = await refreshAccessToken()

  const originalClone = requestClones.get(request)
  requestClones.delete(request)
  if (!originalClone) return response

  const retryRequest = originalClone.clone()
  retryRequest.headers.set('Authorization', `Bearer ${newToken}`)
  return fetch(retryRequest)
})
