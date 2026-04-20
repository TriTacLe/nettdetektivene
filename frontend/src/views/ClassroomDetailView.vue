<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import ClassroomService from "@/services/ClassroomService.ts";
import SceneComponent from "@/components/SceneComponent.vue";
import type { ClassroomResponseDTO, ClassroomStudentResponseDTO } from "@/types";

const route = useRoute()
const router = useRouter()

const classroomId = route.params.id as string
const classroom = ref<ClassroomResponseDTO | null> (null)
const students = ref<ClassroomStudentResponseDTO[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const activeTab = ref<'students' | 'settings'>('students')

onMounted(async () => {
  loading.value = true
  try {
    const [classroomData, studentData] = await Promise.all([
      ClassroomService.getClassroom(classroomId),
      ClassroomService.getStudents(classroomId),
    ])
    classroom.value = classroomData
    students.value = studentData
  } catch (e: any) {
    error.value = e.message ?? 'Kunne ikke hente klasserom'
  } finally {
    loading.value = false
  }
})

async function approveStudent(userId: string) {
  try {
    const updated = await ClassroomService.approveStudent(classroomId, userId)
    const idx = students.value.findIndex((s) => s.userId === userId)
    if (idx !== -1) students.value[idx] = updated
  } catch (e: any) {
    error.value = e.message ?? 'Kunne ikke godkjenne elev'
  }
}

async function removeStudent(userId: string) {
  if(!confirm('Er du sikker på at du vil fjerne denne eleven?')) return
  try {
    await ClassroomService.removeStudent(classroomId, userId)
    students.value = students.value.filter((s) => s.userId !== userId)
  } catch (e: any) {
    error.value = e.message ?? 'Kunne ikke fjerne elev'
  }
}
</script>

<template>
  <SceneComponent title="Detaljer for klasserom" background="/StartBackground.jpg">
    <div class="min-h-screen p-8 max-w-3xl mx-auto">
      <button
        @click="router.push({ name: 'classroom-overview' })"
        class="text-amber-900 hover:text-amber-700 font-semibold mb-6 block"
      >
        Tilbake til oversikt
      </button>

      <div v-if="loading" class="text-amber-900 text-center py-16 text-xl">Laster...</div>

      <div v-else-if="error" class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded">
        {{ error }}
      </div>

      <template v-else-if="classroom">
        <div class="bg-amber-50 rounded-2xl p-6 shadow mb-6">
          <h1 class="text-3xl font-bold text-amber-900">{{ classroom.title }}</h1>
          <p v-if="classroom.classDesc" class="text-amber-700 mt-1">{{ classroom.classDesc }}</p>
          <div class="mt-3 flex items-center gap-4">
            <span class="text-sm text-amber-800">
              Klassekode:
              <span class="font-mono font-bold text-lg tracking-widest bg-amber-200 px-2 py-0.5 rounded">
                {{ classroom.joinCode }}
              </span>
            </span>
            <span
              :class="classroom.isGameEnabled ? 'text-green-700' : 'text-gray-500'"
              class="text-sm font-semibold"
            >
              {{ classroom.isGameEnabled ? 'Spill aktivert' : 'Spill ikke aktivert' }}
            </span>
          </div>
        </div>

        <div class="flex gap-2 mb-6">
          <button
            @click="activeTab = 'students'"
            :class="activeTab === 'students'
              ? 'bg-amber-800 text-white'
              : 'bg-amber-100 text-amber-900 hover:bg-amber-200'"
            class="px-5 py-2 rounded-lg font-semibold transition"
          >
            Elever ({{ students.length }})
          </button>
          <button
            @click="activeTab = 'settings'"
            :class="activeTab === 'settings'
              ? 'bg-amber-800 text-white'
              : 'bg-amber-100 text-amber-900 hover:bg-amber-200'"
            class="px-5 py-2 rounded-lg font-semibold transition"
          >
            Innstillinger
          </button>
        </div>

        <div v-if="activeTab === 'students'">
          <div v-if="students.length === 0" class="text-amber-800 text-center py-10">
            Ingen elever har blitt med ennå.
          </div>
          <ul v-else class="flex flex-col gap-3">
            <li
              v-for="student in students"
              :key="student.userId"
              class="bg-amber-50 border border-amber-300 rounded-xl px-5 py-3 flex items-center justify-between shadow-sm"
            >
              <div>
                <p class="font-bold text-amber-900">{{ student.displayName }}</p>
                <p class="text-xs text-amber-600">
                  Ble med: {{ new Date(student.joinedAt).toLocaleDateString('nb-NO') }}
                </p>
              </div>
              <div class="flex items-center gap-3">
                <span
                  v-if="!student.isAllowed"
                  class="text-xs bg-yellow-200 text-yellow-800 px-2 py-0.5 rounded-full font-semibold"
                >
                  Venter på godkjenning
                </span>
                <span
                  v-else
                  class="text-xs bg-green-200 text-green-800 px-2 py-0.5 rounded-full font-semibold"
                >
                  Godkjent
                </span>
                <button
                  v-if="!student.isAllowed"
                  @click="approveStudent(student.userId)"
                  class="text-green-700 hover:text-green-500 font-bold text-lg"
                  title="Godkjenn"
                >
                  Godkjenn
                </button>
                <button
                  @click="removeStudent(student.userId)"
                  class="text-red-700 hover:text-red-500 font-bold text-lg"
                  title="Fjern elev"
                >
                  Fjern
                </button>
              </div>
            </li>
          </ul>
        </div>

        <div v-if="activeTab === 'settings'" class="bg-amber-50 rounded-2xl p-6 shadow">
          <p class="text-amber-800 mb-2 font-semibold">Klasserom-ID:</p>
          <p class="font-mono text-sm text-amber-700 mb-4">{{ classroom.id }}</p>
          <p class="text-amber-800 mb-2 font-semibold">Opprettet:</p>
          <p class="text-sm text-amber-700">
            {{ new Date(classroom.createdAt).toLocaleDateString('nb-NO') }}
          </p>
        </div>
      </template>
    </div>
  </SceneComponent>
</template>
