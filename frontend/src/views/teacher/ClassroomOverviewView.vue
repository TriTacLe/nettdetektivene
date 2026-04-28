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
      <div class="menu-stack">
        <div
          v-if="error"
          class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded text-sm"
        >
          {{ error }}
        </div>

        <button class="btn-vn menu-item" @click="router.push({ name: 'create-classroom' })">
          <span class="menu-text">
            <span class="menu-title">+ Nytt klasserom</span>
            <span class="menu-subtitle">Opprett et nytt klasserom</span>
          </span>
          <span class="menu-arrow" aria-hidden="true">›</span>
        </button>

        <div v-if="isLoading" class="text-center text-white py-6">Laster...</div>
        <div v-else-if="myClassrooms.length === 0" class="text-center text-white py-6 text-sm">
          Du har ingen klasserom enda.
        </div>

        <div
          v-for="classroom in myClassrooms"
          :key="classroom.id"
          class="classroom-card"
        >
          <div class="classroom-card-info">
            <p class="classroom-card-title">{{ classroom.title }}</p>
            <p class="classroom-card-meta">
              Kode: <span class="font-mono font-bold">{{ classroom.joinCode }}</span>
            </p>
            <p v-if="classroom.classDesc" class="classroom-card-desc">
              {{ classroom.classDesc }}
            </p>
          </div>
          <div class="classroom-card-actions">
            <button
              class="btn-vn overview-action-btn"
              @click="classroom.id && openClassroom(classroom.id)"
            >
              Åpne
            </button>
            <button
              class="btn-vn btn-vn-danger overview-action-btn"
              @click="classroom.id && deleteClassroom(classroom.id)"
            >
              Slett
            </button>
          </div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.classroom-card {
  background: rgba(255, 251, 235, 0.95);
  border: 1px solid rgba(252, 211, 77, 0.6);
  border-radius: 0.75rem;
  padding: 0.9rem 1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
}
.classroom-card-info { flex: 1; min-width: 0; }
.classroom-card-title {
  font-size: 1.1rem;
  font-weight: 800;
  color: #78350f;
  margin: 0;
}
.classroom-card-meta {
  font-size: 0.85rem;
  color: #92400e;
  margin: 0.15rem 0 0;
}
.classroom-card-desc {
  font-size: 0.85rem;
  color: #b45309;
  margin: 0.25rem 0 0;
}
.classroom-card-actions {
  display: flex;
  gap: 0.4rem;
  flex-shrink: 0;
}
</style>
