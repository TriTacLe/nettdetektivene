<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import type { DialogChoice, DialogScene } from '@/types/cutscene'
import { useTypewriter } from '@/composables/UseTypewriter'
import { useAudioStore } from '@/stores/AudioStore'

const props = defineProps<{
  scene: DialogScene
  charsPerSecond?: number
}>()

const audio = useAudioStore()
const { textSpeed } = storeToRefs(audio)

const emit = defineEmits<{
  done: []
  choose: [choice: DialogChoice]
}>()

const lineIndex = ref(0)
// Prefer explicit prop; otherwise read live text speed from preferences.
const charsPerSecond = computed(() => props.charsPerSecond ?? textSpeed.value)
watch(() => props.scene.id, () => { lineIndex.value = 0 })

const currentLine = computed(() => props.scene.lines[lineIndex.value])
const currentText = computed(() => currentLine.value?.text ?? '')
const currentMood = computed(() => currentLine.value?.mood ?? 'neutral')

const isLastLine = computed(() => lineIndex.value === props.scene.lines.length - 1)
const showChoices = computed(() =>
  isLastLine.value && Boolean(props.scene.choices && props.scene.choices.length > 0),
)

const { visibleText, isComplete, skip } = useTypewriter({
  source: currentText,
  charsPerSecond,
  onTick: () => audio.playTypingTick(),
})

function advance() {
  if (!isComplete.value) {
    skip()
    return
  }
  if (showChoices.value) return
  if (lineIndex.value < props.scene.lines.length - 1) {
    lineIndex.value++
  } else {
    emit('done')
  }
}

function choose(choice: DialogChoice) {
  if (!isComplete.value) {
    skip()
    return
  }
  emit('choose', choice)
}

function onKeydown(e: KeyboardEvent) {
  if (e.code === 'Space' || e.code === 'Enter') {
    e.preventDefault()
    advance()
    return
  }
  if (showChoices.value && /^Digit[1-4]$/.test(e.code)) {
    const idx = Number(e.code.replace('Digit', '')) - 1
    const choice = props.scene.choices?.[idx]
    if (choice) {
      e.preventDefault()
      choose(choice)
    }
  }
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div
    class="cutscene"
    :data-mood="currentMood"
    @click="advance"
  >
    <div
      v-if="scene.background"
      class="cutscene-background"
      :style="{ backgroundImage: `url('${scene.background}')` }"
    />
    <div class="cutscene-backdrop" />

    <img
      class="cutscene-character"
      :class="`mood-${currentMood}`"
      :src="scene.character"
      :alt="scene.characterName"
    />

    <div class="cutscene-dialog vn-panel" @click.stop="advance">
      <div class="dialog-name">{{ scene.characterName }}</div>
      <div class="dialog-text">
        {{ visibleText }}<span v-if="!isComplete" class="dialog-caret">|</span>
      </div>

      <div v-if="showChoices" class="dialog-choices">
        <button
          v-for="(choice, i) in scene.choices"
          :key="choice.next + i"
          class="dialog-choice"
          @click.stop="choose(choice)"
        >
          <span class="dialog-choice-key">{{ i + 1 }}</span>
          <span class="dialog-choice-label">{{ choice.label }}</span>
        </button>
      </div>

      <div v-else class="dialog-progress">
        <span
          v-for="(_, i) in scene.lines"
          :key="i"
          class="dialog-dot"
          :class="{ active: i === lineIndex }"
        />
      </div>

      <div v-if="!showChoices" class="dialog-hint">
        <template v-if="!isComplete">Klikk for å hoppe over</template>
        <template v-else-if="!isLastLine">Klikk for å fortsette</template>
        <template v-else>Klikk for å gå videre</template>
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
  overflow: hidden;
}

.cutscene-background {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: brightness(0.7) saturate(1.05);
  transform: scale(1.04);
  transition: transform 6s ease-out;
}
.cutscene[data-mood] .cutscene-background {
  transform: scale(1.08);
}

