<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  title: string
  description: string
  icon?: string
  unlocked?: boolean
  earnedAt?: string
}>()

const earnedDate = computed(() => {
  if (!props.earnedAt) return ''
  const d = new Date(props.earnedAt)
  if (Number.isNaN(d.getTime())) return ''
  return d.toLocaleDateString('nb-NO', { day: '2-digit', month: 'short', year: 'numeric' })
})
</script>

<template>
  <li
    class="medal"
    :class="{ 'medal--locked': !unlocked }"
    :aria-label="`${unlocked ? 'Oppnådd' : 'Låst'}: ${title}. ${description}`"
  >
    <span class="medal-rail" aria-hidden="true" />

    <span class="medal-icon" aria-hidden="true">
      <span v-if="unlocked" class="medal-glyph">{{ icon || '🏅' }}</span>
      <span v-else class="medal-glyph medal-glyph--locked">
        <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="11" width="16" height="10" rx="2" />
          <path d="M8 11V8a4 4 0 0 1 8 0v3" />
        </svg>
      </span>
    </span>

    <div class="medal-body">
      <h3 class="medal-title">{{ title }}</h3>
      <p class="medal-desc">{{ description }}</p>
    </div>

    <div class="medal-meta">
      <span v-if="unlocked" class="medal-tag medal-tag--earned">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M5 13l4 4L19 7" />
        </svg>
        Oppnådd
      </span>
      <span v-else class="medal-tag medal-tag--locked">Låst</span>
      <span v-if="unlocked && earnedDate" class="medal-date">{{ earnedDate }}</span>
    </div>
  </li>
</template>

<style scoped>
.medal {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 0.85rem;
  padding: 0.6rem 1rem 0.6rem 1.25rem;
  list-style: none;
  background: #fdfaf4;
  border: 1px solid #e8e0d0;
  border-radius: 12px;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
}

.medal:hover {
  transform: translateX(4px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
  border-color: #d8cfb8;
}

.medal--locked {
  background: #f1eee6;
  border-color: #d6cfbf;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.medal-rail {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 5px;
  background: linear-gradient(180deg, #f0c040, #c98c10);
}

.medal--locked .medal-rail {
  background: linear-gradient(180deg, #b8b0a0, #8a8474);
}

.medal-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: radial-gradient(circle at 30% 30%, #fff7d6, #f0c040 70%, #c98c10 100%);
  border: 2px solid #1a1a1a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.25), inset 0 1px 2px rgba(255, 255, 255, 0.4);
}

.medal--locked .medal-icon {
  background: radial-gradient(circle at 30% 30%, #e8e3d2, #b8b0a0 70%, #8a8474 100%);
  border-color: #5a5a5a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.medal-glyph {
  font-size: 1.35rem;
  line-height: 1;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.25));
}

.medal-glyph--locked {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4a4a4a;
}

.medal-body {
  min-width: 0;
}

.medal-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: #1a1a1a;
  -webkit-text-stroke: 0;
  paint-order: normal;
  text-shadow: none;
  text-transform: uppercase;
}

.medal-desc {
  margin: 0.2rem 0 0;
  font-size: 0.92rem;
  font-weight: 500;
  color: #4a4a4a;
  line-height: 1.4;
}

.medal--locked .medal-title {
  color: #5a5a5a;
}

.medal--locked .medal-desc {
  color: #6a6a6a;
  font-style: italic;
}

.medal-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.35rem;
  flex-shrink: 0;
}

.medal-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.medal-tag--earned {
  background: #1a1a1a;
  color: #f0c040;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.25);
}

.medal-tag--locked {
  background: #e2dccd;
  color: #5a5a5a;
  border: 1px solid #c8c0a8;
}

.medal-date {
  font-size: 0.78rem;
  font-weight: 600;
  color: #5a5a5a;
  letter-spacing: 0.02em;
}

@media (max-width: 640px) {
  .medal {
    grid-template-columns: auto 1fr;
    grid-template-areas:
      'icon body'
      'icon meta';
    padding: 0.85rem 1rem 0.85rem 1.3rem;
    gap: 0.75rem;
  }

  .medal-icon { grid-area: icon; width: 50px; height: 50px; }
  .medal-glyph { font-size: 1.55rem; }
  .medal-body { grid-area: body; }
  .medal-meta { grid-area: meta; flex-direction: row; align-items: center; }
  .medal-title { font-size: 1rem; }
}
</style>
