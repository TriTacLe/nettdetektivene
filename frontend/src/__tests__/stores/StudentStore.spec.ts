import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useStudentStore } from '@/stores/StudentStore'

describe('StudentStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('initial state', () => {
    it('initializes with null classroom and institution', () => {
      const store = useStudentStore()
      expect(store.classroomId).toBeNull()
      expect(store.institutionId).toBeNull()
    })
  })

  describe('setClassroom', () => {
    it('sets classroom and institution IDs', () => {
      const store = useStudentStore()
      store.setClassroom('class-123', 'institution-456')

      expect(store.classroomId).toBe('class-123')
      expect(store.institutionId).toBe('institution-456')
    })

    it('replaces existing classroom and institution', () => {
      const store = useStudentStore()
      store.setClassroom('old-class', 'old-institution')
      store.setClassroom('new-class', 'new-institution')

      expect(store.classroomId).toBe('new-class')
      expect(store.institutionId).toBe('new-institution')
    })
  })

  describe('clear', () => {
    it('clears classroom and institution IDs', () => {
      const store = useStudentStore()
      store.setClassroom('class-123', 'institution-456')
      store.clear()

      expect(store.classroomId).toBeNull()
      expect(store.institutionId).toBeNull()
    })

    it('handles clearing when already null', () => {
      const store = useStudentStore()
      expect(() => store.clear()).not.toThrow()
      expect(store.classroomId).toBeNull()
      expect(store.institutionId).toBeNull()
    })
  })

  describe('persistence', () => {
    it('should work with persist plugin configured', () => {
      const store = useStudentStore()
      store.setClassroom('class-123', 'institution-456')

      expect(store.classroomId).toBe('class-123')
      expect(store.institutionId).toBe('institution-456')

      store.clear()
    })
  })
})