<script setup lang="ts">
import NavigationHeader from './components/NavigationHeader.vue'
</script>

<template>
  <div class="app">
    <NavigationHeader previous="/youtube" previous-label="Tilbake" title="Lag din avatar" next="/github" next-label="Fortsett" />
    <RouterView />
  </div>
</template>

<style scoped></style>
