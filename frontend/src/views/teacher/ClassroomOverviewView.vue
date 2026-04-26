<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/AuthStore.ts'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import {
  getAllClassroomsOptions,
  getAllClassroomsQueryKey,
  deleteClassroomMutation,
} from '@/generated-client/@tanstack/vue-query.gen.ts'

const router = useRouter()
const authStore = useAuthStore()
const queryClient = useQueryClient()

const { data, isLoading, error } = useQuery(getAllClassroomsOptions())

const myClassrooms = computed(() => data.value?.data ?? [])

const deleteMutation = useMutation({
  ...deleteClassroomMutation(),
  onSuccess: () => queryClient.invalidateQueries({ queryKey: getAllClassroomsQueryKey() }),
})

async function deleteClassroom(id: string) {
  if (!confirm('Er du sikker på at du vil slette dette klasserommet?')) return
  deleteMutation.mutate({ path: { id } })
}

function openClassroom(id: string) {
  router.push({ name: 'classroom-detail', params: { id } })
}
</script>

<template>
  <SceneComponent
    title="Klasseromsoversikt"
    background="/StartBackground.jpg"
    previous="teacher-home"
  >
    <div class="flex flex-col items-center flex-1 w-full px-6 pb-10 pt-4">
      <div class="w-full max-w-md mx-auto flex justify-end mb-2">
        <span class="text-white text-sm font-semibold opacity-80">{{ authStore.user?.displayName ?? authStore.user?.email }}</span>
      </div>
      <div class="flex flex-col gap-3 w-full max-w-md mx-auto">
        <div
          v-if="error"
          class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded text-sm"
        >
          {{ error }}
        </div>

        <button
          @click="router.push({ name: 'create-classroom' })"
          class="w-full bg-white rounded-2xl shadow px-5 py-4 flex items-center gap-4 hover:bg-gray-50 transition text-left"
        >
          <span class="text-2xl">+</span>
          <div class="flex-1">
            <p class="font-bold text-gray-900 text-base leading-tight">Nytt Klasserom</p>
            <p class="text-gray-500 text-sm mt-0.5">Opprett et nytt klasserom</p>
          </div>
          <span class="text-gray-400 text-lg">›</span>
        </button>

        <div v-if="isLoading" class="text-center text-white py-6">Laster...</div>

        <div v-else-if="myClassrooms.length === 0" class="text-center text-white py-6 text-sm">
          Du har ingen klasserom enda.
        </div>

        <div
          v-for="classroom in myClassrooms"
          :key="classroom.id"
          class="bg-amber-50 border border-amber-300 rounded-xl px-6 py-4 flex items-center justify-between shadow"
        >
          <div>
            <p class="text-xl font-bold text-amber-900">{{ classroom.title }}</p>
            <p class="text-sm text-amber-700">
              Kode: <span class="font-mono font-bold">{{ classroom.joinCode }}</span>
            </p>
            <p v-if="classroom.classDesc" class="text-sm text-amber-600 mt-1">
              {{ classroom.classDesc }}
            </p>
          </div>
          <div class="flex gap-3">
            <button
              @click="classroom.id && openClassroom(classroom.id)"
              class="text-blue-700 hover:text-blue-500 text-xl"
              title="Åpne"
            >
              Åpne klasserom
            </button>
            <button
              @click="classroom.id && deleteClassroom(classroom.id)"
              class="text-red-700 hover:text-red-500 text-xl"
              title="Slett"
            >
              Slett
            </button>
          </div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>
