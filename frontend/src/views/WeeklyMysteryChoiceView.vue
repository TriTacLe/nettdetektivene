<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { getClassroomOptions } from '@/generated-client/@tanstack/vue-query.gen'
import SceneComponent from '@/components/SceneComponent.vue'

const route = useRoute()
const router = useRouter()

const classroomId = computed(() => String(route.params.classroomId))

const { data: classroomData } = useQuery(
  getClassroomOptions({ path: { id: classroomId.value } }),
)

const classroomTitle = computed(() => classroomData.value?.data?.title ?? 'Klasserom')

function goToPlay() {
  router.push({
    name: 'student-weekly-mystery-play',
    params: { classroomId: classroomId.value },
  })
}

function goToSubmit() {
  router.push({
    name: 'student-weekly-mystery-submit',
    params: { classroomId: classroomId.value },
  })
}
</script>

<template>
  <SceneComponent
    :title="classroomTitle"
    previous="/student-home"
    background="/StartBackground.jpg"
    @back="router.back()"
  >
    <div class="flex flex-1 items-center justify-center p-6">
      <div class="w-full max-w-4xl">
        <div class="mb-8 text-center text-white">
          <p class="text-sm font-bold uppercase tracking-wide text-white/70">
            Ukens mysterium
          </p>
          <h1 class="text-4xl font-black">Hva vil du gjøre?</h1>
        </div>

        <div class="grid gap-6 md:grid-cols-2">
          <button type="button" class="btn-bare choice-card" @click="goToPlay">
            <div class="icon">🕵️</div>
            <h2>Spill ukens mysterium</h2>
            <p>
              Løs lærerens valgte mysterium for denne uken og se om du klarer å avsløre hva som er mistenkelig.
            </p>
            <span>Start mysterium →</span>
          </button>

          <button type="button" class="btn-bare choice-card" @click="goToSubmit">
            <div class="icon">📩</div>
            <h2>Send inn mysterium</h2>
            <p>
              Send inn en falsk nyhet, manipulert bilde eller mistenkelig nettside som læreren kan bruke senere.
            </p>
            <span>Send inn eksempel →</span>
          </button>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.choice-card {
  min-height: 260px;
  border: 0;
  border-radius: 24px;
  padding: 2rem;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.22);
  color: #132a34;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    background 0.15s ease;
}

.choice-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 44px rgba(15, 23, 42, 0.3);
  background: white;
}

.icon {
  margin-bottom: 1rem;
  font-size: 2.5rem;
}

.choice-card h2 {
  margin: 0 0 0.75rem;
  font-size: 1.5rem;
  font-weight: 900;
}

.choice-card p {
  margin: 0 0 1.5rem;
  color: #475569;
  font-weight: 700;
  line-height: 1.5;
}

.choice-card span {
  color: #0f5c75;
  font-weight: 900;
}
</style>
