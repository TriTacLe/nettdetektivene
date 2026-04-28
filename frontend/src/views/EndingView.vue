<script setup lang="ts">
import { useRouter } from 'vue-router'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import { useDialogRunner } from '@/composables/UseDialogRunner'
import { endingDialogs, endingStartId } from '@/data/EndingDialogs'
import type { DialogChoice } from '@/types/cutscene'

const router = useRouter()
const runner = useDialogRunner(endingDialogs, endingStartId)

function finish() {
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
