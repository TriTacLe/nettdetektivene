import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useStudentStore = defineStore(
  'student',
  () => {
    const classroomId = ref<string | null>(null)
    const institutionId = ref<string | null>(null)

    function setClassroom(cId: string, iId: string) {
      classroomId.value = cId
      institutionId.value = iId
    }

    function clear() {
      classroomId.value = null
      institutionId.value = null
    }

    return { classroomId, institutionId, setClassroom, clear }
  },
  {
    persist: {
      storage: sessionStorage,
    },
  },
)
