<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import { useDialogRunner } from '@/composables/UseDialogRunner'
import { endingDialogs, endingStartId } from '@/data/EndingDialogs'
import { useStoryProgressStore, StoryStep } from '@/stores/StoryProgressStore'
import { useStopStore } from '@/stores/StopStore'
import type { DialogChoice } from '@/types/cutscene'

const router = useRouter()
const storyStore = useStoryProgressStore()
const stopStore = useStopStore()
const runner = useDialogRunner(endingDialogs, endingStartId)

function finish() {
  if (!stopStore.testModeClassroomId) storyStore.advance(StoryStep.ENDING)
  router.push({ name: 'stop-finished', query: { stop: 'datasenteret' } })
}

function onDone() {
  runner.advance()
  if (runner.isFinished.value) finish()
}

function onChoose(choice: DialogChoice) {
  runner.advance(choice)
  if (runner.isFinished.value) finish()
}

onMounted(() => {
  if (!stopStore.testModeClassroomId && storyStore.hasSeen(StoryStep.ENDING)) {
    finish()
  }
})
</script>

<template>
  <div class="ending-cutscene">
    <img class="ending-background" src="/datasenteret.jpg" alt="" aria-hidden="true" />
    <CutsceneDialog
      v-if="runner.currentScene.value"
      :key="runner.currentScene.value.id"
      :scene="runner.currentScene.value"
      @done="onDone"
      @choose="onChoose"
    />
  </div>
</template>

<style scoped>
.ending-cutscene {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.ending-background {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}
</style>
