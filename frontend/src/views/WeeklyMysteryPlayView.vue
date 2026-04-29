<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'

const route = useRoute()
const router = useRouter()

const classroomId = computed(() => String(route.params.classroomId))
</script>

<template>
  <SceneComponent
    title="Ukens mysterium"
    background="/StartBackground.jpg"
    @previous="router.push({ name: 'student-weekly-mystery', params: { classroomId } })"
  >
    <div class="flex flex-1 items-center justify-center p-6">
      <div class="w-full max-w-3xl rounded-3xl bg-white/95 p-8 shadow-xl">
        <p class="mb-2 text-sm font-black uppercase tracking-wide text-slate-500">
          Spill mysterium
        </p>

        <h1 class="mb-4 text-3xl font-black text-slate-900">
          Ukens mysterium kommer her
        </h1>

        <p class="text-base font-semibold leading-7 text-slate-600">
          Denne siden kan kobles til oppgaven læreren lager fra en mystery submission.
          Neste steg er å hente aktiv mystery-task for klasserommet og vise riktig oppgavekomponent.
        </p>
      </div>
    </div>
  </SceneComponent>
</template>
