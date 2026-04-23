import apiClient from '@/services/ApiService'
import type { TaskDTO, NewsChallengeContent, PhotoChallengeContent, ApiResponse } from '@/types'

export async function fetchAllTasks(): Promise<TaskDTO[]> {
  const { data } = await apiClient.get<ApiResponse<TaskDTO[]>>('/tasks')
  return data.data.map(task => ({
    ...task,
    content: parseTaskContent(task.content as string, task.taskType)
  }))
}

export async function fetchTasksByStopLocation(stopLocation: string): Promise<TaskDTO[]> {
  const { data } = await apiClient.get<ApiResponse<TaskDTO[]>>('/tasks')
  return data.data
    .filter(task => task.stopLocation === stopLocation)
    .map(task => ({
      ...task,
      content: parseTaskContent(task.content as string, task.taskType)
    }))
}

export async function fetchTaskById(id: string): Promise<TaskDTO> {
  const { data } = await apiClient.get<ApiResponse<TaskDTO>>(`/tasks/${id}`)
  const task = data.data
  return {
    ...task,
    content: parseTaskContent(task.content as string, task.taskType)
  }
}

export function parseTaskContent(contentString: string, taskType: string) {
  try {
    const content = typeof contentString === 'string' ? JSON.parse(contentString) : contentString

    if (taskType === 'NEWS') {
      return { news: content as NewsChallengeContent }
    } else if (taskType === 'PHOTO') {
      return { photo: content as PhotoChallengeContent }
    }
    return content
  } catch (error) {
    console.error('Failed to parse task content:', error)
    return null
  }
}

export function getTasksByStopLocation(tasks: TaskDTO[], stopLocation: string): TaskDTO[] {
  return tasks.filter(task => task.stopLocation === stopLocation)
}

export function getTasksByType(tasks: TaskDTO[], taskType: string): TaskDTO[] {
  return tasks.filter(task => task.taskType === taskType)
}