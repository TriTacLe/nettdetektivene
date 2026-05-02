<script setup lang="ts">
import { getAllClassroomsOptions, getClassroomsLeaderboardByInstitutionOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'

const router = useRouter()

const { data: classroomsData, isLoading: classroomsLoading } = useQuery(getAllClassroomsOptions())

const institutionId = computed(() => classroomsData.value?.data?.[0]?.institutionId ?? null)

const { data, isLoading: leaderboardLoading, isError } = useQuery(
  computed(() => {
    if (!institutionId.value) {
      return {
        queryKey: ['-'] as const,
        queryFn: () => Promise.resolve({ data: [] }),
        enabled: false,
        staleTime: Infinity,
        refetchOnWindowFocus: false
      }
    }
    const baseOpts = getClassroomsLeaderboardByInstitutionOptions({ path: { institutionId: institutionId.value } })
    return {
      ...baseOpts,
      enabled: true,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any
  })
)

const isLoading = computed(() => classroomsLoading.value || leaderboardLoading.value)

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const entries = computed(() => (data.value as { data?: Array<{ classroomId: string; rank: number; title: string; totalScore: number }> } | null)?.data ?? [])

const listRef = ref<HTMLElement | null>(null)
const hasMoreBelow = ref(false)
const focusedIndex = ref(-1)

function checkScroll() {
  const el = listRef.value
  if (!el) return
  hasMoreBelow.value = el.scrollTop + el.clientHeight < el.scrollHeight - 4
}

function getRows(): HTMLElement[] {
  return Array.from(listRef.value?.querySelectorAll<HTMLElement>('.lb-row') ?? [])
}

function focusRow(index: number) {
  const rows = getRows()
  if (!rows.length) return
  focusedIndex.value = Math.max(0, Math.min(index, rows.length - 1))
  const row = rows[focusedIndex.value]
  row.focus()
  row.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowDown') { e.preventDefault(); focusRow(focusedIndex.value + 1) }
  if (e.key === 'ArrowUp')   { e.preventDefault(); focusRow(focusedIndex.value - 1) }
}

onMounted(() => {
  checkScroll()
  listRef.value?.addEventListener('scroll', checkScroll)
  focusRow(0)
})

onUnmounted(() => {
  listRef.value?.removeEventListener('scroll', checkScroll)
})
</script>

<template>
  <SceneComponent title="Ledertavle" previous="/teacher-home" background="/StartBackground.jpg" dimOverlay>
    <div class="lb-container">
      <div class="lb-card">

        <!-- Tittel -->
        <div class="lb-header">
          <h2 class="lb-title">Klasserom ledertavle</h2>
        </div>

        <!-- Loading -->
        <div v-if="isLoading" class="lb-state" aria-live="polite">
          Laster...
        </div>

        <!-- Error -->
        <div v-else-if="isError" class="lb-state lb-state--error" role="alert">
          Kunne ikke hente ledertavlen.
        </div>

        <!-- Liste -->
        <template v-else>
          <div class="lb-header-row" aria-hidden="true">
            <span class="lb-col-rank">Plass</span>
            <span class="lb-col-name">Klasse</span>
            <span class="lb-col-score">Poeng</span>
            <span class="lb-col-cta"></span>
          </div>

          <div class="lb-list-wrap">
            <ol
              ref="listRef"
              class="lb-list"
              aria-label="Klasseromrangering"
              @keydown="handleKeydown"
            >
              <li
                v-for="entry in entries"
                :key="entry.classroomId"
                class="lb-row"
                role="button"
                tabindex="-1"
                :aria-label="`${entry.rank}. plass: ${entry.title}, ${entry.totalScore} poeng. Trykk for detaljer.`"
                @click="focusedIndex = entries.indexOf(entry); router.push({ name: 'teacher-classroom-leaderboard', params: { classroomId: entry.classroomId } })"
                @keydown.enter="router.push({ name: 'teacher-classroom-leaderboard', params: { classroomId: entry.classroomId } })"
                @keydown.space.prevent="router.push({ name: 'teacher-classroom-leaderboard', params: { classroomId: entry.classroomId } })"
              >
                <span class="lb-rank">{{ entry.rank }}</span>
                <span class="lb-name">{{ entry.title }}</span>
                <span class="lb-score">
                  {{ entry.totalScore.toLocaleString('nb-NO') }}
                </span>
                <span class="lb-cta" aria-hidden="true">Se elever →</span>
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
.lb-col-score { width: 6rem; text-align: left; flex-shrink: 0; }
.lb-col-cta   { width: 6.5rem; flex-shrink: 0; }

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
  cursor: pointer;
  transition: background 0.12s ease, box-shadow 0.12s ease;
}

.lb-row:hover,
.lb-row:focus-visible {
  background: #f0f4ff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  outline: none;
}

.lb-row:focus-visible {
  outline: 2px solid #1d4e8f;
  outline-offset: 2px;
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
  width: 6rem;
  justify-content: flex-start;
}

.lb-cta {
  flex-shrink: 0;
  width: 5.75rem;
  margin-left: 0.75rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: #1d4e8f;
  background: #e8f0fb;
  border: 1px solid #a8c0e8;
  border-radius: 6px;
  padding: 0.25rem 0.6rem;
  white-space: nowrap;
  text-align: center;
  transition: background 0.12s ease, color 0.12s ease;
}

.lb-row:hover .lb-cta,
.lb-row:focus-visible .lb-cta {
  background: #1d4e8f;
  color: #ffffff;
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
  .lb-score { font-size: 0.85rem; width: 5rem; }
  .lb-col-score { width: 5rem; }
  .lb-col-cta { width: 5.5rem; }
  .lb-cta { width: 4.75rem; font-size: 0.72rem; }
}
</style>
