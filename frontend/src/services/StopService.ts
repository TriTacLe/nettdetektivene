import apiClient from './ApiService'
import type { ApiResponse, StopTaskDTO } from '@/types'

export async function fetchStopTasks(stopId: string): Promise<StopTaskDTO[]> {
  const { data } = await apiClient.get<ApiResponse<StopTaskDTO[]>>(`/stops/${stopId}/tasks`)
  return data.data
}
