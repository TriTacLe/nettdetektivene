import apiClient from '@/services/ApiService'
import type {
  ApiResponse,
  ClassroomResponseDTO,
  ClassroomRequestDTO,
  ClassroomStudentResponseDTO,
  ClassroomTaskResponseDTO,
} from "@/types";

async function getMyClassrooms(): Promise<ClassroomResponseDTO[]> {
  console.log('[ClassroomService] Fetching my classrooms...')
  const { data } = await apiClient.get<ApiResponse<ClassroomResponseDTO>>('/classrooms/me')
  console.log('[ClassroomService] Response:', data)
  const classrooms = data.data ? [data.data] : []
  console.log('[ClassroomService] Parsed classrooms:', classrooms)
  return classrooms
}

async function getAllClassrooms(): Promise<ClassroomResponseDTO[]> {
  const { data } = await apiClient.get<ApiResponse<ClassroomResponseDTO[]>>('/classrooms')
  return data.data
}

async function getClassroom(id: string): Promise<ClassroomResponseDTO> {
  const { data } = await apiClient.get<ApiResponse<ClassroomResponseDTO>>(`/classrooms/${id}`)
  return data.data
}

async function createClassroom(payload: ClassroomRequestDTO): Promise<ClassroomResponseDTO> {
  const { data } = await apiClient.post<ApiResponse<ClassroomResponseDTO>>('/classrooms', payload)
  return data.data
}

async function updateClassroom(id: string, payload: ClassroomRequestDTO): Promise<ClassroomResponseDTO> {
  const { data } = await apiClient.patch<ApiResponse<ClassroomResponseDTO>>(`/classrooms/${id}`, payload)
  return data.data
}

async function deleteClassroom(id: string): Promise<void> {
  await apiClient.delete(`/classrooms/${id}`)
}

async function getStudents(classroomId: string): Promise<ClassroomStudentResponseDTO[]>{
  const { data } = await apiClient.get<ApiResponse<ClassroomStudentResponseDTO[]>>(
    `/classrooms/${classroomId}/students`
  )
  return data.data
}

async function approveStudent(classroomId: string, userId: string): Promise<ClassroomStudentResponseDTO> {
  const { data } = await apiClient.post<ApiResponse<ClassroomStudentResponseDTO>>(
    `/classrooms/${classroomId}/students/${userId}/approve`
  )
  return data.data
}

async function removeStudent(classroomId: string, userId: string): Promise<void> {
  await apiClient.delete(`/classrooms/${classroomId}/students/${userId}`)
}

async function getUnlockedTasks(classroomId: string): Promise<ClassroomTaskResponseDTO[]> {
  console.log('[ClassroomService] getUnlockedTasks - classroomId:', classroomId)
  const { data } = await apiClient.get<ApiResponse<ClassroomTaskResponseDTO[]>>(
    `/classrooms/${classroomId}/tasks/unlocked`
  )
  console.log('[ClassroomService] Unlocked tasks:', data.data?.map(t => t.task.title))
  return data.data
}

async function getClassroomTasks(classroomId: string, activeOnly: boolean = true, includeUnlocked: boolean = true): Promise<ClassroomTaskResponseDTO[]> {
  console.log('[ClassroomService] getClassroomTasks - classroomId:', classroomId, 'activeOnly:', activeOnly, 'includeUnlocked:', includeUnlocked)
  const params: Record<string, boolean> = {}
  if (activeOnly) params.activeOnly = true
  if (includeUnlocked) params.includeUnlocked = true

  const queryString = new URLSearchParams(params as any).toString()
  const url = queryString ? `/classrooms/${classroomId}/tasks?${queryString}` : `/classrooms/${classroomId}/tasks`

  console.log('[ClassroomService] Fetching URL:', url)
  const { data } = await apiClient.get<ApiResponse<ClassroomTaskResponseDTO[]>>(url)
  console.log('[ClassroomService] Response data:', data)
  console.log('[ClassroomService] Tasks with unlock status:', data.data?.map(t => ({
    title: t.task.title,
    isUnlocked: t.isUnlocked,
    taskId: t.taskId
  })))
  return data.data
}

export default {
  getMyClassrooms,
  getAllClassrooms,
  getClassroom,
  createClassroom,
  updateClassroom,
  deleteClassroom,
  getStudents,
  approveStudent,
  removeStudent,
  getUnlockedTasks,
  getClassroomTasks,
}
