<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery } from '@tanstack/vue-query'
import { getClassroomOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'

const route = useRoute()
const router = useRouter()
const classroomId = route.params.id as string

const showJoinCodeModal = ref(false)

const { data } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroom = computed(() => data.value?.data ?? null)

const menuItems = [
  {
    title: 'Oversikt Elever',
    subtitle: 'Se og administrer elever',
    route: 'classroom-students',
  },
  {
    title: 'Oversikt Oppgaver',
    subtitle: 'Oppgaver i spillet',
    route: '',
  },
  {
    title: 'Varslinger',
    subtitle: 'Meldinger fra klassen',
    route: 'teacher-notifications',
  },
  {
    title: 'Ukens Mysterium',
    subtitle: 'Se og godkjenn innlegg',
    route: '',
  },
  {
    title: 'Klasseromsstyring',
    subtitle: 'Rediger innstillinger',
    route: 'classroom-settings',
  },
  {
    title: 'Elev Progresjon',
    subtitle: 'Fremgang per elev',
    route: '',
  },
  {
    title: 'Test Spillet',
    subtitle: 'Spill som elev',
    route: '',
  },
  {
    title: 'Vis Klasseromskode',
    subtitle: 'Del kode med elever',
    route: 'join-code',
  },
]

function handleMenuItem(item: (typeof menuItems)[0]) {
  if (!item.route) return
  if (item.route === 'join-code') {
    showJoinCodeModal.value = true
    return
  }
  if (item.route === 'teacher-notifiactions') {
    router.push({ name: 'teacher-notifications', query: { classroomId } })
    return
  }
  router.push({ name: item.route, params: { id: classroomId } })
}
</script>

<template>
  <SceneComponent
    :title="classroom?.title || 'Klasserom'"
    previous="/classroom-overview"
    background="/StartBackground.jpg"
  >
    <div class="p-6 grid grid-cols-2 gap-3">
      <button
        v-for="item in menuItems"
        :key="item.title"
        :disabled="!item.route"
        class="bg-white rounded-xl p-4 flex items-center justify-between text-left shadow transition hover:brightness-95 disabled:opacity-50 disabled:cursor-not-allowed"
        @click="handleMenuItem(item)"
      >
        <div>
          <p class="font-bold text-gray-900 text-sm">{{ item.title }}</p>
          <p class="text-gray-500 text-xs mt-0.5">{{ item.subtitle }}</p>
        </div>
        <span class="text-gray-400 text-lg">›</span>
      </button>
    </div>
  </SceneComponent>

  <Teleport to="body">
    <div
      v-if="showJoinCodeModal"
      class="fixed inset-0 bg-black/60 flex items-center justify-center z-50"
      @click.self="showJoinCodeModal = false"
    >
      <div class="bg-white rounded-2xl p-8 text-center shadow-xl max-w-xs w-full mx-4">
        <h2 class="text-xl font-bold mb-2">Klasseromskode</h2>
        <p class="text-5xl font-mono font-black tracking-widest text-indigo-600 my-6">
          {{ classroom?.joinCode }}
        </p>
        <button
          class="w-full bg-gray-900 text-white font-bold py-3 rounded-xl"
          @click="showJoinCodeModal = false"
        >
          Lukk
        </button>
      </div>
    </div>
  </Teleport>
</template>
