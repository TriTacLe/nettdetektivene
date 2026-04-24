import apiClient from '@/services/ApiService'
import type { ApiResponse, TaskAttemptRequestDTO, TaskAttemptResponseDTO } from '@/types'

async function submitAttempt(
  taskId: string,
  classroomId: string,
  score: number,
): Promise<TaskAttemptResponseDTO> {
  const payload: TaskAttemptRequestDTO = { taskId, classroomId, score }
  const { data } = await apiClient.post<ApiResponse<TaskAttemptResponseDTO>>('/attempts', payload)
  return data.data
}

export default { submitAttempt }
