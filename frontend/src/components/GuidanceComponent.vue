<script setup lang="ts">
interface Props {
  tip: string | undefined
}

defineProps<Props>()
</script>

<template>
  <div v-if="tip" class="guidance">
    <div class="guidance__header">
      <span>💡 Tips</span>
    </div>
    <div class="guidance__content">
      <p>{{ tip }}</p>
    </div>
  </div>
</template>

<style scoped>
.guidance {
  background-color: rgba(0, 0, 0, 0.7);
  border-radius: 0.5rem;
  padding: 1rem;
  margin: 1rem 0;
  color: white;
  font-size: 0.95rem;
  max-width: 100%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.guidance__header {
  font-weight: 600;
  margin-bottom: 0.5rem;
  font-size: 1rem;
}

.guidance__content {
  line-height: 1.5;
}
</style>