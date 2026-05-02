<script setup lang="ts">
import { computed, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import SceneComponent from '@/components/SceneComponent.vue'
import { useQuery } from '@tanstack/vue-query'
import {
  getClassroomLeaderboardOptions,
  getStudentScoreOptions,
  getClassroomTasksOptions,
  getAllNotebooksOptions,
  getStudentsOptions,
  getAchievementsForStudentOptions,
  getAllAchievementsOptions,
} from '@/generated-client/@tanstack/vue-query.gen.ts'

const route = useRoute()
const classroomId = route.params.id as string
const userId = route.params.userId as string

const STOP_ORDER = ['nyhetskvartalet', 'fotografen', 'datasenteret']

const STOP_LABELS: Record<string, string> = {
  nyhetskvartalet: 'Nyhetskvartalet',
  fotografen: 'Fotografen',
  datasenteret: 'Datasenteret',
}

const activeTab = ref<'overview' | 'achievements'>('overview')
const openNotebookStop = ref<string | null>(null)

const tabs = [
  { key: 'overview' as const, label: 'Oversikt' },
  { key: 'achievements' as const, label: 'Prestasjoner' },
]

const tabRefs = ref<Record<string, HTMLButtonElement | undefined>>({})

function setTabRef(key: string, el: Element | null) {
  tabRefs.value[key] = (el as HTMLButtonElement) ?? undefined
}

function onTabKeydown(e: KeyboardEvent) {
  if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return
  e.preventDefault()
  const order = tabs.map((t) => t.key)
  const i = order.indexOf(activeTab.value)
  const next = e.key === 'ArrowRight'
    ? order[(i + 1) % order.length]!
    : order[(i - 1 + order.length) % order.length]!
  activeTab.value = next
  nextTick(() => tabRefs.value[next]?.focus())
}

const { data: leaderboardData, isLoading: loadingLeaderboard } = useQuery(
  getClassroomLeaderboardOptions({ path: { classroomId } }),
)
const { data: studentsData, isLoading: loadingStudents } = useQuery(
  getStudentsOptions({ path: { classroomId } }),
)
const { data: scoreData, isLoading: loadingScores } = useQuery(
  getStudentScoreOptions({ path: { classroomId, userId } }),
)
const { data: tasksData, isLoading: loadingTasks } = useQuery(
  getClassroomTasksOptions({ path: { classroomId }, query: { activeOnly: true } }),
)
const { data: notebookData, isLoading: loadingNotebooks } = useQuery(
  getAllNotebooksOptions({ path: { classroomId } }),
)
const { data: achievementsData, isLoading: loadingAchievements } = useQuery(
  getAchievementsForStudentOptions({ path: { userId } }),
)
const { data: allAchievementsData, isLoading: loadingAllAchievements } = useQuery(
  getAllAchievementsOptions(),
)

const isLoading = computed(
  () =>
    loadingLeaderboard.value ||
    loadingScores.value ||
    loadingTasks.value ||
    loadingNotebooks.value ||
    loadingStudents.value ||
    loadingAchievements.value ||
    loadingAllAchievements.value,
)

const studentEntry = computed(() =>
  leaderboardData.value?.data?.find((e) => e.userId === userId),
)

const studentRecord = computed(() =>
  studentsData.value?.data?.find((s) => s.userId === userId),
)

const displayName = computed(
  () => studentRecord.value?.displayName ?? studentEntry.value?.displayName ?? 'Ukjent elev',
)

const taskScores = computed(() => scoreData.value?.data ?? [])

const allTasks = computed(() => {
  const out: NonNullable<NonNullable<typeof tasksData.value>['data']>[number]['task'][] = []
  for (const ct of tasksData.value?.data ?? []) {
    if (ct.isActive && ct.task) out.push(ct.task)
  }
  return out.filter((t): t is NonNullable<typeof t> => t !== undefined)
})

const scoreMap = computed(() => {
  const map = new Map<string, (typeof taskScores.value)[0]>()
  for (const score of taskScores.value) {
    if (score.taskId) map.set(score.taskId, score)
  }
  return map
})

const studentNotebooks = computed(() =>
  (notebookData.value?.data ?? []).filter((n) => n.userId === userId),
)

const earnedAchievements = computed(() => achievementsData.value?.data ?? [])
const allAchievements = computed(() => allAchievementsData.value?.data ?? [])

const earnedIds = computed(() => new Set(earnedAchievements.value.map((a) => a.achievementId)))

const notebookByStop = computed(() => {
  const map = new Map<string, (typeof studentNotebooks.value)[0]>()
  for (const entry of studentNotebooks.value) {
    if (entry.stopLocation) map.set(entry.stopLocation, entry)
  }
  return map
})

const totalActive = computed(() => allTasks.value.length)
const totalPassed = computed(() => taskScores.value.filter((s) => s.firstPassedAt != null).length)
const completionPct = computed(() =>
  totalActive.value > 0 ? Math.round((totalPassed.value / totalActive.value) * 100) : 0,
)

function formatDate(iso: string | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('nb-NO')
}

const xpLevel = computed(() => studentEntry.value?.xpLevel ?? 1)
const xpPoints = computed(() => studentEntry.value?.xp ?? 0)
const totalScore = computed(() => studentEntry.value?.totalScore ?? 0)
const joinedAt = computed(() => formatDate(studentRecord.value?.joinedAt))

type StopStatus = 'fullfort' | 'pagar' | 'last'

const stopData = computed(() => {
  // collect unique stop locations present in active tasks
  const stopSet = new Set(allTasks.value.map((t) => t.stopLocation).filter(Boolean) as string[])

  // order by canonical list, then append any unknown stops at the end
  const orderedKeys = [
    ...STOP_ORDER.filter((k) => stopSet.has(k)),
    ...[...stopSet].filter((k) => !STOP_ORDER.includes(k)).sort(),
  ]

  return orderedKeys.map((stopKey) => {
    const stopTasks = allTasks.value.filter((t) => t.stopLocation === stopKey)
    const stopTaskIds = new Set(stopTasks.map((t) => t.id).filter(Boolean) as string[])
    const passed = taskScores.value.filter(
      (s) => s.taskId && stopTaskIds.has(s.taskId) && s.firstPassedAt != null,
    ).length
    const pct = stopTasks.length > 0 ? Math.round((passed / stopTasks.length) * 100) : 0
    const status: StopStatus = pct === 100 ? 'fullfort' : passed > 0 ? 'pagar' : 'last'
    return {
      key: stopKey,
      label: STOP_LABELS[stopKey] ?? stopKey.charAt(0).toUpperCase() + stopKey.slice(1),
      total: stopTasks.length,
      passed,
      pct,
      status,
      tasks: stopTasks.map((task) => ({
        task,
        score: task.id ? scoreMap.value.get(task.id) : undefined,
      })),
      notebook: notebookByStop.value.get(stopKey),
    }
  })
})

function toggleNotebook(stopKey: string) {
  openNotebookStop.value = openNotebookStop.value === stopKey ? null : stopKey
}
</script>

<template>
  <SceneComponent
    :title="displayName"
    :previous="`/classroom/${classroomId}`"
    background="/StartBackground.jpg"
    dimOverlay
  >
    <div v-if="isLoading" class="loading-state">Laster...</div>

    <div v-else class="progress-layout">

      <div
        class="tab-bar"
        role="tablist"
        aria-label="Innholdsvelger"
        @keydown="onTabKeydown"
      >
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :ref="(el) => setTabRef(tab.key, el as Element | null)"
          type="button"
          class="tab-btn btn-bare"
          :class="{ 'tab-btn--active': activeTab === tab.key }"
          role="tab"
          :aria-selected="activeTab === tab.key"
          :tabindex="activeTab === tab.key ? 0 : -1"
          @click="activeTab = tab.key"
        >
          <span class="tab-label">{{ tab.label }}</span>
        </button>
      </div>

      <div v-show="activeTab === 'overview'">
        <div class="stat-grid">
          <div class="stat-card" v-for="(stat, i) in [
            { value: `${completionPct}%`, label: 'Fullført' },
            { value: totalScore, label: 'Totalpoeng' },
            { value: joinedAt, label: 'Ble med' },
          ]" :key="stat.label" :style="{ animationDelay: `${i * 0.04}s` }">
            <span class="stat-value">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>

        <p v-if="stopData.length === 0" class="empty-text" style="padding: 1.5rem;">
          Ingen aktive oppgaver er lagt til enda.
        </p>

        <div class="stop-list">
          <section
            v-for="(stop, si) in stopData"
            :key="stop.key"
            class="panel"
            :style="{ animationDelay: `${0.1 + si * 0.06}s` }"
          >
            <div class="stop-header">
              <div class="stop-header-left">
                <span class="dot" :class="`dot--${stop.status === 'fullfort' ? 'green' : stop.status === 'pagar' ? 'amber' : 'grey'}`" />
                <h3 class="stop-title">{{ stop.label }}</h3>
              </div>
              <span class="stop-badge" :class="`stop-badge--${stop.status}`">
                {{ stop.status === 'fullfort' ? 'Fullført' : stop.status === 'pagar' ? 'Pågår' : 'Låst' }}
              </span>
            </div>

            <div class="progress-row">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :class="`progress-fill--${stop.status}`"
                  :style="{ width: `${stop.pct}%` }"
                />
              </div>
              <span class="progress-pct">{{ stop.pct }}%</span>
            </div>

            <div class="task-list">
              <div
                v-for="{ task, score } in stop.tasks"
                :key="task.id"
                class="task-row"
                :class="score?.firstPassedAt ? 'task-row--passed' : ''"
              >
                <span class="task-check" :class="score?.firstPassedAt ? 'task-check--done' : ''">
                  {{ score?.firstPassedAt ? '✓' : '○' }}
                </span>
                <span class="task-name">{{ task.title }}</span>
                <div class="task-meta">
                  <span v-if="score" class="task-score">{{ score.bestScore ?? 0 }}p</span>
                  <span v-if="score" class="task-attempts">{{ score.totalAttempts }} forsøk</span>
                  <span v-if="score?.firstPassedAt" class="task-date">{{ formatDate(score.firstPassedAt) }}</span>
                  <span v-if="!score" class="task-none">ikke forsøkt</span>
                </div>
              </div>
            </div>

            <div class="notebook-section">
              <button class="btn-bare notebook-btn" @click="toggleNotebook(stop.key)">
                {{ openNotebookStop === stop.key ? 'Skjul notater' : 'Se notater ›' }}
              </button>
              <div v-if="openNotebookStop === stop.key" class="notebook-content">
                <p v-if="stop.notebook?.studentReflection" class="notebook-text">
                  "{{ stop.notebook.studentReflection }}"
                </p>
                <p v-else class="notebook-empty">
                  Ingen refleksjon skrevet for dette stoppestedet.
                </p>
              </div>
            </div>
          </section>
        </div>

      </div>

      <div v-show="activeTab === 'achievements'">
        <div class="stat-grid">
          <div class="stat-card" v-for="(stat, i) in [
            { value: xpLevel, label: 'Nivå' },
            { value: xpPoints, label: 'XP' },
            { value: earnedAchievements.length, label: 'Medaljer' },
          ]" :key="stat.label" :style="{ animationDelay: `${i * 0.04}s` }">
            <span class="stat-value">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>

        <p v-if="allAchievements.length === 0" class="empty-text" style="padding: 1.5rem;">
          Ingen prestasjoner er opprettet enda.
        </p>

        <div v-else class="achievement-list">
          <div
            v-for="(ach, i) in allAchievements"
            :key="String(ach.id)"
            class="achievement-row"
            :class="earnedIds.has(ach.id) ? 'achievement-row--earned' : 'achievement-row--locked'"
            :style="{ animationDelay: `${i * 0.03}s` }"
          >
            <span class="achievement-icon">{{ earnedIds.has(ach.id) ? '★' : '○' }}</span>
            <div class="achievement-info">
              <span class="achievement-title">{{ ach.title }}</span>
              <span v-if="ach.description" class="achievement-desc">{{ ach.description }}</span>
            </div>
            <span class="achievement-status">{{ earnedIds.has(ach.id) ? 'Opptjent' : 'Låst' }}</span>
          </div>
        </div>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.loading-state {
  text-align: center;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 700;
  padding: 3rem 0;
}

