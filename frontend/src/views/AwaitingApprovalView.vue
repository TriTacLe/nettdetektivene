<script setup lang="ts">
import { useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery } from '@tanstack/vue-query'
import { getMyClassroomOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import { useStudentStore } from '@/stores/StudentStore.ts'
import { watch } from 'vue'

const router = useRouter()
const studentStore = useStudentStore()

const { data } = useQuery({
  ...getMyClassroomOptions(),
  refetchInterval: 5000,
  retry: false,
})

watch(data, (val) => {
  const classroom = val?.data
  if (classroom?.id && classroom?.institutionId) {
    studentStore.setClassroom(classroom.id, classroom.institutionId)
    router.push({ name: 'introduction' })
  }
})
</script>

<template>
  <SceneComponent title="Venter på godkjenning" previous="/join-classroom">
    <div class="flex flex-col items-center justify-center flex-1 px-6">
      <div class="flex flex-col gap-4 w-full max-w-sm items-center text-center">
        <div class="spinner" />
        <h2 class="awaiting-title">
          Venter på godkjenning
        </h2>
        <p class="awaiting-text">
          Læreren din må godkjenne forespørselen din før du kan bli med i klasserommet.
        </p>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.awaiting-title {
  color: white;
  font-weight: 700;
  font-size: 1.25rem;
  -webkit-text-stroke: 1.5px #000;
  paint-order: stroke fill;
}

.awaiting-text {
  color: white;
  font-weight: 600;
  font-size: 1rem;
  -webkit-text-stroke: 1px #000;
  paint-order: stroke fill;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 5px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
