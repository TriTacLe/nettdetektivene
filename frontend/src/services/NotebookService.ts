import apiClient from '@/services/ApiService'
import type { NotebookEntryDTO, NotebookEntryRequest } from '@/types'

export async function getNotebookEntries(): Promise<NotebookEntryDTO[]> {
  const { data } = await apiClient.get<NotebookEntryDTO[]>('/notebook')
  return data
}

export async function createNotebookEntry(request: NotebookEntryRequest): Promise<NotebookEntryDTO> {
  const { data } = await apiClient.post<NotebookEntryDTO>('/notebook', request)
  return data
}

export async function updateNotebookEntry(id: string, request: NotebookEntryRequest): Promise<NotebookEntryDTO> {
  const { data } = await apiClient.put<NotebookEntryDTO>(`/notebook/${id}`, request)
  return data
}