.cutscene-backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at center, rgba(0, 0, 0, 0) 30%, rgba(0, 0, 0, 0.55) 100%),
    linear-gradient(to top, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.4) 60%, rgba(0, 0, 0, 0.2) 100%);
}

.cutscene-character {
  position: absolute;
  bottom: 180px;
  left: 5%;
  height: 60vh;
  max-height: 520px;
  object-fit: contain;
  filter: drop-shadow(0 12px 28px rgba(0, 0, 0, 0.75));
  z-index: 1;
  pointer-events: none;
  animation: portrait-idle 4.5s ease-in-out infinite;
  transform-origin: bottom center;
  transition: filter 0.3s ease, transform 0.3s ease;
}

.cutscene-character.mood-happy { filter: drop-shadow(0 12px 28px rgba(56, 189, 124, 0.45)) brightness(1.05); }
.cutscene-character.mood-angry { filter: drop-shadow(0 12px 28px rgba(229, 72, 77, 0.45)) brightness(1.02) saturate(1.15); }
.cutscene-character.mood-sus   { filter: drop-shadow(0 12px 28px rgba(245, 166, 35, 0.45)) brightness(0.95); }
.cutscene-character.mood-thinking { filter: drop-shadow(0 12px 28px rgba(120, 180, 255, 0.45)) brightness(0.98); }

@keyframes portrait-idle {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-6px); }
}

.cutscene-dialog {
  position: relative;
  z-index: 2;
  width: 100%;
  padding: 1.5rem 2rem 1.5rem 42%;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

@media (max-width: 640px) {
  .cutscene-character { height: 38vh; bottom: 220px; left: 0; }
  .cutscene-dialog { padding: 1.25rem; padding-top: 3rem; min-height: 180px; }
}

.dialog-name {
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-nd-glow, #f0c040);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-shadow: 0 0 12px rgba(240, 192, 64, 0.35);
}

.dialog-text {
  font-size: 1.1rem;
  color: #f4f4f4;
  line-height: 1.65;
  max-width: 720px;
  min-height: 3.4em;
}

.dialog-caret {
  display: inline-block;
  margin-left: 2px;
  animation: caret-blink 0.9s steps(1) infinite;
  color: var(--color-nd-glow, #f0c040);
}
@keyframes caret-blink { 50% { opacity: 0; } }

.dialog-progress { display: flex; gap: 0.4rem; margin-top: 0.25rem; }
.dialog-dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255, 255, 255, 0.25); transition: background 0.2s; }
.dialog-dot.active { background: var(--color-nd-glow, #f0c040); }

.dialog-choices {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.5rem;
  max-width: 720px;
}

.dialog-choice {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.7rem 1rem;
  background: rgba(20, 24, 40, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 0.5rem;
  color: #f4f4f4;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.12s ease;
}
.dialog-choice:hover, .dialog-choice:focus-visible {
  background: rgba(40, 48, 75, 0.95);
  border-color: var(--color-nd-glow, #f0c040);
  transform: translateX(3px);
  outline: none;
}

.dialog-choice-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.6rem;
  height: 1.6rem;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.35rem;
  font-size: 0.85rem;
  color: var(--color-nd-glow, #f0c040);
  font-weight: 800;
  flex-shrink: 0;
}

.dialog-choice-label { flex: 1; }

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
  animation: arrow-pulse 1.2s ease-in-out infinite;
}
@keyframes arrow-pulse {
  0%, 100% { opacity: 0.4; transform: translateX(0); }
  50%      { opacity: 1; transform: translateX(3px); }
}

/* VN-style panel base — overridable in main.css if --color-nd-panel is defined */
.vn-panel {
  background: var(--color-nd-panel, rgba(10, 10, 20, 0.92));
  border-top: 2px solid rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(6px);
}
</style>
