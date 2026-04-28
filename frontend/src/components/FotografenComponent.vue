<script setup lang="ts">
import { ref, computed } from 'vue'
import GuidanceComponent from './GuidanceComponent.vue'
import type { FotografenChallenge, PhotoType } from '../types'

const props = defineProps<{ challenge: FotografenChallenge; question?: string; tip?: string }>()
const emit = defineEmits<{
  complete: []
  failed: [reason: string]
}>()

const TYPES: { value: PhotoType; label: string }[] = [
  { value: 'ekte', label: 'Ekte' },
  { value: 'ai-generert', label: 'KI-generert' },
  { value: 'manipulert', label: 'Manipulert' },
]

const selections = ref<(PhotoType | null)[]>(props.challenge.alternatives.map(() => null))
const begrunnelser = ref<(string | null)[]>(props.challenge.alternatives.map(() => null))
const submitted = ref(false)

const allSelected = computed(() => selections.value.every((s) => s !== null))

function select(index: number, type: PhotoType) {
  if (submitted.value) return
  selections.value[index] = type
}

function submit() {
  if (!allSelected.value) return
  submitted.value = true

  const wrongIndexes = props.challenge.alternatives
    .map((alt, i) => (selections.value[i] !== alt.correctType ? i + 1 : null))
    .filter((i): i is number => i !== null)

  if (wrongIndexes.length === 0) {
    emit('complete')
  } else {
    const which = wrongIndexes.length === 1
      ? `Bilde ${wrongIndexes[0]} ble klassifisert feil.`
      : `Bildene ${wrongIndexes.join(', ')} ble klassifisert feil.`
    emit('failed', `${which} Se nøye etter detaljer som uskarpe kanter, unaturlig lys og urealistiske mønstre som avslører manipulasjon eller KI-generering.`)
  }
}
</script>

<template>
  <div class="fotografen-wrapper">
  <div class="header">
    <h2 class="title">{{ question ?? 'Er bildene ekte, KI-generert eller manipulert?' }}</h2>
    <GuidanceComponent :tip="tip" />
    <p v-if="!tip" class="subtitle">Velg riktig alternativ for hvert bilde</p>
  </div>

    <div class="cards" :style="{ '--count': challenge.alternatives.length }">
      <div
        v-for="(alt, i) in challenge.alternatives"
        :key="i"
        class="card"
      >
        <img class="card-image" :src="alt.imagePath" :alt="alt.description" />

        <div class="choices">
          <button
            v-for="type in TYPES"
            :key="type.value"
            type="button"
            class="choice-btn btn-bare"
            :class="{ 'choice-btn--selected': selections[i] === type.value }"
            :disabled="submitted"
            @click="select(i, type.value)"
          >
            <span class="checkbox" :class="{ 'checkbox--checked': selections[i] === type.value }">
              <svg v-if="selections[i] === type.value" viewBox="0 0 12 10" fill="none" aria-hidden="true">
                <path d="M1 5l3.5 3.5L11 1" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </span>
            {{ type.label }}
          </button>
        </div>

        <div class="begrunnelse">
          <label :for="`begrunnelse-${i}`" class="begrunnelse-label">Begrunnelse:</label>
          <select
            :id="`begrunnelse-${i}`"
            v-model="begrunnelser[i]"
            class="begrunnelse-select"
            :disabled="submitted"
          >
            <option :value="null" disabled>Velg en begrunnelse...</option>
            <option v-for="b in alt.begrunnelser" :key="b" :value="b">{{ b }}</option>
          </select>
        </div>

      </div>
    </div>

    <div class="lever-row">
      <button
        type="button"
        class="lever-btn btn-bare"
        :disabled="!allSelected || submitted"
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
  padding: 1.25rem 2rem 1.5rem;
  gap: 1rem;
  overflow-y: auto;
  min-height: 0;
}

.header {
  text-align: center;
  flex-shrink: 0;
}

.title {
  font-size: clamp(1.4rem, 3vw, 2.25rem);
  font-weight: 900;
  color: white;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
  margin: 0 0 0.35rem;
}

.subtitle {
  font-size: 1.1rem;
  font-weight: 700;
  color: white;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.7);
  margin: 0;
}

.tip {
  margin: 0.35rem 0 0;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.8);
  font-style: italic;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

.tip-label {
  font-weight: 700;
  font-style: normal;
  color: #FFD700;
}

.cards {
  display: grid;
  grid-template-columns: repeat(var(--count), minmax(0, 260px));
  justify-content: center;
  gap: 1.25rem;
}

.card {
  background: #fdfaf4;
  border-radius: 14px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  border: 3px solid transparent;
  transition: border-color 0.2s;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
  align-self: start;
}

.card-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  background: #e9e5da;
  flex-shrink: 0;
}

.choices {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  flex-shrink: 0;
}

.choice-btn {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  width: 100%;
  padding: 0.75rem 0.9rem;
  border: 2px solid #c8c0d4;
  border-radius: 8px;
  background: white;
  font-size: 1rem;
  font-weight: 600;
  color: #1a1a1a;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  font-family: inherit;
}

.choice-btn--selected {
  border-color: #1a1a1a;
  background: #f5f0ff;
}

.choice-btn:disabled {
  cursor: default;
  opacity: 0.7;
}

.checkbox {
  width: 20px;
  height: 20px;
  border: 2px solid #888;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.15s, border-color 0.15s;
}

.checkbox--checked {
  background: #1a1a1a;
  border-color: #1a1a1a;
  color: white;
}

.checkbox svg {
  width: 12px;
  height: 12px;
}

.begrunnelse {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.begrunnelse-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #555;
}

.begrunnelse-select {
  width: 100%;
  padding: 0.55rem 0.7rem;
  border: 2px solid #c8c0d4;
  border-radius: 8px;
  background: white;
  font-size: 0.9rem;
  font-family: inherit;
  cursor: pointer;
  color: #1a1a1a;
}

.begrunnelse-select:disabled {
  cursor: default;
  opacity: 0.7;
}

.lever-row {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: center;
  flex-shrink: 0;
  padding-top: 0.5rem;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.32) 42%, rgba(0, 0, 0, 0.55));
}

.lever-btn {
  padding: 1rem 3rem;
  background: white;
  border: 2px solid #1a1a1a;
  border-radius: 999px;
  font-size: 1.1rem;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.lever-btn:hover:not(:disabled) {
  background: #1a1a1a;
  color: white;
}

.lever-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

@media (max-width: 1100px) {
  .cards {
    grid-template-columns: repeat(auto-fit, minmax(220px, 260px));
  }
}

@media (max-width: 800px) {
  .fotografen-wrapper {
    padding: 1rem;
  }

  .cards {
    grid-template-columns: 1fr;
  }
}
</style>
