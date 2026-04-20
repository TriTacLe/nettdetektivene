<script setup lang="ts">
import SceneComponent from '../components/SceneComponent.vue'
import NyhetskvartaletComponent from '../components/NyhetskvartaletComponent.vue'
import type { NewsChallenge } from '../types'

// Placeholder — replace with backend fetch when API is ready
const challenge: NewsChallenge = {
  articles: [
    { imagePath: '/NyhetTest.png', isReal: false },
    { imagePath: '/NyhetTest.png', isReal: true },
  ],
}
</script>

<template>
  <SceneComponent title="Nyhetskvartalet" previous="/game-map" background="/NyhetskvartaletBackground.png">
    <NyhetskvartaletComponent :challenge="challenge" />
  </SceneComponent>
</template>
