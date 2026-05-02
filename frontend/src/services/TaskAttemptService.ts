import apiClient from '@/services/ApiService'
import type {
  ApiResponse,
  TaskAnswers,
  TaskAttemptRequestDTO,
  TaskAttemptResponseDTO,
} from '@/types'

async function submitAttempt(
  taskId: string,
  classroomId: string,
  answers: TaskAnswers,
): Promise<TaskAttemptResponseDTO> {
  const payload: TaskAttemptRequestDTO = { taskId, classroomId, answers }
  const { data } = await apiClient.post<ApiResponse<TaskAttemptResponseDTO>>('/attempts', payload)
  return data.data
}

async function getMyPassedTaskIds(): Promise<string[]> {
  const { data } = await apiClient.get<ApiResponse<string[]>>('/attempts/me/passed')
  return data.data ?? []
}

export default { submitAttempt, getMyPassedTaskIds }
