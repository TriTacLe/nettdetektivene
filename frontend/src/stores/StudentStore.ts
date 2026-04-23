import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useStudentStore = defineStore(
  'student',
  () => {
    const classroomId = ref<string | null>(null)

    function setClassroomId(id: string) {
      classroomId.value = id
    }

    function clear() {
      classroomId.value = null
    }

    return { classroomId, setClassroomId, clear }
  },
  {
    persist: {
      storage: sessionStorage,
    },
  },
)
