<script setup lang="ts">
defineProps<{ pageName: string }>()
</script>

<template>
  <div class="test-page">
    <h2>{{ pageName }} page</h2>
  </div>
</template>

<style scoped>
.test-page {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64px;
  color: white;
}
</style>
