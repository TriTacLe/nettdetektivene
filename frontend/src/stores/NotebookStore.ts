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

  async function fetchEntries(): Promise<void> {
    isLoading.value = true
    error.value = null
    try {
      // TODO: fjern mock når backend er klar
      entries.value = [
        {
          id: '1',
          userId: 'test',
          classroomId: 'test',
          stopLocation: 'NEWS',
          autoNotes: '["Sjekk alltid kilden", "Se etter clickbait-overskrifter"]',
          studentReflection: '',
          createdAt: '',
          updatedAt: '',
        }
      ]
      // entries.value = await getNotebookEntries()  // kommenter ut til backend er klar
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Kunne ikke hente notatblokkoppføringer'
    } finally {
      isLoading.value = false
    }
  }

  async function saveEntry(request: NotebookEntryRequest): Promise<void> {
    const existing = getEntryByStop(request.stopLocation)
    if (existing) {
      const updated = await updateNotebookEntry(existing.id, request)
      const index = entries.value.findIndex((e) => e.id === existing.id)
      if (index !== -1) entries.value[index] = updated
    } else {
      const created = await createNotebookEntry(request)
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
    fetchEntries,
    saveEntry,
    getEntryByStop,
  }


})
