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
    router.push({ name: 'classroom-overview' })
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
      <div class="flex flex-col gap-3 w-full max-w-md mx-auto">
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
          @click="item.action()"
          class="w-full bg-white rounded-2xl shadow px-5 py-4 flex items-center gap-4 transition text-left"
          :class="
            item.disabled
              ? 'opacity-50 cursor-not-allowed'
              : item.danger
                ? 'hover:bg-red-50'
                : 'hover:bg-gray-50'
          "
        >
          <div class="flex-1">
            <p
              class="font-bold text-base leading-tight"
              :class="item.danger ? 'text-red-700' : 'text-gray-900'"
            >
              {{ item.title }}
            </p>
            <p class="text-gray-500 text-sm mt-0.5">{{ item.subtitle }}</p>
          </div>
          <span v-if="!item.disabled" class="text-gray-400 text-lg">›</span>
        </button>
      </div>
    </div>
  </SceneComponent>
</template>
