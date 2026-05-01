<script setup lang="ts">
import { ref, computed } from 'vue'
import GuidanceComponent from './GuidanceComponent.vue'
import type { FotografenChallenge, PhotoAnswers, PhotoType } from '../types'

const props = defineProps<{ challenge: FotografenChallenge; question?: string; tip?: string }>()
const emit = defineEmits<{
  submit: [answers: PhotoAnswers]
}>()

const TYPES: { value: PhotoType; label: string; tone: 'success' | 'accent' | 'danger' }[] = [
  { value: 'ekte', label: 'Ekte', tone: 'success' },
  { value: 'ai-generert', label: 'KI-generert', tone: 'accent' },
  { value: 'manipulert', label: 'Manipulert', tone: 'danger' },
]

const selections = ref<(PhotoType | null)[]>(props.challenge.alternatives.map(() => null))
const submitted = ref(false)

const allAnswered = computed(() => selections.value.every((s) => s !== null))

function select(index: number, type: PhotoType) {
  if (submitted.value) return
  selections.value[index] = type
}

function submit() {
  if (!allAnswered.value) return
  submitted.value = true

  emit('submit', {
    selections: props.challenge.alternatives.map((_, i) => ({
      type: selections.value[i]!,
    })),
  })
}
</script>

<template>
  <div class="fotografen-wrapper">
    <div class="header">
      <h2 class="title">{{ question ?? 'Er bildene ekte, KI-generert eller manipulert?' }}</h2>
      <GuidanceComponent :tip="tip" />
      <p v-if="!tip" class="subtitle">Velg riktig alternativ for hvert bilde</p>
    </div>

    <div class="photos" :style="{ '--count': challenge.alternatives.length }">
      <div
        v-for="(alt, i) in challenge.alternatives"
        :key="i"
        class="photo-block"
      >
        <div class="photo-frame">
          <img class="photo-image" :src="alt.imagePath" alt="" />
        </div>

        <div class="chips" role="radiogroup" :aria-label="`Klassifiser bilde ${i + 1}`">
          <button
            v-for="type in TYPES"
            :key="type.value"
            type="button"
            class="chip"
            :class="[`chip--${type.tone}`, { 'chip--selected': selections[i] === type.value }]"
            :disabled="submitted"
            role="radio"
            :aria-checked="selections[i] === type.value"
            @click="select(i, type.value)"
          >
            {{ type.label }}
          </button>
        </div>
      </div>
    </div>

    <div class="lever-row">
      <button
        type="button"
        class="lever-btn"
        :disabled="!allAnswered || submitted"
        @click="submit"
      >
        Lever svar &rarr;
      </button>
    </div>
  </div>
</template>

<style scoped>
.fotografen-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 0.25rem 1rem 1rem;
  gap: 0.9rem;
  overflow: hidden;
  min-height: 0;
}

.header {
  text-align: center;
  flex-shrink: 0;
  padding-top: 0.25rem;
  margin-bottom: 0.75rem;
}

.title {
  font-size: clamp(2rem, 4.5vw, 3.25rem);
  font-weight: 900;
  letter-spacing: 0.01em;
  color: white;
  text-shadow: 0 3px 10px rgba(0, 0, 0, 0.85);
  margin: 0 0 0.85rem;
}

.subtitle {
  font-size: 1.1rem;
  font-weight: 700;
  color: white;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.7);
  margin: 0;
}

.photos {
  display: grid;
  grid-template-columns: repeat(var(--count), minmax(0, 1fr));
  justify-content: center;
  gap: 1.75rem;
  width: 100%;
  max-width: min(1800px, 100%);
  margin: 0 auto;
  flex: 1;
  align-items: start;
  min-height: 0;
}

.photo-block {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  align-self: start;
  min-width: 0;
}

.photo-frame {
  width: 100%;
  aspect-ratio: 4 / 3;
  max-height: min(40vh, 430px);
  border-radius: 12px;
  background: #0a0a14;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.55);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.chips {
  display: flex;
  gap: 0.5rem;
  justify-content: center;
  flex-wrap: wrap;
}

.chip {
  --chip-color: var(--color-nd-success);
  flex: 1 1 0;
  min-width: 0;
  padding: 0.55rem 1rem;
  font-size: 0.9rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.chip--success { --chip-color: var(--color-nd-success); }
.chip--accent  { --chip-color: var(--color-nd-accent); }
.chip--danger  { --chip-color: var(--color-nd-danger); }

.chip:not(.chip--selected)::after {
  background: #1a1a1a;
}

.chip:not(.chip--selected) {
  color: #ffffff;
}

.chip--selected::before {
  background: color-mix(in srgb, var(--chip-color) 60%, #000);
}

.chip--selected::after {
  background: var(--chip-color);
}

.chip--selected {
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45);
}

.lever-row {
  display: flex;
  justify-content: center;
  flex-shrink: 0;
  padding-top: 0.25rem;
}

.lever-btn {
  font-size: 1.1rem;
  padding: 0.95rem 2.5rem;
}

@media (max-width: 1100px) {
  .photos {
    grid-template-columns: 1fr;
    max-width: min(38rem, 100%);
  }
}

@media (max-width: 800px) {
  .fotografen-wrapper {
    padding: 1rem;
    overflow-y: auto;
    justify-content: flex-start;
  }

  .photos {
    grid-template-columns: 1fr;
    flex: none;
  }

  .chip {
    font-size: 0.9rem;
    padding: 0.55rem 0.6rem;
  }

  .lever-row {
    padding-top: 0.75rem;
    margin-top: 0;
  }

  .lever-btn {
    width: 100%;
    max-width: 22rem;
  }
}
</style>
