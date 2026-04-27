import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { StopTaskDTO } from '@/types'
import { fetchStopTasks, fetchClassroomId } from '@/services/StopService'

export const useStopStore = defineStore('stop', () => {
  const tasks = ref<Record<string, StopTaskDTO[]>>({})
  const loading = ref<Record<string, boolean>>({})
  const completedTasks = ref<Record<string, string[]>>(
    JSON.parse(sessionStorage.getItem('stop-completed') ?? '{}')
  )

  async function loadTasks(stopId: string, forceReload: boolean = false) {
    console.log('[StopStore] loadTasks called - stopId:', stopId, 'forceReload:', forceReload)
    console.log('[StopStore] Current state - tasks exist?', !!tasks.value[stopId], 'loading?', loading.value[stopId])

    if (!forceReload && (tasks.value[stopId] || loading.value[stopId])) {
      console.log('[StopStore] Skipping load (already loaded or loading)')
      return
    }
    loading.value[stopId] = true
    console.log('[StopStore] Started fetching tasks for stopId:', stopId)
    try {
      const fetchedTasks = await fetchStopTasks(stopId)
      tasks.value[stopId] = fetchedTasks
      console.log('[StopStore] Tasks loaded successfully:', fetchedTasks.map(t => ({title: t.title, isUnlocked: t.isUnlocked})))
    } finally {
      loading.value[stopId] = false
      console.log('[StopStore] Loading finished for stopId:', stopId)
    }
  }

  function clearCompleted() {
    completedTasks.value = {}
    sessionStorage.removeItem('stop-completed')
  }

  function clearTasks(stopId?: string) {
    console.log('[StopStore] clearTasks called - stopId:', stopId)
    if (stopId) {
      delete tasks.value[stopId]
    } else {
      tasks.value = {}
    }
    console.log('[StopStore] Tasks cleared. Remaining:', Object.keys(tasks.value))
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

  return { tasks, loading, completedTasks, loadTasks, clearTasks, clearCompleted, markComplete, isComplete, isAllComplete, countComplete, totalTasks, fetchClassroomId }
})
