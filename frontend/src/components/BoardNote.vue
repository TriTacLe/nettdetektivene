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

// Randomize the idle-sway phase per instance so notes don't all bob in sync.
const swayDelay = `-${(Math.random() * 4).toFixed(2)}s`
const swayDuration = `${(4 + Math.random() * 1.5).toFixed(2)}s`
</script>

<template>
  <button
    type="button"
    class="board-note btn-bare"
    :style="{
      '--note-rotation': `${rotation}deg`,
      '--sway-delay': swayDelay,
      '--sway-duration': swayDuration,
      width,
      height,
    }"
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
  background-image:
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='300'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='300' height='300' filter='url(%23n)' opacity='0.045'/%3E%3C/svg%3E"),
    linear-gradient(160deg, #fefcf5 0%, #faf5e4 60%, #f5ecce 100%);
  background-size: 300px 300px, 100% 100%;
  padding: 1.5rem 1.1rem 1.2rem;
  box-shadow:
    0 2px 2px rgba(0, 0, 0, 0.12),
    0 6px 14px rgba(0, 0, 0, 0.22),
    0 12px 20px -4px rgba(0, 0, 0, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    inset -1px 0 0 rgba(0, 0, 0, 0.04),
    inset 0 -1px 0 rgba(0, 0, 0, 0.06);
  color: #1a1a1a;
  font-family: inherit;
  border: none;
  border-radius: 1px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transform-origin: top center;
  transform: rotate(var(--note-rotation, 0deg)) translateY(0);
  animation: note-sway var(--sway-duration, 5s) ease-in-out var(--sway-delay, 0s) infinite;
  transition: transform 0.2s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

@keyframes note-sway {
  0%, 100% { transform: rotate(var(--note-rotation, 0deg)) translateY(0); }
  50%      { transform: rotate(calc(var(--note-rotation, 0deg) + 0.4deg)) translateY(-2px); }
}

.board-note:hover {
  animation: none;
  transform: rotate(var(--note-rotation, 0deg)) scale(1.07) translateY(-4px);
  box-shadow:
    0 16px 28px rgba(0, 0, 0, 0.38),
    0 6px 10px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(245, 166, 35, 0.25);
  filter: saturate(1.08) brightness(1.02);
  z-index: 5;
}

.board-note:active {
  transform: rotate(var(--note-rotation, 0deg)) scale(1.03) translateY(-1px);
}

.board-note:focus-visible {
  outline: 2px solid #1c7ed6;
  outline-offset: 3px;
}

@media (prefers-reduced-motion: reduce) {
  .board-note { animation: none; }
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
