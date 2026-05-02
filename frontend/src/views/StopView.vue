<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import { useStopStore } from '@/stores/StopStore'
import { STOPS } from '@/data/StopData'
import { STOP_PICKERS } from '@/data/taskDialogs/pickers'
import type { DialogChoice, DialogScene } from '@/types/cutscene'

const route = useRoute()
const router = useRouter()
const stopStore = useStopStore()

const stopId = route.params.stopId as string
const stop = computed(() => STOPS[stopId])
const testClassroomId = stopStore.testModeClassroomId
const previousRoute = computed(() =>
  testClassroomId ? `/classroom/${testClassroomId}/test-game` : '/game-map',
)

const tasks = computed(() => stopStore.tasks[stopId] ?? [])
const isLoading = computed(() => stopStore.loading[stopId] ?? false)

const taskHints = computed(
  () => stop.value?.taskHints as Record<string, { hint?: string }> | undefined,
)

const startAtLastLine = ref(stopStore.hasSeenPicker(stopId))

onMounted(() => {
  stopStore.markPickerSeen(stopId)
  if (testClassroomId) {
    stopStore.loadTasksTestMode(testClassroomId, stopId)
  } else {
    stopStore.clearTasks(stopId)
    stopStore.loadTasks(stopId, true)
  }
})

const choices = computed<DialogChoice[]>(() => {
  if (isLoading.value) return []
  if (tasks.value.length === 0) {
    return [
      {
        label: 'Ingen oppgaver tilgjengelig ennå',
        next: '__noop__',
        disabled: true,
      },
    ]
  }
  return tasks.value.map((task) => ({
    label: task.title,
    next: '__noop__',
    flag: task.uuid,
    disabled: !task.isUnlocked,
    done: stopStore.isComplete(stopId, task.uuid),
  }))
})

const scene = computed<DialogScene | null>(() => {
  const template = STOP_PICKERS[stopId]
  if (!template) return null
  return {
    ...template,
    choices: choices.value,
  }
})

function handleChoose(choice: DialogChoice) {
  if (choice.disabled || !choice.flag || !stop.value) return
  const task = tasks.value.find((t) => t.uuid === choice.flag)
  if (!task) return
  router.push(`${stop.value.taskRoute}/${task.uuid}`)
}

function handleBack() {
  router.push(previousRoute.value)
}

function handleDone() {
  router.push(previousRoute.value)
}
</script>

<template>
  <CutsceneDialog
    v-if="stop && scene"
    :key="stopId"
    :scene="scene"
    :start-at-last-line="startAtLastLine"
    @choose="handleChoose"
    @back="handleBack"
    @done="handleDone"
  />

  <div v-else-if="!stop" class="not-found">
    <p>Stoppested ikke funnet.</p>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  color: white;
  font-size: 1.5rem;
}
</style>
