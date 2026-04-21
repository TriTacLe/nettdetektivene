import apiClient from './ApiService'
import type { ApiResponse, NewsChallenge, FotografenChallenge } from '@/types'

export async function fetchNewsChallenge(stopId: string, taskId: number): Promise<NewsChallenge> {
  const { data } = await apiClient.get<ApiResponse<NewsChallenge>>(
    `/stops/${stopId}/tasks/${taskId}/challenge`
  )
  return data.data
}

export async function fetchFotografenChallenge(stopId: string, taskId: number): Promise<FotografenChallenge> {
  const { data } = await apiClient.get<ApiResponse<FotografenChallenge>>(
    `/stops/${stopId}/tasks/${taskId}/challenge`
  )
  return data.data
}
