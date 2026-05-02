<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { getStudentsOptions, getStudentsQueryKey, approveMutation, removeMutation} from '@/generated-client/@tanstack/vue-query.gen.ts'


const route = useRoute()
const router = useRouter()
const classroomId = route.params.id as string
const queryClient = useQueryClient()

const { data, isLoading, error } = useQuery(getStudentsOptions({ path: { classroomId } }))
const students = computed(() => data.value?.data ?? [])

const approve = useMutation({
  ...approveMutation(),
  onSuccess: () =>
    queryClient.invalidateQueries({ queryKey: getStudentsQueryKey({ path: { classroomId } })}),
})

const remove = useMutation({
  ...removeMutation(),
  onSuccess: () =>
    queryClient.invalidateQueries({ queryKey: getStudentsQueryKey({ path: { classroomId } })}),
})

function approveStudent(userId: string) {
  approve.mutate({ path: { classroomId, userId }})
}

function removeStudent(userId: string) {
  if(!confirm('Er du sikker på at du vil fjerne denne eleven?')) return
  remove.mutate({ path: { classroomId, userId } })
}
</script>

<template>
  <SceneComponent
    title="Oversikt elever"
    :previous="`/classroom/${classroomId}`"
    background="/StartBackground.jpg"
  >
    <div class="flex flex-col items-center flex-1 w-full px-4 sm:px-6 pb-10 pt-4">
      <div class="flex flex-col gap-3 w-full max-w-3xl mx-auto">
        <div v-if="error" class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded text-sm">
          {{ error }}
        </div>

        <div v-if="isLoading" class="text-center text-white py-6">Laster...</div>

        <div v-else-if="students.length === 0" class="text-center text-white py-6 text-sm">
          Ingen elever i klasserommet enda.
        </div>

        <div
          v-for="student in students"
          :key="student.userId"
          class="w-full bg-white rounded-xl shadow px-4 py-4 sm:px-5 sm:py-4 flex flex-col sm:flex-row sm:items-center gap-3 sm:gap-4"
        >
          <div class="flex-1 min-w-0">
            <p class="font-bold text-gray-900 text-base leading-tight truncate">
              {{ student.displayName }}
            </p>
            <p class="text-gray-500 text-sm mt-0.5">
              Ble med: {{ student.joinedAt ? new Date(student.joinedAt).toLocaleDateString('nb-NO') : '' }}
            </p>
          </div>

          <div class="flex items-center gap-2 sm:gap-3 flex-wrap sm:flex-nowrap">
            <span
              class="text-xs px-2 py-1 rounded-full font-semibold shrink-0"
              :class="student.isAllowed ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'"
            >
              {{ student.isAllowed ? 'Godkjent' : 'Venter' }}
            </span>

            <button
              v-if="!student.isAllowed"
              class="btn-vn overview-action-btn"
              @click="student.userId && approveStudent(student.userId)"
            >
              Godkjenn
            </button>

            <button
              v-if="student.isAllowed"
              class="btn-vn overview-action-btn"
              @click="student.userId && router.push({ name: 'classroom-student-progress', params: { id: classroomId, userId: student.userId } })"
            >
              Se progresjon
            </button>

            <button
              class="btn-vn btn-vn-danger overview-action-btn"
              title="Fjern elev"
              aria-label="Fjern elev"
              @click="student.userId && removeStudent(student.userId)"
            >
              &times;
            </button>
          </div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.student-action-btn {
  font-size: 0.8rem;
  padding: 0.4rem 0.8rem;
  --notch: 5px;
  flex-shrink: 0;
}


</style>
