<script setup lang="ts">
import { ref } from 'vue'
import GuidanceComponent from './GuidanceComponent.vue'
import type { NewsAnswers, NewsChallenge } from '../types'

defineProps<{ challenge: NewsChallenge; question?: string; tip?: string }>()
const emit = defineEmits<{
  submit: [answers: NewsAnswers]
}>()

const selectedIndex = ref<number | null>(null)
const flashIndex = ref<number | null>(null)

const LABELS = ['A', 'B', 'C', 'D']

function choose(index: number) {
  if (selectedIndex.value !== null) return
  selectedIndex.value = index
  flashIndex.value = index
  setTimeout(() => {
    flashIndex.value = null
    emit('submit', { selectedIndex: index })
  }, 140)
}
</script>

<template>
  <div class="news-task">
    <div class="task-header">
      <h2 class="question">{{ question ?? 'Hvilken artikkel er ekte?' }}</h2>
      <GuidanceComponent :tip="tip" />
    </div>

    <div class="articles">
      <button
        v-for="(article, i) in challenge.articles"
        :key="i"
        type="button"
        class="article-tile btn-bare"
        :class="{
          'is-selected': selectedIndex === i,
          'is-dimmed': selectedIndex !== null && selectedIndex !== i,
          'is-flashing': flashIndex === i,
        }"
        :style="{ '--ring-color': i === 0 ? 'var(--color-nd-glow-cool)' : 'var(--color-nd-accent)' }"
        :disabled="selectedIndex !== null"
        :aria-label="`Velg artikkel ${LABELS[i] ?? i + 1}`"
        @click="choose(i)"
      >
        <span class="tile-badge">{{ LABELS[i] ?? i + 1 }}</span>
        <img class="article-image" :src="article.imagePath" alt="Nyhetsartikkel" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.news-task {
  display: flex;
  flex-direction: column;
  flex: 1;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.25rem 1.5rem 3rem;
  overflow: hidden;
  min-height: 0;
}

.task-header {
  flex-shrink: 0;
  text-align: center;
  padding-top: 0.25rem;
  margin-bottom: 0.75rem;
}

.question {
  color: white;
  font-size: clamp(2rem, 4.5vw, 3.25rem);
  font-weight: 900;
  letter-spacing: 0.01em;
  margin: 0 0 0.85rem;
  text-shadow: 0 3px 10px rgba(0, 0, 0, 0.85);
}

.articles {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 640px));
  justify-content: center;
  align-items: start;
  gap: 2.5rem;
  padding: 0 1%;
  flex: 1;
  min-height: 0;
}

.article-tile {
  position: relative;
  display: block;
  width: 100%;
  padding: 0;
  background: transparent;
  border: none;
  overflow: visible;
  cursor: pointer;
  transition: transform 0.18s ease, opacity 0.2s ease, filter 0.12s ease;
}

.article-tile:hover:not(:disabled),
.article-tile:focus-visible {
  transform: scale(1.04);
}

.article-tile:hover:not(:disabled) .article-image,
.article-tile:focus-visible .article-image {
  box-shadow:
    0 0 0 3px var(--ring-color),
    0 0 28px color-mix(in srgb, var(--ring-color) 65%, transparent),
    0 8px 24px rgba(0, 0, 0, 0.55);
}

.article-tile.is-selected .article-image {
  box-shadow:
    0 0 0 4px var(--ring-color),
    0 0 36px color-mix(in srgb, var(--ring-color) 80%, transparent),
    0 8px 24px rgba(0, 0, 0, 0.55);
}

.article-tile.is-flashing .article-image {
  filter: brightness(1.2);
}

.article-tile.is-dimmed {
  opacity: 0.35;
  transform: scale(0.98);
}

.article-tile:disabled {
  cursor: default;
}

.article-image {
  width: 100%;
  max-height: min(46vh, 560px);
  object-fit: contain;
  border-radius: 14px;
  background: #1c1c24;
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.6);
  transition: box-shadow 0.18s ease;
  display: block;
}

.tile-badge {
  position: absolute;
  top: -14px;
  left: -14px;
  width: 56px;
  height: 56px;
  border-radius: 999px;
  background: var(--ring-color);
  color: #ffffff;
  font-size: 1.6rem;
  font-weight: 900;
  font-family: 'Inter', system-ui, sans-serif;
  display: flex;
  align-items: center;
  justify-content: center;
  -webkit-text-stroke: 3px #000000;
  paint-order: stroke fill;
  box-shadow:
    0 0 0 3px #000000,
    0 6px 16px rgba(0, 0, 0, 0.5);
  z-index: 2;
  pointer-events: none;
  letter-spacing: 0.02em;
}

@media (max-width: 1100px) {
  .news-task {
    padding-inline: 1rem;
  }

  .articles {
    padding: 0;
    gap: 1.25rem;
  }
}

@media (max-width: 800px) {
  .news-task {
    justify-content: flex-start;
    overflow-y: auto;
  }

  .articles {
    grid-template-columns: 1fr;
    flex: none;
  }

  .tile-badge {
    width: 48px;
    height: 48px;
    font-size: 1.4rem;
    top: -10px;
    left: -10px;
  }
}
</style>
