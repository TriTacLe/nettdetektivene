<script setup lang="ts">
import { ref } from 'vue'
import GuidanceComponent from './GuidanceComponent.vue'
import type { NewsChallenge, NewsArticle } from '../types'

const props = defineProps<{ challenge: NewsChallenge; question?: string; tip?: string }>()
const emit = defineEmits<{
  complete: []
  failed: [reason: string]
}>()

const selected = ref<NewsArticle | null>(null)

function choose(article: NewsArticle) {
  if (selected.value !== null) return
  selected.value = article
  if (article.isReal) {
    emit('complete')
  } else {
    emit('failed', 'Du valgte den falske nyheten. Sjekk alltid kilden, se etter troverdige forfattere og vær kritisk til overdrevne overskrifter.')
  }
}
</script>

<template>
  <div class="news-task">
    <div class="task-header">
      <h2 class="question">{{ question ?? 'Hvilken artikkel er ekte?' }}</h2>
      <GuidanceComponent :tip="tip" />
    </div>

    <div class="articles">
      <article
        v-for="(article, i) in props.challenge.articles"
        :key="i"
        class="article-card"
        :class="{ 'is-selected': selected === article }"
      >
        <img class="article-image" :src="article.imagePath" alt="Nyhetsartikkel" />

        <button
          type="button"
          class="article-choice"
          :disabled="selected !== null"
          @click="choose(article)"
        >
          Dette er den ekte nyheten
        </button>
      </article>
    </div>
  </div>
</template>

<style scoped>
.news-task {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem 1.5rem 1.5rem;
  overflow-y: auto;
  min-height: 0;
}

.task-header {
  flex-shrink: 0;
  text-align: center;
}

.question {
  color: white;
  font-size: clamp(1.5rem, 3vw, 2.25rem);
  font-weight: 800;
  margin: 0;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.8);
}

.tip {
  margin: 0.4rem 0 0;
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

.articles {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
  padding: 0 1%;
  align-items: stretch;
}

.article-card {
  display: flex;
  flex-direction: column;
  background: #fdfaf4;
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  gap: 1rem;
  border: 3px solid transparent;
  transition: border-color 0.2s ease, opacity 0.2s ease;
  min-height: 0;
}

.article-card.is-selected {
  border-color: #22c55e;
}

.article-image {
  width: 100%;
  max-height: min(52vh, 560px);
  object-fit: contain;
  border-radius: 6px;
  background: #e9e5da;
}

.article-choice {
  display: block;
  width: 100%;
  padding: 0.85rem 1rem;
  border: 2px solid #1a1a1a;
  border-radius: 8px;
  cursor: pointer;
  margin-top: auto;
  background: white;
  font-weight: 600;
  color: #1a1a1a;
  font-family: system-ui, sans-serif;
  font-size: 1rem;
  text-align: center;
  transition: background 0.15s ease, transform 0.1s ease;
}

.article-choice:hover:not(:disabled) {
  background: #1a1a1a;
  color: white;
}

.article-choice:active:not(:disabled) {
  transform: scale(0.98);
}

.article-choice:disabled {
  cursor: default;
  opacity: 0.6;
}

@media (max-width: 1100px) {
  .news-task {
    padding-inline: 1rem;
  }

  .articles {
    padding: 0;
  }
}

@media (max-width: 800px) {
  .articles {
    grid-template-columns: 1fr;
  }
}

</style>
