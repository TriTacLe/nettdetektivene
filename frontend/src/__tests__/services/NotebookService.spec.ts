import { describe, it, expect, vi, beforeEach } from 'vitest'
import apiClient from '@/services/ApiService'
import {
  getNotebookEntries,
  createNotebookEntry,
  updateNotebookEntry,
} from '@/services/NotebookService'
import type { NotebookEntryDTO, NotebookEntryRequest } from '@/types'

vi.mock('@/services/ApiService', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

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

const mockRequest: NotebookEntryRequest = {
  stopLocation: 'NEWS',
  autoNotes: '["Tips 1"]',
  studentReflection: 'Min refleksjon',
}

describe('NotebookService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getNotebookEntries', () => {
    it('returnerer notebook-oppføringer fra backend', async () => {
      vi.mocked(apiClient.get).mockResolvedValue({
        data: { success: true, message: null, data: [mockEntry] },
      })

      const result = await getNotebookEntries('classroom-1')

      expect(apiClient.get).toHaveBeenCalledWith('/classrooms/classroom-1/notebooks/me')
      expect(result).toEqual([mockEntry])
    })

    it('kaster feil ved nettverksfeil', async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error('Nettverksfeil'))

      await expect(getNotebookEntries('classroom-1')).rejects.toThrow('Nettverksfeil')
    })
  })

  describe('createNotebookEntry', () => {
    it('oppretter en ny notebook-oppføring', async () => {
      vi.mocked(apiClient.post).mockResolvedValue({
        data: { success: true, message: null, data: mockEntry },
      })

      const result = await createNotebookEntry('classroom-1', mockRequest)

      expect(apiClient.post).toHaveBeenCalledWith(
        '/classrooms/classroom-1/notebooks',
        mockRequest,
      )
      expect(result).toEqual(mockEntry)
    })
  })

  describe('updateNotebookEntry', () => {
    it('oppdaterer en eksisterende notebook-oppføring', async () => {
      vi.mocked(apiClient.patch).mockResolvedValue({
        data: { success: true, message: null, data: mockEntry },
      })

      const result = await updateNotebookEntry('classroom-1', 'entry-1', mockRequest)

      expect(apiClient.patch).toHaveBeenCalledWith(
        '/classrooms/classroom-1/notebooks/entry-1',
        mockRequest,
      )
      expect(result).toEqual(mockEntry)
    })
  })
})
