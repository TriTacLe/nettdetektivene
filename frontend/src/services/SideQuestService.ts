import apiClient from '@/services/ApiService'
import type { ApiResponse } from '@/types'

export type SideQuestStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED'

export interface SideQuestState {
  key: string
  stage: number
  status: SideQuestStatus
}

async function getMine(): Promise<SideQuestState[]> {
  const { data } = await apiClient.get<ApiResponse<SideQuestState[]>>('/side-quests/me')
  return data.data ?? []
}

async function upsert(key: string, stage: number, status?: SideQuestStatus): Promise<SideQuestState> {
  const { data } = await apiClient.post<ApiResponse<SideQuestState>>(
    `/side-quests/me/${encodeURIComponent(key)}`,
    { stage, status },
  )
  return data.data
}

export default { getMine, upsert }
