import apiClient from '@/services/ApiService'
import type {
  ApiResponse,
  ClassroomResponseDTO,
  ClassroomRequestDTO,
  ClassroomStudentResponseDTO,
  ClassroomTaskResponseDTO,
} from "@/types";

async function getMyClassrooms(): Promise<ClassroomResponseDTO[]> {
  const { data } = await apiClient.get<ApiResponse<ClassroomResponseDTO>>('/classrooms/me')
  return data.data ? [data.data] : []
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

async function getClassroomTasks(classroomId: string, activeOnly: boolean = true, includeUnlocked: boolean = true, testMode: boolean = false): Promise<ClassroomTaskResponseDTO[]> {
  const params: Record<string, string> = {}
  if (activeOnly) params.activeOnly = 'true'
  if (includeUnlocked) params.includeUnlocked = 'true'
  if (testMode) params.testMode = 'true'

  const queryString = new URLSearchParams(params).toString()
  const url = queryString ? `/classrooms/${classroomId}/tasks?${queryString}` : `/classrooms/${classroomId}/tasks`

  const { data } = await apiClient.get<ApiResponse<ClassroomTaskResponseDTO[]>>(url)
  return data.data
}

async function assignTaskToClassroom(
  classroomId: string,
  taskId: string,
  sortOrder?: number,
  isActive: boolean = true
): Promise<ClassroomTaskResponseDTO> {
  const { data } = await apiClient.post<ApiResponse<ClassroomTaskResponseDTO>>(
    `/classrooms/${classroomId}/tasks`,
    {
      taskId,
      sortOrder,
      isActive,
    }
  )
  return data.data
}

async function removeClassroomTask(classroomId: string, taskId: string): Promise<void> {
  await apiClient.delete(`/classrooms/${classroomId}/tasks/${taskId}`)
}

async function updateClassroomTask(
  classroomId: string,
  taskId: string,
  payload: { sortOrder?: number; isActive?: boolean }
): Promise<ClassroomTaskResponseDTO> {
  const { data } = await apiClient.patch<ApiResponse<ClassroomTaskResponseDTO>>(
    `/classrooms/${classroomId}/tasks/${taskId}`,
    payload
  )
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
  assignTaskToClassroom,
  removeClassroomTask,
  updateClassroomTask,
}
