import apiClient from '@/services/ApiService'
import type { ApiResponse } from '@/types'
import type { UserAchievementResponseDto } from '@/generated-client/types.gen'

async function awardSecret(triggerRef: string): Promise<UserAchievementResponseDto> {
  const { data } = await apiClient.post<ApiResponse<UserAchievementResponseDto>>(
    `/achievements/secret/${encodeURIComponent(triggerRef)}`,
  )
  return data.data
}

export default { awardSecret }
