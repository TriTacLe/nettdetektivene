<script setup lang="ts">
withDefaults(
  defineProps<{
    rotation?: number
    pinColor?: string
    width?: string
    height?: string
  }>(),
  {
    rotation: 0,
    pinColor: '#2b6cb0',
    width: '14rem',
    height: 'auto',
  },
)

defineEmits<{
  (e: 'click'): void
}>()
</script>

<template>
  <button
    type="button"
    class="board-note"
    :style="{ '--note-rotation': `${rotation}deg`, width, height }"
    @click="$emit('click')"
  >
    <span class="board-note-pin" :style="{ background: pinColor }" aria-hidden="true" />
    <div class="board-note-content">
      <slot />
    </div>
  </button>
</template>

<style scoped>
.board-note {
  position: relative;
  background: linear-gradient(180deg, #fffdf0 0%, #fdf6d8 100%);
  padding: 1.5rem 1.1rem 1.2rem;
  box-shadow:
    0 6px 12px rgba(0, 0, 0, 0.25),
    0 2px 4px rgba(0, 0, 0, 0.15);
  color: #1a1a1a;
  font-family: inherit;
  border: none;
  border-radius: 2px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(var(--note-rotation, 0deg)) scale(1);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.board-note:hover {
  transform: rotate(var(--note-rotation, 0deg)) scale(1.06);
  box-shadow:
    0 12px 20px rgba(0, 0, 0, 0.32),
    0 4px 8px rgba(0, 0, 0, 0.22);
}

.board-note:active {
  transform: rotate(var(--note-rotation, 0deg)) scale(1.02);
}

.board-note:focus-visible {
  outline: 2px solid #1c7ed6;
  outline-offset: 3px;
}

.board-note-pin {
  position: absolute;
  top: -0.4rem;
  left: 50%;
  transform: translateX(-50%);
  width: 0.9rem;
  height: 0.9rem;
  border-radius: 50%;
  box-shadow:
    inset -2px -2px 3px rgba(0, 0, 0, 0.35),
    inset 2px 2px 3px rgba(255, 255, 255, 0.45),
    0 2px 3px rgba(0, 0, 0, 0.4);
}

.board-note-content {
  width: 100%;
}
</style>
