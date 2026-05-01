<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { getClassroomsLeaderboardByInstitutionOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import SceneComponent from '@/components/SceneComponent.vue'
import { useStudentStore } from '@/stores/StudentStore'

const studentStore = useStudentStore()
const institutionId = computed(() => studentStore.institutionId)

const queryOptions = computed(() =>
  institutionId.value
    ? getClassroomsLeaderboardByInstitutionOptions({ path: { institutionId: institutionId.value } })
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    : { queryKey: ['leaderboard-disabled'], queryFn: () => null, enabled: false } as any,
)

const { data, isLoading: queryLoading, isError: queryError } = useQuery(queryOptions)

const isLoading = computed(() => queryLoading.value)
const isError = computed(() => queryError.value)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const entries = computed(() => (data.value as any)?.data ?? [])

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
  <SceneComponent title="Ledertavle" previous="/student-home">
    <div class="lb-container">
      <div class="lb-card">

        <!-- Ribbon-overskrift -->
        <div class="lb-ribbon-wrap" role="presentation">
          <svg class="lb-title-svg" viewBox="0 0 500 70" aria-label="Klasserom ledertavle" role="img">
            <defs>
              <path id="ribbon-arc" d="M 30,55 Q 250,18 470,55" />
            </defs>
            <text
              font-size="20"
              font-weight="900"
              font-family="Inter, ui-sans-serif, system-ui, sans-serif"
              letter-spacing="2.5"
              fill="white"
            >
              <textPath href="#ribbon-arc" startOffset="50%" text-anchor="middle">
                KLASSEROM LEDERTAVLE
              </textPath>
            </text>
          </svg>
        </div>

        <!-- Loading -->
        <div v-if="isLoading" class="lb-state" aria-live="polite">
          Laster...
        </div>

        <!-- Error -->
        <div v-else-if="isError" class="lb-state lb-state--error" role="alert">
          Kunne ikke hente ledertavlen.
        </div>

        <!-- List -->
        <template v-else>
          <!-- Kolonneoverskrifter -->
          <div class="lb-header-row" aria-hidden="true">
            <span class="lb-col-rank">Plass</span>
            <span class="lb-col-name">Klasse</span>
            <span class="lb-col-score">Poeng</span>
          </div>

          <div class="lb-list-wrap">
          <ol
            ref="listRef"
            class="lb-list"
            aria-label="Klasserom ledertavle"
            tabindex="0"
            @keydown="handleKeydown"
          >
            <li
              v-for="entry in entries"
              :key="entry.classroomId"
              class="lb-row"
              :class="{
                'lb-row--gold':   entry.rank === 1,
                'lb-row--silver': entry.rank === 2,
                'lb-row--bronze': entry.rank === 3,
              }"
            >
              <img
                v-if="entry.rank <= 3"
                :src="`/medal-${entry.rank}.png`"
                :alt="`${entry.rank}. plass`"
                class="lb-medal"
              />
              <span v-else class="lb-rank">{{ entry.rank }}</span>
              <span class="lb-name">{{ entry.title }}</span>
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
  padding-top: 2.3rem;
  flex: 1;
  min-height: 0;
}

/* Kort */
.lb-card {
  width: 100%;
  max-width: 580px;
  background: linear-gradient(170deg, #fdfaf4 0%, #f5ecce 100%);
  border-radius: 10px;
  padding: 0 1.5rem 1.75rem;
  padding-top: 0;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.28);
  border: 2px solid #c9a96e;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* Ribbon-overskrift */
.lb-ribbon-wrap {
  position: relative;
  margin: -65px -1.5rem -10px;
  height: 150px;
  background: url('/ribbon.png') no-repeat center center;
  background-size: 85% auto;
  display: flex;
  align-items: center;
  justify-content: center;
  filter: drop-shadow(0 6px 10px rgba(0, 0, 0, 0.35));
}

.lb-title-svg {
  position: absolute;
  width: 85%;
  top: 38%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.lb-title-text {
  font-size: 18px;
  font-weight: 900;
  font-family: 'Nunito', 'Segoe UI', sans-serif;
  letter-spacing: 3px;
  fill: #fff8e1;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.45));
  stroke: #8b1a0a;
  stroke-width: 0.5;
}

/* Kolonneoverskrifter */
.lb-header-row {
  display: flex;
  align-items: center;
  padding: 0.3rem 0.75rem 0.15rem;
  font-size: 0.82rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #5a4a2e;
  border-bottom: 1.5px solid #d9c89a;
}

.lb-col-rank  { width: 3.5rem; text-align: left; }
.lb-col-name  { flex: 1; text-align: left; padding-left: 0.5rem; }
.lb-col-score { width: 6rem; text-align: right; flex-shrink: 0; }

/* Liste-wrapper med fade */
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
  background: linear-gradient(to bottom, transparent, #f5ecce);
  pointer-events: none;
}

/* Liste */
.lb-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  overflow-y: auto;
  max-height: 55vh;
  outline: none;
}

.lb-list::-webkit-scrollbar { width: 5px; }
.lb-list::-webkit-scrollbar-track { background: transparent; }
.lb-list::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.15);
  border-radius: 999px;
}

/* Rad */
.lb-row {
  display: flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  border-radius: 12px;
  background: #fff8ee;
  border: 1.4px solid #e0d5c0;
  transition: box-shadow 0.12s ease;
}

.lb-row:hover {
  box-shadow: 0 2px 10px rgba(0,0,0,0.12);
}

/* Topp 3 */
.lb-row--gold {
  background: linear-gradient(90deg, #fffbe6, #fff3c0);
  border-color: #d4a017;
}

.lb-row--silver {
  background: linear-gradient(90deg, #f5f5f5, #e8e8e8);
  border-color: #999;
}

.lb-row--bronze {
  background: linear-gradient(90deg, #fdf0e6, #f5dfc8);
  border-color: #b87146;
}

/* Medalje-bilde (topp 3) */
.lb-medal {
  width: 2.4rem;
  height: 2.4rem;
  flex-shrink: 0;
  margin-right: 0.75rem;
  object-fit: contain;
  mix-blend-mode: multiply;
}

/* Rang-sirkel (øvrige) */
.lb-rank {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 800;
  flex-shrink: 0;
  background: #ede5d0;
  color: #5a4a35;
  margin-right: 0.75rem;
}

/* Navn */
.lb-name {
  flex: 1;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1a1a1a;
  padding-left: 0.7rem;
}

/* Poeng */
.lb-score {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.95rem;
  font-weight: 700;
  color: #3b2a1a;
  font-variant-numeric: tabular-nums;
  width: 6rem;
  justify-content: flex-end;
}


/* States */
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
  color: #5a4a2e;
  font-style: italic;
}

@media (max-width: 500px) {
  .lb-card {
    padding: 0 0.75rem 1rem;
    border-radius: 16px;
  }

  .lb-ribbon-wrap {
    margin: -55px -0.75rem 0;
    height: 130px;
    background-size: 92% auto;
  }

  .lb-title-svg {
    width: 90%;
  }

  .lb-row {
    padding: 0.4rem 0.5rem;
  }

  .lb-medal {
    width: 2rem;
    height: 2rem;
    margin-right: 0.5rem;
  }

  .lb-rank {
    width: 1.9rem;
    height: 1.9rem;
    font-size: 0.78rem;
    margin-right: 0.5rem;
  }

  .lb-name {
    font-size: 0.85rem;
    padding-left: 0.4rem;
  }

  .lb-score {
    font-size: 0.85rem;
    width: 4.5rem;
  }

  .lb-col-rank { width: 3rem; }
  .lb-col-score { width: 4.5rem; }
}
</style>
