import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotebookStore } from '@/stores/NotebookStore'
import * as NotebookService from '@/services/NotebookService'
import type { NotebookEntryDTO, NotebookEntryRequest } from '@/types'

vi.mock('@/services/NotebookService')

const mockEntry: NotebookEntryDTO = {
  id: 'entry-1',
  userId: 'user-1',
  classroomId: 'classroom-1',
  stopLocation: 'NEWS',
  autoNotes: '["Tips 1"]',
  studentReflection: 'Min refleksjon',
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
}

describe('NotebookStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('fetchEntries', () => {
    it('henter og lagrer oppføringer fra backend', async () => {
      vi.mocked(NotebookService.getNotebookEntries).mockResolvedValue([mockEntry])
      const store = useNotebookStore()

      await store.fetchEntries('classroom-1')

      expect(store.entries).toEqual([mockEntry])
      expect(store.isLoading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('setter classroomId ved henting', async () => {
      vi.mocked(NotebookService.getNotebookEntries).mockResolvedValue([])
      const store = useNotebookStore()

      await store.fetchEntries('classroom-1')

      expect(store.classroomId).toBe('classroom-1')
    })

    it('setter feilmelding ved nettverksfeil', async () => {
      vi.mocked(NotebookService.getNotebookEntries).mockRejectedValue(
        new Error('Nettverksfeil'),
      )
      const store = useNotebookStore()

      await store.fetchEntries('classroom-1')

      expect(store.error).toBe('Nettverksfeil')
      expect(store.entries).toEqual([])
      expect(store.isLoading).toBe(false)
    })

    it('setter isLoading til true under henting', async () => {
      let loadingDuringFetch = false
      vi.mocked(NotebookService.getNotebookEntries).mockImplementation(async () => {
        loadingDuringFetch = useNotebookStore().isLoading
        return []
      })
      const store = useNotebookStore()

      await store.fetchEntries('classroom-1')

      expect(loadingDuringFetch).toBe(true)
    })
  })

  describe('saveEntry', () => {
    it('oppretter ny oppføring hvis stopLocation ikke finnes fra før', async () => {
      vi.mocked(NotebookService.getNotebookEntries).mockResolvedValue([])
      vi.mocked(NotebookService.createNotebookEntry).mockResolvedValue(mockEntry)
      const store = useNotebookStore()
      await store.fetchEntries('classroom-1')

      const request: NotebookEntryRequest = {
        stopLocation: 'NEWS',
        studentReflection: 'Min refleksjon',
      }
      await store.saveEntry(request)

      expect(NotebookService.createNotebookEntry).toHaveBeenCalledWith('classroom-1', request)
      expect(store.entries).toContainEqual(mockEntry)
    })

    it('oppdaterer eksisterende oppføring hvis stopLocation finnes', async () => {
      vi.mocked(NotebookService.getNotebookEntries).mockResolvedValue([mockEntry])
      const updatedEntry = { ...mockEntry, studentReflection: 'Oppdatert' }
      vi.mocked(NotebookService.updateNotebookEntry).mockResolvedValue(updatedEntry)
      const store = useNotebookStore()
      await store.fetchEntries('classroom-1')

      const request: NotebookEntryRequest = {
        stopLocation: 'NEWS',
        studentReflection: 'Oppdatert',
      }
      await store.saveEntry(request)

      expect(NotebookService.updateNotebookEntry).toHaveBeenCalledWith(
        'classroom-1',
        'entry-1',
        request,
      )
      expect(store.entries[0]?.studentReflection).toBe('Oppdatert')
    })

    it('kaster feil hvis classroomId mangler', async () => {
      const store = useNotebookStore()

      await expect(
        store.saveEntry({ stopLocation: 'NEWS', studentReflection: '' }),
      ).rejects.toThrow('Mangler klasserom-ID')
    })
  })

  describe('getEntryByStop', () => {
    it('returnerer oppføring for gitt stopLocation', async () => {
      vi.mocked(NotebookService.getNotebookEntries).mockResolvedValue([mockEntry])
      const store = useNotebookStore()
      await store.fetchEntries('classroom-1')

      expect(store.getEntryByStop('NEWS')).toEqual(mockEntry)
    })

    it('returnerer undefined hvis stopLocation ikke finnes', async () => {
      vi.mocked(NotebookService.getNotebookEntries).mockResolvedValue([])
      const store = useNotebookStore()
      await store.fetchEntries('classroom-1')

      expect(store.getEntryByStop('PHOTO')).toBeUndefined()
    })
  })
})
