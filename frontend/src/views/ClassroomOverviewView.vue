<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/AuthStore.ts";
import ClassroomService from "@/services/ClassroomService.ts";
import SceneComponent from "@/components/SceneComponent.vue";
import type { ClassroomResponseDTO } from "@/types";

const router = useRouter()
const authStore = useAuthStore()

const classrooms = ref<ClassroomResponseDTO[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const myClassrooms = computed(() =>
  classrooms.value.filter((c) => c.createdById === authStore.user?.id)
)

onMounted(async () => {
  loading.value = true
  try {
    classrooms.value = await ClassroomService.getAllClassrooms()
  } catch (e: any) {
    error.value = e.message ?? 'Kunne ikke hente klasserom'
  } finally {
    loading.value = false
  }
})

async function deleteClassroom(id: string) {
  if(!confirm('Er du sikker på at du vil slette dette klasserommet?')) return
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
  <SceneComponent title="Klasseromsoversikt" background="/StartBackground.jpg">
    <div class="min-h-screen p-8 max-w-3xø mx-auto">
      <div class="flex items-center justify between mb-8">
        <button
          @click="$router.push({ name: 'teacher-home'})"
          class="text-amber-900 hover:text-amber-700 font-semibold"
        >
          Tilbake
        </button>
        <h1 class="text-3xl font-bold text-amber-900">Oversikt</h1>
        <button
          @click="$router.push({ name: 'create-classroom' })"
          class="bg-green-700 hover: bg-green-600 text-white px-4 py-2 rounded-lg font-semibold shadow"
        >
          Lag nytt klasserom
        </button>
      </div>

      <div v-if="error" class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded mb-4">
        {{ error }}
      </div>

      <div v-if="loading" class="text-amber-900 text-center py-10 text-xl">Laster...</div>

      <div v-else-if="myClassrooms.length === 0"
           class="text-center text-amber-900 py-16 text-lg"
      >
        Du har ingen klasserom enda
        <br />
        <button
          @click="$router.push({ name: 'create-classroom' })"
          class="mt-4 underline hover:text-amber-700"
        >
          Opprett ditt første klasserom
        </button>
      </div>

      <ul v-else class="flex flex-col gap-4">
        <li
          v-for="classroom in myClassrooms"
          :key="classroom.id"
          class="bg-amber-50 border border-amber-300 rounded-xl px-6 py-4 flex items-center justify-between shadow"
          >
          <div>
            <p class="text-xl font-bold text-amber-900">{{ classroom.title }} </p>
            <p class="text-sm text-amber-700">
              Kode: <span class="font-mono font-bold">{{ classroom.joinCode }}</span>
            </p>
            <p v-if="classroom.classDesc" class="text-sm text-amber-600 mt-1">
              {{ classroom.classDesc }}
            </p>
          </div>
          <div class="flex gap-3">
            <button
              @click="openClassroom(classroom.id)"
              class="text-blue-700 hover:text-blue-500 text-xl"
              title="Åpne"
            >
              Åpne klasserom
            </button>
            <button
              @click="deleteClassroom(classroom.id)"
              class="text-red-700 hover:text-red-500 text-xl"
              title="Slett"
            >
              Slett
            </button>
          </div>
        </li>
      </ul>
    </div>
  </SceneComponent>
</template>











