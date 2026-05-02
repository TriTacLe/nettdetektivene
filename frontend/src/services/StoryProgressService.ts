import apiClient from '@/services/ApiService'
import type { ApiResponse } from '@/types'

interface StoryProgress {
  step: number
}

async function getMine(): Promise<number> {
  const { data } = await apiClient.get<ApiResponse<StoryProgress>>('/profiles/me/story-progress')
  return data.data?.step ?? 0
}

async function advance(step: number): Promise<number> {
  const { data } = await apiClient.post<ApiResponse<StoryProgress>>(
    '/profiles/me/story-progress',
    { step },
  )
  return data.data?.step ?? step
}

export default { getMine, advance }
