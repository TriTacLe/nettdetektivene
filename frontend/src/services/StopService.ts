import { fetchAllTasks } from '@/services/TaskService'
import ClassroomService from '@/services/ClassroomService'
import type { StopTaskDTO } from '@/types'

export async function fetchStopTasks(stopId: string): Promise<StopTaskDTO[]> {
  console.log('='.repeat(80))
  console.log('[fetchStopTasks] ========== START FETCH FOR STOP:', stopId, '==========')
  console.log('='.repeat(80))

  try {
    console.log('[fetchStopTasks] Step 1: Fetching classrooms...')
    const classrooms = await ClassroomService.getMyClassrooms()
    console.log('[fetchStopTasks] Step 1 RESULT: Classrooms fetched:', classrooms?.length, classrooms?.map(c => ({id: c.id, title: c.title})))

    if (classrooms && classrooms.length > 0) {
      const classroom = classrooms[0]
      if (!classroom) {
        throw new Error('Classroom not found')
      }
      const classroomId = classroom.id

      console.log('[fetchStopTasks] Step 2: Fetching tasks for classroom:', classroomId)
      const allClassroomTasks = await ClassroomService.getClassroomTasks(classroomId, true, true)

      console.log('[fetchStopTasks] Step 2 RESULT: All classroom tasks:', allClassroomTasks.length, 'total tasks')
      console.log('[fetchStopTasks] Task details:', allClassroomTasks.map(t => ({
        taskId: t.taskId,
        title: t.task.title,
        stop: t.task.stopLocation,
        isUnlocked: t.isUnlocked
      })))

      const filteredTasks = allClassroomTasks.filter(task => task.task.stopLocation === stopId)

      console.log('[fetchStopTasks] Step 3: Filtered for stopId', stopId, ':', filteredTasks.length, 'tasks')

      const result = filteredTasks.map((task, index) => {
        const mapped = {
          taskId: index + 1,
          uuid: task.task.id,
          title: task.task.title,
          taskType: task.task.taskType,
          isUnlocked: task.isUnlocked,
        }
        console.log('[fetchStopTasks] Mapped task:', index, mapped)
        return mapped
      })

      console.log('='.repeat(50))
      console.log('[fetchStopTasks] ========== FINAL RESULT ==========')
      console.log('[fetchStopTasks] Total tasks:', result.length)
      console.log('[fetchStopTasks] Available (unlocked):', result.filter(t => t.isUnlocked).map(t => t.title))
      console.log('[fetchStopTasks] Locked:', result.filter(t => !t.isUnlocked).map(t => t.title))
      console.log('='.repeat(50))

      return result
    }
  } catch (error) {
    console.error('[fetchStopTasks] ERROR: Failed to fetch classroom tasks:', error)
  }

  console.log('[fetchStopTasks] Using fallback - fetching all tasks')
  const allTasks = await fetchAllTasks()
  const filteredTasks = allTasks.filter(task => task.stopLocation === stopId)

  return filteredTasks.map((task, index) => ({
    taskId: index + 1,
    uuid: task.id,
    title: task.title,
    taskType: task.taskType,
    isUnlocked: false,
  }))
}

export async function fetchClassroomId(): Promise<string | null> {
  try {
    const classrooms = await ClassroomService.getMyClassrooms()
    return classrooms && classrooms.length > 0 ? classrooms[0].id : null
  } catch {
    return null
  }
}