.progress-layout {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 42rem;
  margin: 0 auto;
  padding: 0 1rem 2.5rem;
  gap: 1rem;
}

.tab-bar {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  padding-bottom: 0;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 0.25rem;
  font-size: 1.25rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.95);
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s;
}

.tab-btn:hover {
  color: rgba(255, 255, 255, 0.85);
}

.tab-btn:focus-visible {
  outline: 2px solid #ffffff;
  outline-offset: 2px;
}

.tab-btn--active {
  color: #ffffff;
  border-bottom-color: #ffffff;
  font-weight: 700;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem;
  width: 100%;
  margin-bottom: 1.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  padding: 1rem 0.5rem;
  background: rgba(255, 255, 255, 0.95);
  clip-path: var(--notch-clip);
  animation: slideUp 0.4s ease-out both;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 800;
  color: #111827;
  line-height: 1.2;
  -webkit-text-stroke: 0;
  paint-order: normal;
  text-shadow: none;
}

.stat-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.stop-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.panel {
  width: 100%;
  padding: 1.25rem;
  background: rgba(255, 255, 255, 0.95);
  clip-path: var(--notch-clip);
  animation: slideUp 0.4s ease-out both;
}

.empty-text {
  color: rgba(255, 255, 255, 0.85);
  font-size: 0.875rem;
  font-weight: 600;
  text-align: center;
  margin: 0;
}

