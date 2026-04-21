<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/AuthStore.ts'
import ClassroomService from '@/services/ClassroomService.ts'
import SceneComponent from '@/components/SceneComponent.vue'
import type { ClassroomResponseDTO } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const classrooms = ref<ClassroomResponseDTO[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const myClassrooms = computed(() =>
  classrooms.value.filter((c) => c.createdById === authStore.user?.id),
)

onMounted(async () => {
  loading.value = true
  try {
    classrooms.value = await ClassroomService.getMyClassrooms()
  } catch (e: any) {
    error.value = e.message ?? 'Kunne ikke hente klasserom'
  } finally {
    loading.value = false
  }
})

async function deleteClassroom(id: string) {
  if (!confirm('Er du sikker på at du vil slette dette klasserommet?')) return
  try {
    await ClassroomService.deleteClassroom(id)
    classrooms.value = classrooms.value.filter((c) => c.id !== id)
  } catch (e: any) {
    error.value = e.message ?? 'Kunne ikke slette klasserommet'
  }
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
      <div class="flex flex-col gap 3 w-full max-w-md mx-auto">
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

        <div v-if="loading" class="text-center text-white py-6">Laster...</div>

        <div v-else-if="classrooms.length === 0" class="text-center text-white py-6 text-sm">
          Du har ingen klasserom enda.
        </div>

        <div
          v-for="classroom in classrooms"
          :key="classroom.id"
          class="w-full bg-white rounded-2xl shadow px-5 py-4 flex items-center gap-3"
        >
          <button
            @click="deleteClassroom(classroom.id)"
            class="text-red-600 hover:text-red-800 font-bold text-xl leading-none shrink-0"
            title="Slett klasserom"
            aria-label="Slett klasserom"
          >
            ✕
          </button>
          <button
            @click="router.push({ name: 'classroom-detail', params: { id: classroom.id } })"
            class="text-gray-400 hover:text-gray-600 text-xl leading-none shrink-0"
            title="Åpne klasserom"
            aria-label="Åpne klasserom"
          >
            ›
          </button>
          <button
            @click="router.push({ name: 'classroom-detail', params: { id: classroom.id } })"
            class="flex-1 text-left"
          >
            <p class="font-bold text-gray-900 text-base leading-tight">{{ classroom.title }}</p>
            <p class="text-gray-500 text-sm">
              Kode: <span class="font-mono">{{ classroom.joinCode }}</span>
            </p>
          </button>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>
