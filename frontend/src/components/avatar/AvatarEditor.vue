<script setup lang="ts">
import type { AvatarConfig } from '@/types/avatar'
import {
  SKIN_COLOR_OPTIONS,
  HAIR_OPTIONS,
  HAIR_COLOR_OPTIONS,
  HAT_OPTIONS,
  CLOTHING_OPTIONS,
  PROP_OPTIONS,
  DEFAULT_AVATAR_CONFIG,
} from '@/types/avatar'
import AvatarPreview from './AvatarPreview.vue'

const props = defineProps<{ modelValue: AvatarConfig }>()
const emit = defineEmits<{ 'update:modelValue': [value: AvatarConfig] }>()

function update(patch: Partial<AvatarConfig>) {
  emit('update:modelValue', { ...props.modelValue, ...patch })
}

const LABELS: Record<string, string> = {
  //skin
  edb98a: 'Lys',
  d08b5b: 'Medium',
  '8d5524': 'Mørk',
  '614335': 'Brun',
  // hair color
  '2c1b18': 'Svart',
  '724133': 'Brun',
  b58143: 'Blond',
  c93305: 'Rød',
  // hair
  frizzle: 'Kort Rett',
  straight01: 'Langt Rett',
  bigHair: 'Langt Krøllete',
  dreads01: 'Kort Krøllete',
  // hat
  none: 'Ingen',
  hat: 'Hatt',
  hijab: 'Hijab',
  winterHat03: 'Lue',
  // clothes
  hoodie: 'Hettegenser',
  shirtCrewNeck: 'T-skjorte',
  blazerAndShirt: 'Blazer',
  collarAndSweater: 'Genser',
  // other
  round: 'Briller',
  sunglasses: 'Solbriller',
  eyepatch: 'Øyelapp',
}

const SKIN_HEX: Record<string, string> = {
  edb98a: '#edb98a',
  d08b5b: '#d08b5b',
  '8d5524': '#8d5524',
  '614335': '#614335',
}

const HAIR_HEX: Record<string, string> = {
  '2c1b18': '#2c1b18',
  '724133': '#724133',
  b58143: '#b58143',
  c93305: '#c93305',
}

interface SwatchCategory {
  key: keyof AvatarConfig
  label: string
  options: readonly string[]
  colors: Record<string, string>
}

interface TileCategory {
  key: keyof AvatarConfig
  label: string
  options: readonly string[]
}

const swatchCategories: SwatchCategory[] = [
  { key: 'skinColor', label: 'Hudfarge', options: SKIN_COLOR_OPTIONS, colors: SKIN_HEX },
  { key: 'hairColor', label: 'Hårfarge', options: HAIR_COLOR_OPTIONS, colors: HAIR_HEX },
]

const tileCategories: TileCategory[] = [
  { key: 'hair', label: 'Hår', options: HAIR_OPTIONS },
  { key: 'hat', label: 'Hatt', options: HAT_OPTIONS },
  { key: 'clothing', label: 'Klær', options: CLOTHING_OPTIONS },
  { key: 'prop', label: 'Tilbehør', options: PROP_OPTIONS },
]


const TILT = [-3, 2.5, -1.5, 3] as const

function tiltStyle(idx: number) {
  return { transform: `rotate(${TILT[idx % TILT.length]}deg)` }
}

function previewConfig(cat: keyof AvatarConfig, id: string): AvatarConfig {
  return { ...DEFAULT_AVATAR_CONFIG, [cat]: id }
}

function handleKeydown(e: KeyboardEvent, options: readonly string[], key: keyof AvatarConfig) {
  const currentIndex = options.indexOf(props.modelValue[key] as string)
  let nextIndex = currentIndex
  if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
    e.preventDefault()
    nextIndex = (currentIndex + 1) % options.length
  } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
    e.preventDefault()
    nextIndex = (currentIndex - 1 + options.length) % options.length
  } else {
    return
  }
  update({ [key]: options[nextIndex] })
  const group = e.currentTarget as HTMLElement
  const buttons = group.querySelectorAll<HTMLButtonElement>('[role="radio"]')
  buttons[nextIndex]?.focus()
}
</script>