.achievement-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.achievement-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  clip-path: var(--notch-clip);
  animation: slideUp 0.4s ease-out both;
}

.achievement-row--earned {
  background: rgba(255, 255, 255, 0.95);
}

.achievement-row--locked {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.achievement-icon {
  font-size: 1.125rem;
  flex-shrink: 0;
}

.achievement-row--earned .achievement-icon {
  color: #d97706;
}

.achievement-row--locked .achievement-icon {
  color: rgba(255, 255, 255, 0.75);
}

.achievement-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.achievement-title {
  font-size: 0.9375rem;
  font-weight: 700;
}

.achievement-row--earned .achievement-title {
  color: #111827;
}

.achievement-row--locked .achievement-title {
  color: rgba(255, 255, 255, 0.85);
}

.achievement-desc {
  font-size: 0.75rem;
  font-weight: 500;
}

.achievement-row--earned .achievement-desc {
  color: #6b7280;
}

.achievement-row--locked .achievement-desc {
  color: rgba(255, 255, 255, 0.75);
}

.achievement-status {
  font-size: 0.75rem;
  font-weight: 700;
  flex-shrink: 0;
}

.achievement-row--earned .achievement-status {
  color: #16a34a;
}

.achievement-row--locked .achievement-status {
  color: rgba(255, 255, 255, 0.75);
}

/* --- Stop header --- */
.stop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.stop-header-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.stop-title {
  font-size: 1.125rem;
  font-weight: 800;
  color: #111827;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-text-stroke: 0;
  paint-order: normal;
  text-shadow: none;
}

.dot {
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot--green { background: #22c55e; }
.dot--amber { background: #f59e0b; }
.dot--grey { background: #d1d5db; }

.stop-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.25rem 0.65rem;
  border-radius: 9999px;
  white-space: nowrap;
  flex-shrink: 0;
}

.stop-badge--fullfort { background: #dcfce7; color: #166534; }
.stop-badge--pagar { background: #fef3c7; color: #92400e; }
.stop-badge--last { background: #f3f4f6; color: #6b7280; }

.progress-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.progress-track {
  flex: 1;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.4s ease;
}
.progress-fill--fullfort { background: #22c55e; }
.progress-fill--pagar { background: #f59e0b; }
.progress-fill--last { background: #d1d5db; }

.progress-pct {
  font-size: 0.75rem;
  font-weight: 700;
  color: #6b7280;
  min-width: 2.5rem;
  text-align: right;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.task-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.6rem 0.75rem;
  background: #f9fafb;
  border-radius: 0.375rem;
}

.task-row--passed {
  background: #f0fdf4;
}

.task-check {
  font-weight: 800;
  font-size: 0.875rem;
  color: #6b7280;
  flex-shrink: 0;
}
.task-check--done { color: #16a34a; }

.task-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-row--passed .task-name { color: #111827; }

.task-meta {
  display: flex;
  gap: 0.75rem;
  flex-shrink: 0;
  font-size: 0.75rem;
}

.task-score { font-weight: 700; color: #374151; }
.task-attempts { color: #6b7280; }
.task-date { color: #16a34a; font-weight: 600; }
.task-none { font-style: italic; color: #6b7280; }

.notebook-section {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid #e5e7eb;
}

.notebook-btn {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #4b5563;
  cursor: pointer;
  padding: 0.4rem 0.75rem;
  background: #f3f4f6;
  border-radius: 0.375rem;
  transition: background 0.15s, color 0.15s;
}

.notebook-btn:hover {
  background: #e5e7eb;
  color: #111827;
}

.notebook-btn:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

.notebook-content {
  margin-top: 0.5rem;
}

.notebook-empty {
  color: #6b7280;
  font-size: 0.8125rem;
  font-weight: 500;
  font-style: italic;
  text-align: center;
  margin: 0;
  padding: 0.5rem 0;
}

.notebook-text {
  padding: 0.75rem 1rem;
  background: #fefce8;
  border: 1px solid #fde68a;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  font-style: italic;
  color: #374151;
  margin: 0;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .task-row {
    flex-wrap: wrap;
    gap: 0.3rem 0.5rem;
  }

  .task-name {
    width: 100%;
    min-width: 0;
  }

  .task-meta {
    width: 100%;
    gap: 0.5rem;
  }

  .tab-bar {
    padding: 0 0.5rem;
  }
}
</style>
