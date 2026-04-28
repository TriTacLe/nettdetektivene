<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { DialogScene } from '@/types/cutscene'

const props = defineProps<{
  scene: DialogScene
}>()

const emit = defineEmits<{ done: [] }>()

const currentLine = ref(0)

function advance() {
  if (currentLine.value < props.scene.lines.length - 1) {
    currentLine.value++
  } else {
    emit('done')
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.code === 'Space' || e.code === 'Enter') {
    e.preventDefault()
    advance()
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="cutscene" @click="advance">
    <div class="cutscene-backdrop" />

    <img
      class="cutscene-character"
      :src="scene.character"
      :alt="scene.characterName"
    />

    <div class="cutscene-dialog">
      <div class="dialog-name">{{ scene.characterName }}</div>
      <div class="dialog-text">{{ scene.lines[currentLine] }}</div>
      <div class="dialog-progress">
        <span
          v-for="(_, i) in scene.lines"
          :key="i"
          class="dialog-dot"
          :class="{ active: i === currentLine }"
        />
      </div>
      <div class="dialog-hint">
        {{ currentLine < scene.lines.length - 1 ? 'Klikk for å fortsette' : 'Klikk for å gå videre' }}
        <span class="dialog-arrow">&#9654;</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cutscene {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  cursor: pointer;
  user-select: none;
}

.cutscene-backdrop {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.4) 60%, rgba(0, 0, 0, 0.2) 100%);
}

.cutscene-character {
  position: absolute;
  bottom: 160px;
  left: 5%;
  height: 55vh;
  max-height: 480px;
  object-fit: contain;
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.7));
  z-index: 1;
  pointer-events: none;
}

.cutscene-dialog {
  position: relative;
  z-index: 2;
  width: 100%;
  background: rgba(10, 10, 20, 0.92);
  border-top: 2px solid rgba(255, 255, 255, 0.15);
  padding: 1.25rem 2rem 1.25rem 42%;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

@media (max-width: 640px) {
  .cutscene-character {
    height: 38vh;
    bottom: 180px;
    left: 0;
  }

  .cutscene-dialog {
    padding: 1.25rem 1.25rem 1.25rem 1.25rem;
    padding-top: 3rem;
  }
}

.dialog-name {
  font-size: 1.1rem;
  font-weight: 700;
  color: #f0c040;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.dialog-text {
  font-size: 1.05rem;
  color: #f0f0f0;
  line-height: 1.6;
  max-width: 700px;
}

.dialog-progress {
  display: flex;
  gap: 0.4rem;
  margin-top: 0.25rem;
}

.dialog-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  transition: background 0.2s;
}

.dialog-dot.active {
  background: #f0c040;
}

.dialog-hint {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.4);
  display: flex;
  align-items: center;
  gap: 0.3rem;
  margin-top: auto;
}

.dialog-arrow {
  font-size: 0.65rem;
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.4; transform: translateX(0); }
  50% { opacity: 1; transform: translateX(3px); }
}
</style>
