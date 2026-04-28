<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import introductionDialogs from '@/data/IntroductionDialogs'

const router = useRouter()
const currentScene = ref(0)

function onSceneDone() {
  if (currentScene.value < introductionDialogs.length - 1) {
    currentScene.value++
  } else {
    router.push({ name: 'student-home' })
  }
}
</script>

<template>
  <div class="introduction-cutscene">
    <img class="intro-background" src="/StartBackground.jpg" alt="" aria-hidden="true" />
    <CutsceneDialog
      :key="currentScene"
      :scene="introductionDialogs[currentScene]"
      @done="onSceneDone"
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
