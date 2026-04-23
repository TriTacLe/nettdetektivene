import { fetchAllTasks } from '@/services/TaskService'
import type { StopTaskDTO } from '@/types'

export async function fetchStopTasks(stopId: string): Promise<StopTaskDTO[]> {
  const allTasks = await fetchAllTasks()
  const filteredTasks = allTasks.filter(task => task.stopLocation === stopId)

  return filteredTasks.map((task, index) => ({
    taskId: index + 1,
    title: task.title,
    taskType: task.taskType,
  }))
}