<script setup lang="ts">
import { ref } from 'vue'
import type { NewsChallenge, NewsArticle } from '../types'

const props = defineProps<{ challenge: NewsChallenge }>()
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
  <h2 class="question">Hvilken artikkel er ekte?</h2>

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
</template>

<style scoped>
.question {
  flex-shrink: 0;
  text-align: center;
  color: white;
  font-size: 2.25rem;
  font-weight: 800;
  margin: 0 0 1.5rem 0;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.8);
}

.articles {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  padding: 0 5%;
  min-height: 0;
}

.article-card {
  display: flex;
  flex-direction: column;
  background: #fdfaf4;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  gap: 1rem;
  border: 3px solid transparent;
  transition: border-color 0.2s ease, opacity 0.2s ease;
}

.article-card.is-selected {
  border-color: #22c55e;
}

.article-image {
  width: 100%;
  flex: 1;
  min-height: 0;
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

</style>
