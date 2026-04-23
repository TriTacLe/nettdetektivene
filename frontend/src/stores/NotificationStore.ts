import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { NotificationDTO } from '@/types'
import { NotificationType } from '@/types'
import { getNotifications, getNotificationsByClassroom } from '@/services/NotificationService'

export const useNotificationStore = defineStore('notifications', () => {
  const notifications = ref<NotificationDTO[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const activeClassroomId = ref<string | null>(null)
  const activeTypeFilter = ref<NotificationType | 'INAPPROPRIATE' | 'COMPLETED' | null>(null)
  const activeClassroomFilter = ref<string | null>(null)
  const currentPage = ref(1)
  const pageSize = 10

  const availableClassrooms = computed(() => {
    const seen = new Set<string>()
    return notifications.value
    .filter((n) => {
      if (seen.has(n.classroomId)) return false
      seen.add(n.classroomId)
      return true
    })
    .map((n) => ({ id: n.classroomId, name: n.classroomName }))
  })

  const allFilteredNotifications = computed(() => {
    let result = notifications.value
    if (activeClassroomId.value) {
      result = result.filter((n) => n.classroomId === activeClassroomId.value)
    }
    if (activeClassroomFilter.value) {
      result = result.filter((n) => n.classroomId === activeClassroomFilter.value)
    }
    if (activeTypeFilter.value === 'COMPLETED') {
      result = result.filter((n) =>
        n.type === NotificationType.STOP_COMPLETED ||
        n.type === NotificationType.CLASS_COMPLETED_GAME
      )
    } else if (activeTypeFilter.value === 'INAPPROPRIATE') {
      result = result.filter((n) =>
        n.type === NotificationType.INAPPROPRIATE_MESSAGE ||
        n.type === NotificationType.INAPPROPRIATE_USERNAME
      )
    } else if (activeTypeFilter.value) {
      result = result.filter((n) => n.type === activeTypeFilter.value)
    }
    return result
  })

  const totalPages = computed(() => Math.max(1, Math.ceil(allFilteredNotifications.value.length / pageSize)))

  const filteredNotifications = computed(() => {
    const start = (currentPage.value - 1) * pageSize
    return allFilteredNotifications.value.slice(start, start + pageSize)
  })

  async function fetchNotifications(classroomId?: string): Promise<void> {
    isLoading.value = true
    error.value = null
    activeClassroomId.value = classroomId ?? null
    try {
      notifications.value = classroomId
        ? await getNotificationsByClassroom(classroomId)
        : await getNotifications()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Kunne ikke hente varsler'
    } finally {
      isLoading.value = false
    }
  }

  function markAsRead(id: string): void {
    const n = notifications.value.find((n) => n.id === id)
    if (n) n.isRead = true
  }

  function setTypeFilter(type: NotificationType | 'INAPPROPRIATE' | 'COMPLETED' | null): void {
    activeTypeFilter.value = type
    currentPage.value = 1
  }

  function setClassroomFilter(classroomId: string | null): void {
    activeClassroomFilter.value = classroomId
    currentPage.value = 1
  }

  function setPage(page: number): void {
    currentPage.value = page
  }

  return {
    notifications,
    filteredNotifications,
    totalPages,
    currentPage,
    isLoading,
    error,
    activeClassroomId,
    activeTypeFilter,
    activeClassroomFilter,
    availableClassrooms,
    fetchNotifications,
    markAsRead,
    setTypeFilter,
    setClassroomFilter,
    setPage,
  }
})
