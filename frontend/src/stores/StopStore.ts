import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { StopTaskDTO } from '@/types'
import { fetchStopTasks, fetchClassroomId } from '@/services/StopService'
import ClassroomService from '@/services/ClassroomService'
import { parseTaskContent } from '@/services/TaskService'
import TaskAttemptService from '@/services/TaskAttemptService'

export const useStopStore = defineStore('stop', () => {
  const tasks = ref<Record<string, StopTaskDTO[]>>({})
  const loading = ref<Record<string, boolean>>({})

  // Authoritative completion state, derived from the backend
  // (task_best_score.first_passed_at IS NOT NULL). Loaded after auth
  // and refreshed after each successful submit.
  const passedTaskIds = ref<Set<string>>(new Set())

  // Ephemeral test-mode completion state. Teacher previews don't write
  // to task_best_score, so we keep a separate in-memory set that's
  // wiped on exitTestMode / logout.
  const testModeCompleted = ref<Set<string>>(new Set())

  const testModeClassroomId = ref<string | null>(null)
  const inTestMode = () => testModeClassroomId.value !== null
  const activeSet = () => (inTestMode() ? testModeCompleted.value : passedTaskIds.value)

  function setTestMode(classroomId: string | null) {
    testModeClassroomId.value = classroomId
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
    setTestMode(classroomId)
    loading.value[stopId] = true
    try {
      const allClassroomTasks = await ClassroomService.getClassroomTasks(classroomId, true, true, true)
      const filteredTasks = allClassroomTasks
        .filter(task => task.task.stopLocation === stopId)
        .sort((a, b) => a.sortOrder - b.sortOrder)

      const result = filteredTasks.map((task, index) => ({
        taskId: index + 1,
        uuid: task.task.id,
        title: task.task.title,
        taskType: task.task.taskType,
        isUnlocked: task.isUnlocked,
        content: parseTaskContent(task.task.content as string, task.task.taskType),
      }))

      tasks.value[stopId] = result
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

  function clearCompleted() {
    passedTaskIds.value = new Set()
    testModeCompleted.value = new Set()
  }

  function exitTestMode() {
    if (!inTestMode()) return
    setTestMode(null)
    clearTasks()
    testModeCompleted.value = new Set()
  }

  // Fetch the authenticated user's passed task IDs from the backend.
  // No-op in test mode (the test-mode set is purely local).
  async function loadCompleted() {
    if (inTestMode()) return
    try {
      const ids = await TaskAttemptService.getMyPassedTaskIds()
      passedTaskIds.value = new Set(ids)
    } catch {
      // Surface nothing here — completion just stays empty until next refresh.
    }
  }

  async function refreshCompleted() {
    if (inTestMode()) return
    const ids = await TaskAttemptService.getMyPassedTaskIds()
    passedTaskIds.value = new Set(ids)
  }

  function markComplete(_stopId: string, uuid: string) {
    if (inTestMode()) {
      testModeCompleted.value.add(uuid)
      return
    }
    // Optimistic local add so the map updates immediately; reconcile
    // with the backend in the background.
    passedTaskIds.value.add(uuid)
    refreshCompleted().catch(() => {})
  }

  function isComplete(_stopId: string, uuid: string): boolean {
    return activeSet().has(uuid)
  }

  function isAllComplete(stopId: string): boolean {
    const stopTasks = tasks.value[stopId]
    if (!stopTasks || stopTasks.length === 0) return false
    const set = activeSet()
    return stopTasks.every(t => set.has(t.uuid))
  }

  function countComplete(stopId: string): number {
    const stopTasks = tasks.value[stopId]
    if (!stopTasks) return 0
    const set = activeSet()
    return stopTasks.filter(t => set.has(t.uuid)).length
  }

  function totalTasks(stopId: string): number {
    return tasks.value[stopId]?.length ?? 0
  }

  return {
    tasks,
    loading,
    passedTaskIds,
    testModeCompleted,
    testModeClassroomId,
    setTestMode,
    loadTasks,
    loadTasksTestMode,
    clearTasks,
    clearCompleted,
    exitTestMode,
    loadCompleted,
    refreshCompleted,
    markComplete,
    isComplete,
    isAllComplete,
    countComplete,
    totalTasks,
    fetchClassroomId,
  }
})
