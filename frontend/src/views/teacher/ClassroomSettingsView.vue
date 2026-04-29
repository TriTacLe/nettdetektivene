<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ClassroomService from '@/services/ClassroomService.ts'
import SceneComponent from '@/components/SceneComponent.vue'

const route = useRoute()
const router = useRouter()

const classroomId = route.params.id as string
const error = ref<string | null>(null)

async function deleteClassroom() {
  if (!confirm('Er du sikker på at du vil slette klasserommet? Dette kan ikke angres.')) return
  try {
    await ClassroomService.deleteClassroom(classroomId)
    router.push({ name: 'teacher-home' })
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke slette klasserommet'
  }
}

const menuItems = [
  {
    title: 'Rediger Klasserom',
    subtitle: 'Endre navn og innstillinger',
    action: () => router.push({ name: 'edit-classroom', params: { id: classroomId } }),
  },
  {
    title: 'Slett Klasserom',
    subtitle: 'Slett klasserommet permanent',
    action: deleteClassroom,
    danger: true,
  },
  {
    title: 'Inviter Lærer',
    subtitle: 'Legg til en medinstruktør',
    action: () => {},
    disabled: true,
  },
]
</script>

<template>
  <SceneComponent
    title="Klasseromsstyring"
    :previous="`/classroom/${classroomId}`"
    background="/StartBackground.jpg"
  >
    <div class="flex flex-col items-center flex-1 w-full px-6 pb-10 pt-4">
      <div class="menu-stack">
        <div
          v-if="error"
          class="bg-red-100 border border-red-400 text-red-800 px-4 py-3 rounded text-sm"
        >
          {{ error }}
        </div>

        <button
          v-for="item in menuItems"
          :key="item.title"
          :disabled="item.disabled"
          class="btn-vn menu-item"
          :class="{ 'btn-vn-danger': item.danger }"
          @click="item.action()"
        >
          <span class="menu-text">
            <span class="menu-title">{{ item.title }}</span>
            <span class="menu-subtitle">{{ item.subtitle }}</span>
          </span>
          <span v-if="!item.disabled" class="menu-arrow" aria-hidden="true">›</span>
        </button>
      </div>
    </div>
  </SceneComponent>
</template>
