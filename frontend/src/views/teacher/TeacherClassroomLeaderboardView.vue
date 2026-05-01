<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import {
  getClassroomLeaderboardOptions,
  getClassroomOptions,
} from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'

const route = useRoute()
const classroomId = route.params.classroomId as string

const { data: classroomData } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroomTitle = computed(() => classroomData.value?.data?.title ?? 'Klasserom')

const { data, isLoading: queryLoading, isError: queryError } = useQuery(
  getClassroomLeaderboardOptions({ path: { classroomId } }),
)

const isLoading = computed(() => queryLoading.value)
const isError = computed(() => queryError.value)
const entries = computed(() => data.value?.data ?? [])

const listRef = ref<HTMLElement | null>(null)
const hasMoreBelow = ref(false)

function checkScroll() {
  const el = listRef.value
  if (!el) return
  hasMoreBelow.value = el.scrollTop + el.clientHeight < el.scrollHeight - 4
}

function handleKeydown(e: KeyboardEvent) {
  const el = listRef.value
  if (!el) return
  if (e.key === 'ArrowDown') { e.preventDefault(); el.scrollBy({ top: 60, behavior: 'smooth' }) }
  if (e.key === 'ArrowUp')   { e.preventDefault(); el.scrollBy({ top: -60, behavior: 'smooth' }) }
}

onMounted(() => {
  checkScroll()
  listRef.value?.addEventListener('scroll', checkScroll)
  listRef.value?.focus()
})

onUnmounted(() => {
  listRef.value?.removeEventListener('scroll', checkScroll)
})
</script>

<template>
  <SceneComponent
    :title="classroomTitle"
    background="/StartBackground.jpg"
    previous="/teacher-leaderboard"
  >
    <div class="lb-container">
      <div class="lb-card">

        <!-- Tittel -->
        <div class="lb-header">
          <h2 class="lb-title">Elev ledertavle</h2>
        </div>

        <!-- Loading -->
        <div v-if="isLoading" class="lb-state" aria-live="polite">Laster...</div>

        <!-- Error -->
        <div v-else-if="isError" class="lb-state lb-state--error" role="alert">
          Kunne ikke hente ledertavlen.
        </div>

        <!-- Liste -->
        <template v-else>
          <div class="lb-header-row" aria-hidden="true">
            <span class="lb-col-rank">Plass</span>
            <span class="lb-col-name">Elev</span>
            <span class="lb-col-score">Poeng</span>
          </div>

          <div class="lb-list-wrap">
            <ol
              ref="listRef"
              class="lb-list"
              :aria-label="`Elevrangering for ${classroomTitle}`"
              tabindex="0"
              @keydown="handleKeydown"
            >
              <li
                v-for="entry in entries"
                :key="entry.userId"
                class="lb-row"
              >
                <span class="lb-rank">{{ entry.rank }}</span>
                <span class="lb-name">{{ entry.displayName }}</span>
                <span class="lb-score">
                  {{ entry.totalScore.toLocaleString('nb-NO') }}
                </span>
              </li>

              <li v-if="entries.length === 0" class="lb-empty">
                Ingen resultater ennå.
              </li>
            </ol>
            <div v-if="hasMoreBelow" class="lb-fade" aria-hidden="true"></div>
          </div>
        </template>

      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.lb-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 2rem 1rem 1rem;
  flex: 1;
  min-height: 0;
}

.lb-card {
  width: 100%;
  max-width: 640px;
  background: #ffffff;
  border: 1.5px solid #e2e2e2;
  border-radius: 12px;
  padding: 1.5rem 1.5rem 1.75rem;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.14);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  font-family: var(--font-family-sans);
}

.lb-header {
  border-bottom: 1.5px solid #e2e2e2;
  padding-bottom: 0.75rem;
  margin-bottom: 0.25rem;
}

.lb-title {
  text-align: center;
  font-size: 1.1rem;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  -webkit-text-stroke: 0;
  paint-order: normal;
  text-shadow: none;
  margin: 0;
}

.lb-header-row {
  display: flex;
  align-items: center;
  padding: 0.3rem 0.75rem 0.15rem;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #555;
  border-bottom: 1.5px solid #e2e2e2;
}

.lb-col-rank  { width: 3.5rem; text-align: left; flex-shrink: 0; }
.lb-col-name  { flex: 1; text-align: left; padding-left: 0.5rem; }
.lb-col-score { width: 8rem; text-align: right; flex-shrink: 0; }

.lb-list-wrap {
  position: relative;
  min-height: 0;
  flex: 1;
}

.lb-fade {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(to bottom, transparent, #ffffff);
  pointer-events: none;
}

.lb-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  overflow-y: auto;
  max-height: 60vh;
  outline: none;
}

.lb-list::-webkit-scrollbar { width: 5px; }
.lb-list::-webkit-scrollbar-track { background: transparent; }
.lb-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 999px;
}

.lb-row {
  display: flex;
  align-items: center;
  padding: 0.6rem 0.75rem;
  background: #f8f8f8;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  transition: box-shadow 0.12s ease;
}

.lb-rank {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.82rem;
  font-weight: 700;
  flex-shrink: 0;
  background: #e8e8e8;
  color: #333;
  margin-right: 1.5rem;
}

.lb-name {
  flex: 1;
  font-size: 0.95rem;
  font-weight: 500;
  color: #1a1a1a;
  padding-left: 0.5rem;
}

.lb-score {
  display: flex;
  align-items: center;
  font-size: 0.95rem;
  font-weight: 600;
  color: #333;
  font-variant-numeric: tabular-nums;
  width: 8rem;
  justify-content: flex-end;
}

.lb-state {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.lb-state--error {
  color: #c0392b;
  font-weight: 600;
}

.lb-empty {
  text-align: center;
  padding: 2rem;
  color: #555;
  font-style: italic;
}

@media (max-width: 500px) {
  .lb-card {
    padding: 1.25rem 0.75rem 1rem;
  }

  .lb-name  { font-size: 0.85rem; }
  .lb-score { font-size: 0.85rem; width: 6rem; }
  .lb-col-score { width: 6rem; }
}
</style>
