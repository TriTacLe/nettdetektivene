<script setup lang="ts">
interface Props {
  tip: string | undefined
}

defineProps<Props>()
</script>

<template>
  <p v-if="tip" class="guidance">
    <span class="guidance__icon" aria-hidden="true">💡</span>
    <span class="guidance__label">Tips:</span>
    <span class="guidance__text">{{ tip }}</span>
  </p>
</template>

<style scoped>
.guidance {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: center;
  gap: 0.55rem;
  margin: 0.6rem auto 0;
  padding: 0;
  max-width: 70ch;
  color: #ffffff;
  font-size: clamp(1.1rem, 1.6vw, 1.35rem);
  font-style: italic;
  font-weight: 600;
  line-height: 1.45;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.9), 0 0 2px rgba(0, 0, 0, 0.9);
  background: transparent;
}

.guidance__icon {
  font-style: normal;
  font-size: 1.2em;
}

.guidance__label {
  font-weight: 900;
  font-style: normal;
  color: var(--color-nd-glow, #f0c040);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.95), 0 0 2px rgba(0, 0, 0, 0.9);
}
</style>
