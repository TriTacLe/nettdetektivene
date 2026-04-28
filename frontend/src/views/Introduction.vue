<script setup lang="ts">
import { useRouter } from 'vue-router'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import { useDialogRunner } from '@/composables/UseDialogRunner'
import { introductionDialogs, introductionStartId } from '@/data/IntroductionDialogs'
import type { DialogChoice } from '@/types/cutscene'

const router = useRouter()
const runner = useDialogRunner(introductionDialogs, introductionStartId)

function onDone() {
  runner.advance()
  if (runner.isFinished.value) {
    router.push({ name: 'student-home' })
  }
}

function onChoose(choice: DialogChoice) {
  runner.advance(choice)
  if (runner.isFinished.value) {
    router.push({ name: 'student-home' })
  }
}
</script>

<template>
  <div class="introduction-cutscene">
    <img class="intro-background" src="/StartBackground.jpg" alt="" aria-hidden="true" />
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
.introduction-cutscene {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.intro-background {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
}
</style>
