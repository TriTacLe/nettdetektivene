import apiClient from '@/services/ApiService'
import type { ApiResponse, NotebookEntryDTO, NotebookEntryRequest } from '@/types'

export async function getNotebookEntries(classroomId: string): Promise<NotebookEntryDTO[]> {
  const { data } = await apiClient.get<ApiResponse<NotebookEntryDTO[]>>(
    `/classrooms/${classroomId}/notebooks/me`
  )
  return data.data
}

export async function createNotebookEntry(
  classroomId: string,
  request: NotebookEntryRequest
): Promise<NotebookEntryDTO> {
  const { data } = await apiClient.post<ApiResponse<NotebookEntryDTO>>(
    `/classrooms/${classroomId}/notebooks`,
    request
  )
  return data.data
}

export async function updateNotebookEntry(
  classroomId: string,
  id: string,
  request: NotebookEntryRequest
): Promise<NotebookEntryDTO> {
  const { data } = await apiClient.patch<ApiResponse<NotebookEntryDTO>>(
    `/classrooms/${classroomId}/notebooks/${id}`,
    request
  )
  return data.data
}
