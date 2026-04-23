import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { NotebookEntryDTO, NotebookEntryRequest } from '@/types'
import {
  getNotebookEntries,
  createNotebookEntry,
  updateNotebookEntry,
} from '@/services/NotebookService'

export const useNotebookStore = defineStore('notebook', () => {
  const entries = ref<NotebookEntryDTO[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const classroomId = ref<string | null>(null)

  async function fetchEntries(cid: string): Promise<void> {
    classroomId.value = cid
    isLoading.value = true
    error.value = null
    try {
      entries.value = await getNotebookEntries(cid)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Kunne ikke hente notatblokkoppføringer'
    } finally {
      isLoading.value = false
    }
  }

  async function saveEntry(request: NotebookEntryRequest): Promise<void> {
    if (!classroomId.value) throw new Error('Mangler klasserom-ID')
    const existing = getEntryByStop(request.stopLocation)
    if (existing) {
      const updated = await updateNotebookEntry(classroomId.value, existing.id, request)
      const index = entries.value.findIndex((e) => e.id === existing.id)
      if (index !== -1) entries.value[index] = updated
    } else {
      const created = await createNotebookEntry(classroomId.value, request)
      entries.value.push(created)
    }
  }

  function getEntryByStop(stopLocation: string): NotebookEntryDTO | undefined {
    return entries.value.find((e) => e.stopLocation === stopLocation)
  }

  return {
    entries,
    isLoading,
    error,
    classroomId,
    fetchEntries,
    saveEntry,
    getEntryByStop,
  }
})
