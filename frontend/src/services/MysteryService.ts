import apiClient from '@/services/ApiService'
import type {
  ApiResponse,
  MysteryCompletionRequestDTO,
  MysteryCompletionResponseDTO,
  MysteryPlayDTO,
  MysteryReviewActionDTO,
  MysteryReviewDTO,
} from '@/types'

interface SubmissionFields {
  classroomId: string
  alternative1: string
  alternative2: string
  alternative3: string
  correctIndex: number
}

async function submitMystery(payload: SubmissionFields, image: File): Promise<MysteryReviewDTO> {
  const formData = new FormData()
  formData.append(
    'submission',
    new Blob([JSON.stringify(payload)], { type: 'application/json' }),
  )
  formData.append('image', image)
  const { data } = await apiClient.post<ApiResponse<MysteryReviewDTO>>('/mysteries', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data.data
}

async function getPlayList(classroomId: string): Promise<MysteryPlayDTO[]> {
  const { data } = await apiClient.get<ApiResponse<MysteryPlayDTO[]>>(
    `/mysteries/classroom/${classroomId}/play`,
  )
  return data.data
}

async function getMysteryForPlay(submissionId: string): Promise<MysteryPlayDTO> {
  const { data } = await apiClient.get<ApiResponse<MysteryPlayDTO>>(
    `/mysteries/${submissionId}/play`,
  )
  return data.data
}

async function completeMystery(
  submissionId: string,
  payload: MysteryCompletionRequestDTO,
): Promise<MysteryCompletionResponseDTO> {
  const { data } = await apiClient.post<ApiResponse<MysteryCompletionResponseDTO>>(
    `/mysteries/${submissionId}/completion`,
    payload,
  )
  return data.data
}

async function getSubmissionsByClassroom(classroomId: string): Promise<MysteryReviewDTO[]> {
  const { data } = await apiClient.get<ApiResponse<MysteryReviewDTO[]>>(
    `/mysteries/classroom/${classroomId}`,
  )
  return data.data
}

async function getPendingSubmissions(classroomId: string): Promise<MysteryReviewDTO[]> {
  const { data } = await apiClient.get<ApiResponse<MysteryReviewDTO[]>>(
    `/mysteries/classroom/${classroomId}/pending`,
  )
  return data.data
}

async function getMySubmissions(classroomId: string): Promise<MysteryReviewDTO[]> {
  const { data } = await apiClient.get<ApiResponse<MysteryReviewDTO[]>>(
    `/mysteries/classroom/${classroomId}/mine`,
  )
  return data.data
}

async function reviewSubmission(
  submissionId: string,
  payload: MysteryReviewActionDTO,
): Promise<MysteryReviewDTO> {
  const { data } = await apiClient.patch<ApiResponse<MysteryReviewDTO>>(
    `/mysteries/${submissionId}/review`,
    payload,
  )
  return data.data
}

export default {
  submitMystery,
  getPlayList,
  getMysteryForPlay,
  completeMystery,
  getSubmissionsByClassroom,
  getPendingSubmissions,
  getMySubmissions,
  reviewSubmission,
}