<template>
  <div class="avatar-editor flex flex-col gap-0 w-full lg:grid lg:grid-rows-[auto_1fr_1fr_1fr_1fr] lg:gap-0 lg:h-full lg:min-h-0 lg:overflow-hidden">
    <div class="flex justify-between gap-4 pb-2">
      <div v-for="sc in swatchCategories" :key="sc.key" class="flex-1">
        <p class="section-label mb-1">{{ sc.label }}</p>
        <div
          role="radiogroup"
          :aria-label="sc.label"
          class="grid grid-cols-4 gap-3"
          @keydown="handleKeydown($event, sc.options, sc.key)"
        >
          <button
            v-for="(opt, idx) in sc.options"
            :key="opt"
            role="radio"
            :aria-checked="modelValue[sc.key] === opt"
            :aria-label="LABELS[opt] ?? opt"
            :tabindex="modelValue[sc.key] === opt ? 0 : -1"
            class="paper-tile swatch-tile relative flex flex-col items-center gap-1 p-1.5"
            :class="{ 'is-pinned': modelValue[sc.key] === opt }"
            :style="modelValue[sc.key] === opt ? tiltStyle(idx) : undefined"
            @click="update({ [sc.key]: opt })"
          >
            <span class="tile-label">{{ LABELS[opt] ?? opt }}</span>
            <span
              class="block w-6 h-6 rounded-full border border-stone-300"
              :style="{ backgroundColor: sc.colors[opt] }"
            />
            <svg
              v-if="modelValue[sc.key] === opt"
              class="pin-icon"
              aria-hidden="true"
              viewBox="0 0 24 32"
              fill="none"
            >
              <ellipse cx="12" cy="8" rx="9" ry="6" fill="#e04040" stroke="#b91c1c" stroke-width="0.8" />
              <ellipse cx="9" cy="6" rx="3.5" ry="2" fill="#ff7070" opacity="0.5" />
              <line x1="12" y1="14" x2="12" y2="30" stroke="#a8a29e" stroke-width="2" stroke-linecap="round" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div v-for="cat in tileCategories" :key="cat.key" class="category-section lg:flex lg:flex-col lg:min-h-0 lg:overflow-hidden pt-2 pb-1">
      <p class="section-label mb-0.5">{{ cat.label }}</p>
      <div
        role="radiogroup"
        :aria-label="cat.label"
        class="grid grid-cols-4 gap-4 lg:flex-1 lg:min-h-0"
        @keydown="handleKeydown($event, cat.options, cat.key)"
      >
        <button
          v-for="(opt, idx) in cat.options"
          :key="opt"
          role="radio"
          :aria-checked="modelValue[cat.key] === opt"
          :aria-label="LABELS[opt] ?? opt"
          :tabindex="modelValue[cat.key] === opt ? 0 : -1"
          class="paper-tile avatar-tile relative flex flex-col items-center overflow-visible"
          :class="{ 'is-pinned': modelValue[cat.key] === opt }"
          :style="modelValue[cat.key] === opt ? tiltStyle(idx) : undefined"
          @click="update({ [cat.key]: opt })"
        >
          <span class="tile-label pt-0.5">{{ LABELS[opt] ?? opt }}</span>
          <AvatarPreview :config="previewConfig(cat.key, opt)" :size="60" />
          <svg
            v-if="modelValue[cat.key] === opt"
            class="pin-icon"
            aria-hidden="true"
            viewBox="0 0 24 32"
            fill="none"
          >
            <ellipse cx="12" cy="8" rx="9" ry="6" fill="#e04040" stroke="#b91c1c" stroke-width="0.8" />
            <ellipse cx="9" cy="6" rx="3.5" ry="2" fill="#ff7070" opacity="0.5" />
            <line x1="12" y1="14" x2="12" y2="30" stroke="#a8a29e" stroke-width="2" stroke-linecap="round" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Patrick+Hand&display=swap');

.avatar-editor :deep(button) {
  font-size: 0;
  line-height: 1;
  background-color: #ffffff;
  border: none;
  border-radius: 0;
  padding: 0;
}

.paper-tile {
  background: #ffffff !important;
  border-radius: 0 !important;
  box-shadow: 1px 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.25s ease, box-shadow 0.2s ease;
  cursor: pointer;
}

.paper-tile:hover {
  transform: translateY(-3px) !important;
  box-shadow: 2px 4px 8px rgba(0, 0, 0, 0.15);
  z-index: 4;
}

.paper-tile.is-pinned {
  box-shadow: 3px 4px 8px rgba(0, 0, 0, 0.18);
  z-index: 3;
}

.category-section {
  border-top: 1px solid rgba(68, 64, 60, 0.2);
}

.section-label {
  font-family: 'Patrick Hand', cursive;
  font-size: 1.2rem;
  font-weight: 500;
  color: #292524;
}

.tile-label {
  font-family: 'Patrick Hand', cursive;
  font-size: 0.85rem;
  font-weight: 500;
  color: #44403c;
  line-height: 1.1;
}

.swatch-tile {
  font-size: 0.75rem;
  padding: 0.375rem;
}

.avatar-tile {
  font-size: 0.75rem;
}

.pin-icon {
  position: absolute;
  top: -0.5rem;
  right: -0.3rem;
  width: 1.2rem;
  height: 1.6rem;
  z-index: 5;
  pointer-events: none;
  filter: drop-shadow(0 2px 2px rgba(0, 0, 0, 0.3));
}
</style>
