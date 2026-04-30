import apiClient from '@/services/ApiService'
import type {
  ApiResponse,
  ClassroomTaskResponseDTO,
  MysteryReviewDTO,
  MysterySubmissionRequestDTO,
  MysterySubmissionResponseDTO,
  MysteryTaskCreateRequestDTO,
} from '@/types'

async function submitMystery(payload: MysterySubmissionRequestDTO): Promise<MysterySubmissionResponseDTO> {
  const { data } = await apiClient.post<ApiResponse<MysterySubmissionResponseDTO>>('/mysteries', payload)
  return data.data
}

async function getSubmissionsByClassroom(classroomId: string): Promise<MysterySubmissionResponseDTO[]> {
  const { data } = await apiClient.get<ApiResponse<MysterySubmissionResponseDTO[]>>(`/mysteries/classroom/${classroomId}`)
  return data.data
}

async function getPendingSubmissions(classroomId: string): Promise<MysterySubmissionResponseDTO[]> {
  const { data } = await apiClient.get<ApiResponse<MysterySubmissionResponseDTO[]>>(`/mysteries/classroom/${classroomId}/pending`)
  return data.data
}

async function getMySubmissions(classroomId: string): Promise<MysterySubmissionResponseDTO[]> {
  const { data } = await apiClient.get<ApiResponse<MysterySubmissionResponseDTO[]>>(`/mysteries/classroom/${classroomId}/mine`)
  return data.data
}

async function reviewSubmission(submissionId: string, payload: MysteryReviewDTO): Promise<MysterySubmissionResponseDTO> {
  const { data } = await apiClient.patch<ApiResponse<MysterySubmissionResponseDTO>>(`/mysteries/${submissionId}/review`, payload)
  return data.data
}

async function createTaskFromSubmission(
  submissionId: string,
  payload: MysteryTaskCreateRequestDTO,
): Promise<ClassroomTaskResponseDTO> {
  const { data } = await apiClient.post<ApiResponse<ClassroomTaskResponseDTO>>(`/mysteries/${submissionId}/task`, payload)
  return data.data
}

async function getFeaturedSubmission(classroomId: string): Promise<MysterySubmissionResponseDTO | null> {
  try {
    const { data } = await apiClient.get<ApiResponse<MysterySubmissionResponseDTO>>(`/mysteries/classroom/${classroomId}/featured`)
    return data.data
  } catch {
    return null
  }
}

export default {
  submitMystery,
  getSubmissionsByClassroom,
  getPendingSubmissions,
  getMySubmissions,
  reviewSubmission,
  createTaskFromSubmission,
  getFeaturedSubmission,
}
