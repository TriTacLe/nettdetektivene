<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, onUnmounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import type { DialogChoice, DialogScene } from '@/types/cutscene'
import { useTypewriter } from '@/composables/UseTypewriter'
import { useAudioStore } from '@/stores/AudioStore'

const props = defineProps<{
  scene: DialogScene
  charsPerSecond?: number
  startAtLastLine?: boolean
}>()

const audio = useAudioStore()
const { textSpeed } = storeToRefs(audio)

const emit = defineEmits<{
  done: []
  choose: [choice: DialogChoice]
  back: []
}>()

// `back` is declared in defineEmits, so Vue strips `onBack` out of $attrs.
// Read it off the vnode props instead, which always carries listeners.
const instance = getCurrentInstance()
const showBack = computed(() => Boolean(instance?.vnode.props?.onBack))

const lineIndex = ref(props.startAtLastLine ? Math.max(0, props.scene.lines.length - 1) : 0)
// Prefer explicit prop; otherwise read live text speed from preferences.
const charsPerSecond = computed(() => props.charsPerSecond ?? textSpeed.value)
watch(() => props.scene.id, () => {
  lineIndex.value = props.startAtLastLine ? Math.max(0, props.scene.lines.length - 1) : 0
})

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
  if (choice.disabled) return
  if (!isComplete.value) {
    skip()
    return
  }
  emit('choose', choice)
}

function onKeydown(e: KeyboardEvent) {
  if (showBack.value && e.code === 'Escape') {
    e.preventDefault()
    emit('back')
    return
  }
  const active = document.activeElement as HTMLElement | null
  const onChoiceButton = active?.classList.contains('dialog-choice')
  if (e.code === 'Space' || e.code === 'Enter') {
    if (onChoiceButton) return
    e.preventDefault()
    advance()
    return
  }
  if (showChoices.value && /^Digit[1-9]$/.test(e.code)) {
    const idx = Number(e.code.replace('Digit', '')) - 1
    const choice = props.scene.choices?.[idx]
    if (choice && !choice.disabled) {
      e.preventDefault()
      choose(choice)
    }
  }
}

onMounted(() => {
  if (props.startAtLastLine) skip()
  window.addEventListener('keydown', onKeydown)
})
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
    <div class="cutscene-tint" aria-hidden="true" />

    <button
      v-if="showBack"
      type="button"
      class="cutscene-back btn-bare"
      aria-label="Tilbake"
      @click.stop="emit('back')"
    >
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <circle cx="12" cy="12" r="12" fill="white" />
        <path
          d="M14 8L10 12L14 16"
          stroke="black"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </button>

    <img
      class="cutscene-character"
      :class="`mood-${currentMood}`"
      :src="scene.character"
      :alt="scene.characterName"
    />

    <div v-if="scene.featureImage" class="cutscene-feature" aria-hidden="true">
      <img class="cutscene-feature-img" :src="scene.featureImage" alt="" />
      <p v-if="scene.featureLabel" class="cutscene-feature-label">{{ scene.featureLabel }}</p>
    </div>

    <div class="cutscene-text">
      <div class="dialog-name">{{ scene.characterName }}</div>
      <div class="dialog-text">
        {{ visibleText }}<span v-if="!isComplete" class="dialog-caret">|</span>
      </div>

      <div v-if="showChoices" class="dialog-choices btn-stack">
        <button
          v-for="(choice, i) in scene.choices"
          :key="choice.next + i"
          type="button"
          class="btn-vn dialog-choice"
          :class="{
            'dialog-choice--locked': choice.disabled,
            'dialog-choice--done': choice.done,
          }"
          :disabled="choice.disabled"
          :aria-disabled="choice.disabled || undefined"
          @click.stop="choose(choice)"
        >
          <span class="dialog-choice-key">
            <svg
              v-if="choice.disabled"
              xmlns="http://www.w3.org/2000/svg"
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
              aria-hidden="true"
            >
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            <span v-else-if="choice.done" class="dialog-choice-check" aria-hidden="true">&#10003;</span>
            <template v-else>{{ i + 1 }}</template>
          </span>
          <span class="dialog-choice-body">
            <span class="dialog-choice-label">{{ choice.label }}</span>
            <span v-if="choice.hint" class="dialog-choice-hint">{{ choice.hint }}</span>
          </span>
        </button>
      </div>
    </div>

    <div class="dialog-corner">
      <div v-if="!showChoices" class="dialog-progress">
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
  cursor: pointer;
  user-select: none;
  overflow: hidden;
}

.cutscene-background {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  z-index: 0;
}

.cutscene-back {
  position: absolute;
  top: 1rem;
  left: 1rem;
  z-index: 4;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: none;
  border: none;
  cursor: pointer;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.6));
  transition: opacity 0.15s ease, transform 0.1s ease;
}

.cutscene-back:hover { opacity: 0.85; }
.cutscene-back:active { transform: scale(0.96); }

.cutscene-back:focus-visible {
  outline: 2px solid #ffd447;
  outline-offset: 4px;
  border-radius: 50%;
}

/* Dark layer between the background and the character — bumps text
   contrast without fully obscuring the scene. Stronger at the bottom
   where the dialog text sits. */
.cutscene-tint {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
    linear-gradient(to top, rgba(0, 0, 0, 0.88) 0%, rgba(0, 0, 0, 0.75) 45%, rgba(0, 0, 0, 0.6) 100%);
}

.cutscene-character {
  position: absolute;
  bottom: 0;
  right: 4%;
  height: 95vh;
  max-height: 100vh;
  width: auto;
  object-fit: contain;
  object-position: bottom right;
  z-index: 2;
  pointer-events: none;
  animation: portrait-idle 4.5s ease-in-out infinite;
  transform-origin: bottom center;
}

