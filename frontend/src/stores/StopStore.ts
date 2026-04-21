import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { StopTaskDTO } from '@/types'
import { fetchStopTasks } from '@/services/StopService'

export const useStopStore = defineStore('stop', () => {
  const tasks = ref<Record<string, StopTaskDTO[]>>({})
  const loading = ref<Record<string, boolean>>({})
  const completedTasks = ref<Record<string, number[]>>(
    JSON.parse(sessionStorage.getItem('stop-completed') ?? '{}')
  )

  async function loadTasks(stopId: string) {
    if (tasks.value[stopId] || loading.value[stopId]) return
    loading.value[stopId] = true
    try {
      tasks.value[stopId] = await fetchStopTasks(stopId)
    } finally {
      loading.value[stopId] = false
    }
  }

  function markComplete(stopId: string, taskId: number) {
    if (!completedTasks.value[stopId]) {
      completedTasks.value[stopId] = []
    }
    if (!completedTasks.value[stopId].includes(taskId)) {
      completedTasks.value[stopId].push(taskId)
    }
    sessionStorage.setItem('stop-completed', JSON.stringify(completedTasks.value))
  }

  function isComplete(stopId: string, taskId: number): boolean {
    return completedTasks.value[stopId]?.includes(taskId) ?? false
  }

  function isUnlocked(stopId: string, taskId: number): boolean {
    if (taskId === 1) return true
    return isComplete(stopId, taskId - 1)
  }

  return { tasks, loading, completedTasks, loadTasks, markComplete, isComplete, isUnlocked }
})
