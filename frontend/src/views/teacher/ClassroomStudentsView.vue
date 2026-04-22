<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { getStudentsOptions, getStudentsQueryKey, approveMutation, removeMutation} from '@/generated-client/@tanstack/vue-query.gen.ts'


const route = useRoute()
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
    <div class="flex flex-col items-center flex-1 w-full px-6 pb-10 pt-4">
      <div class="flex flex-col gap-3 w-full max-w-md mx-auto">
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
          class="w-full bg-white rounded-2x1 shadow px-5 py-4 flex items-center gap-4"
        >
          <div class="flex-1">
            <p class="font-bold text-gray-900 text-base leading-tight">
              {{ student.displayName }}
            </p>
            <p class="text-gray-500 text-sm mt-0.5">
              Ble med: {{ student.joinedAt ? new Date(student.joinedAt).toLocaleDateString('nb-NO') : '' }}
            </p>
          </div>

          <span
            class="text-xs px-2 py-0.5 rounded-full font-semibold shrink-0"
            :class="student.isAllowed ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'"
          >
            {{ student.isAllowed ? 'Godkjent' : 'Venter' }}
          </span>

          <button
            v-if="!student.isAllowed"
            @click="student.userId && approveStudent(student.userId)"
            class="text-green-700 hover:text-green-500 font-bold text-sm shrink-0"
          >
            Godkjenn
          </button>

          <button
            @click="student.userId && removeStudent(student.userId)"
            class="text-red-600 hover:text-red-800 font-bold text-xl leading-none shrink-0"
            title="Fjern elev"
          >
            x
          </button>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>