@keyframes portrait-idle {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-6px); }
}

/* Text + choices float bottom-center, free-floating on the scene. */
.cutscene-text {
  position: absolute;
  left: 50%;
  bottom: 16vh;
  transform: translateX(-50%);
  z-index: 3;
  width: min(70vw, 60rem);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.65rem;
}

.cutscene-feature {
  position: absolute;
  top: 24vh;
  left: 18vw;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
  padding: 0.85rem 0.85rem 1rem;
  width: clamp(11rem, 18vw, 17rem);
  background:
    linear-gradient(170deg, #fefdf8 0%, #f8f2e0 40%, #f2e8cc 75%, #ede0bc 100%);
  box-shadow:
    0 14px 24px rgba(0, 0, 0, 0.5),
    0 4px 8px rgba(0, 0, 0, 0.3);
  transform: rotate(-4deg);
  pointer-events: none;
  animation: feature-idle 5s ease-in-out infinite;
  transform-origin: center;
}

.cutscene-feature-img {
  display: block;
  width: 100%;
  height: auto;
  aspect-ratio: 1 / 1;
  object-fit: contain;
  border-radius: 2px;
  mix-blend-mode: multiply;
}

.cutscene-feature-label {
  margin: 0;
  font-weight: 800;
  font-size: clamp(0.95rem, 1.1vw, 1.2rem);
  color: #2f2418;
  text-align: center;
  line-height: 1.15;
  text-wrap: balance;
}

@keyframes feature-idle {
  0%, 100% { transform: rotate(-4deg) translateY(0); }
  50%      { transform: rotate(-4deg) translateY(-4px); }
}

@media (max-width: 640px) {
  .cutscene-character { height: 70vh; right: 0; }
  .cutscene-text { width: calc(100% - 2.5rem); bottom: 6rem; }
  .cutscene-feature {
    bottom: auto;
    top: 1.2rem;
    left: 1.2rem;
    width: clamp(5rem, 22vw, 7.5rem);
    padding: 0.4rem 0.4rem 0.5rem;
    gap: 0.3rem;
  }
  .cutscene-feature-label { font-size: 0.7rem; }
}

.dialog-name {
  font-size: 1.5rem;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.04em;
  text-shadow:
    0 2px 6px rgba(0, 0, 0, 0.95),
    0 0 18px rgba(0, 0, 0, 0.7);
}

.dialog-text {
  font-size: 1.9rem;
  font-weight: 500;
  color: #ffffff;
  line-height: 1.45;
  min-height: 2.9em;
  text-shadow:
    0 2px 4px rgba(0, 0, 0, 0.95),
    0 0 14px rgba(0, 0, 0, 0.75);
}

@media (max-width: 640px) {
  .dialog-text {
    font-size: 1.5rem;
  }
}

.dialog-caret {
  display: inline-block;
  margin-left: 2px;
  animation: caret-blink 0.9s steps(1) infinite;
  color: #ffffff;
  text-shadow: inherit;
}
@keyframes caret-blink { 50% { opacity: 0; } }

.dialog-choices {
  margin-top: 0.4rem;
  max-width: min(44rem, 92vw);
}

.dialog-choice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  text-align: center;
}
.dialog-choice .dialog-choice-key {
  color: #555;
}

.dialog-choice-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.4rem;
  height: 1.4rem;
  font-size: 0.8rem;
  color: var(--color-nd-glow, #f0c040);
  font-weight: 800;
  flex-shrink: 0;
}

.dialog-choice-body {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.15rem;
  flex: 1;
  min-width: 0;
}

.dialog-choice-label {
  flex: 1;
  text-align: left;
  white-space: normal;
  overflow-wrap: anywhere;
}

.dialog-choice-hint {
  font-size: 0.8rem;
  font-style: italic;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.78);
  text-align: left;
  line-height: 1.3;
}

.dialog-choice-check {
  color: #22c55e;
  font-weight: 800;
  font-size: 1rem;
  line-height: 1;
}

.dialog-choice--done {
  border-color: rgba(34, 197, 94, 0.65) !important;
}

.dialog-choice--locked {
  opacity: 0.5;
  cursor: not-allowed;
}

.dialog-choice--locked:hover,
.dialog-choice--locked:focus-visible {
  transform: none;
  filter: none;
}

.dialog-choice--locked .dialog-choice-key {
  color: rgba(255, 255, 255, 0.7);
}

.dialog-choice--locked .dialog-choice-hint {
  color: rgba(255, 255, 255, 0.6);
}

/* Progress dots + hint sit discreetly bottom-right so they don't
   compete with the dialog block. */
.dialog-corner {
  position: absolute;
  right: 3vw;
  bottom: 3vh;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.4rem;
  pointer-events: none;
}

.dialog-progress { display: flex; gap: 0.35rem; }
.dialog-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.7);
  transition: background 0.2s;
}
.dialog-dot.active { background: var(--color-nd-glow, #f0c040); }

.dialog-hint {
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.95);
  display: flex;
  align-items: center;
  gap: 0.45rem;
  text-shadow:
    0 2px 6px rgba(0, 0, 0, 0.95),
    0 0 14px rgba(0, 0, 0, 0.7);
}
.dialog-arrow {
  font-size: 0.85rem;
  animation: arrow-pulse 1.2s ease-in-out infinite;
}
@keyframes arrow-pulse {
  0%, 100% { opacity: 0.5; transform: translateX(0); }
  50%      { opacity: 1; transform: translateX(3px); }
}
</style>
