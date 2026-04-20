import apiClient from '@/services/ApiService'
import type {
  ApiResponse,
  ClassroomResponseDTO,
  ClassroomRequestDTO,
  ClassroomStudentResponseDTO,
} from "@/types";

async function getMyClassrooms(): Promise<ClassroomResponseDTO[]> {
  const { data } = await apiClient.get<ApiResponse<ClassroomResponseDTO[]>>('/classrooms/mine')
  return data.data
}

async function getAllClassrooms(): Promise<ClassroomResponseDTO[]> {
  const { data } = await apiClient.get<ApiResponse<ClassroomResponseDTO[]>>('/classrooms')
  return data.data
}

async function getClassroom(id: string): Promise<ClassroomResponseDTO> {
  const { data } = await apiClient.post<ApiResponse<ClassroomResponseDTO>>(`/classrooms/${id}`)
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

// async function getStudents(classroomId: string): Promise<ClassroomStudentResponseDTO>{
//   const { data } = await apiClient.get<ApiResponse<ClassroomStudentResponseDTO[]>>(
//     `/classrooms/${classroomId}/students`
//   )
//   return data.data
// }

async function approveStudent(classroomId: string, userId: string): Promise<ClassroomStudentResponseDTO> {
  const { data } = await apiClient.post<ApiResponse<ClassroomStudentResponseDTO>>(
    `/classrooms/${classroomId}/students/${userId}/approve`
  )
  return data.data
}

async function removeStudent(classroomId: string, userId: string): Promise<void> {
  await apiClient.delete(`/classrooms/${classroomId}/students/${userId}`)
}

export default {
  getMyClassrooms,
  getAllClassrooms,
  getClassroom,
  createClassroom,
  updateClassroom,
  deleteClassroom,
  //getStudents,
  approveStudent,
  removeStudent,
}
