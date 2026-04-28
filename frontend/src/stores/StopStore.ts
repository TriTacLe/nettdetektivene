import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { StopTaskDTO } from '@/types'
import { fetchStopTasks, fetchClassroomId } from '@/services/StopService'
import ClassroomService from '@/services/ClassroomService'
import { parseTaskContent } from '@/services/TaskService'

export const useStopStore = defineStore('stop', () => {
  const tasks = ref<Record<string, StopTaskDTO[]>>({})
  const loading = ref<Record<string, boolean>>({})
  const completedTasks = ref<Record<string, string[]>>(
    JSON.parse(sessionStorage.getItem('stop-completed') ?? '{}')
  )
  const testModeClassroomId = ref<string | null>(
    sessionStorage.getItem('test-mode-classroom-id')
  )

  function setTestMode(classroomId: string | null) {
    testModeClassroomId.value = classroomId
    if (classroomId) {
      sessionStorage.setItem('test-mode-classroom-id', classroomId)
    } else {
      sessionStorage.removeItem('test-mode-classroom-id')
    }
  }

  async function loadTasks(stopId: string, forceReload: boolean = false) {
    if (!forceReload && (tasks.value[stopId] || loading.value[stopId])) return
    loading.value[stopId] = true
    try {
      const fetchedTasks = await fetchStopTasks(stopId)
      tasks.value[stopId] = fetchedTasks
    } finally {
      loading.value[stopId] = false
    }
  }

  async function loadTasksTestMode(classroomId: string, stopId: string) {
    console.log('[StopStore] loadTasksTestMode called - classroomId:', classroomId, 'stopId:', stopId)
    setTestMode(classroomId)
    loading.value[stopId] = true
    try {
      const allClassroomTasks = await ClassroomService.getClassroomTasks(classroomId, true, true, true)
      const filteredTasks = allClassroomTasks
        .filter(task => task.task.stopLocation === stopId)
        .sort((a, b) => a.sortOrder - b.sortOrder)

      const result = filteredTasks.map((task, index) => {
        const parsedContent = parseTaskContent(task.task.content as string, task.task.taskType)
        console.log('[StopStore] Parsed content for task', index, ':', parsedContent)
        return {
          taskId: index + 1,
          uuid: task.task.id,
          title: task.task.title,
          taskType: task.task.taskType,
          isUnlocked: task.isUnlocked,
          content: parsedContent,
        }
      })
      
      tasks.value[stopId] = result
      console.log('[StopStore] Test mode tasks loaded:', result.length, 'tasks')
    } finally {
      loading.value[stopId] = false
    }
  }

  function clearTasks(stopId?: string) {
    if (stopId) {
      delete tasks.value[stopId]
    } else {
      tasks.value = {}
    }
  }

  function markComplete(stopId: string, uuid: string) {
    if (!completedTasks.value[stopId]) {
      completedTasks.value[stopId] = []
    }
    if (!completedTasks.value[stopId].includes(uuid)) {
      completedTasks.value[stopId].push(uuid)
    }
    sessionStorage.setItem('stop-completed', JSON.stringify(completedTasks.value))
  }

  function isComplete(stopId: string, uuid: string): boolean {
    return completedTasks.value[stopId]?.includes(uuid) ?? false
  }

  function isAllComplete(stopId: string): boolean {
    const stopTasks = tasks.value[stopId]
    if (!stopTasks || stopTasks.length === 0) return false
    return stopTasks.every(t => isComplete(stopId, t.uuid))
  }

  function countComplete(stopId: string): number {
    return tasks.value[stopId]?.filter(t => isComplete(stopId, t.uuid)).length ?? 0
  }

  function totalTasks(stopId: string): number {
    return tasks.value[stopId]?.length ?? 0
  }

  return { tasks, loading, completedTasks, testModeClassroomId, setTestMode, loadTasks, loadTasksTestMode, clearTasks, markComplete, isComplete, isAllComplete, countComplete, totalTasks, fetchClassroomId }
})
