<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useFocusTrap } from '@/composables/UseFocusTrap'
import SceneComponent from '@/components/SceneComponent.vue'
import SidePanelMenu, { type SidePanelMenuItem } from '@/components/SidePanelMenu.vue'
import ClassroomTasksPanel from '@/components/ClassroomTasksPanel.vue'
import ClassroomMysteriesPanel from '@/components/ClassroomMysteriesPanel.vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { Bell, Settings, Gamepad2, X, Search } from 'lucide-vue-next'
import {
  getClassroomOptions,
  getMyNotificationsOptions,
  getStudentsOptions,
  getStudentsQueryKey,
  getClassroomLeaderboardOptions,
  getClassroomTasksOptions,
  approveMutation,
  removeMutation,
} from '@/generated-client/@tanstack/vue-query.gen.ts'
import apiClient from '@/services/ApiService'
import MysteryService from '@/services/MysteryService'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const classroomId = route.params.id as string

const { data } = useQuery(getClassroomOptions({ path: { id: classroomId } }))
const classroom = computed(() => data.value?.data ?? null)

const { data: studentsData, isLoading: studentsLoading } = useQuery(
  getStudentsOptions({ path: { classroomId } }),
)
const allStudents = computed(() => studentsData.value?.data ?? [])

const { data: leaderboardData } = useQuery(
  getClassroomLeaderboardOptions({ path: { classroomId } }),
)
const leaderboard = computed(() => leaderboardData.value?.data ?? [])

const { data: tasksData } = useQuery(getClassroomTasksOptions({ path: { classroomId } }))
const totalTasks = computed(() => tasksData.value?.data?.length ?? 0)

const { data: notificationsData } = useQuery({
  ...getMyNotificationsOptions(),
  refetchInterval: 5000,
})
const unreadCount = computed(
  () =>
    (notificationsData.value?.data ?? []).filter((n) => !n.isRead && n.classroomId === classroomId)
      .length,
)

const { data: pendingMysteries } = useQuery({
  queryKey: ['pendingMysteries', classroomId],
  queryFn: () => MysteryService.getPendingSubmissions(classroomId),
})
const pendingCount = computed(() => pendingMysteries.value?.length ?? 0)

// --- Leaderboard lookup by userId ---
function getLeaderboardEntry(userId?: string) {
  if (!userId) return null
  return leaderboard.value.find((e) => e.userId === userId) ?? null
}

// --- Student search ---
const searchQuery = ref('')
const showSearch = computed(() => allStudents.value.length >= 10)

const sortedStudents = computed(() => {
  return [...allStudents.value].sort((a, b) => {
    if (a.isAllowed !== b.isAllowed) return a.isAllowed ? -1 : 1
    const scoreA = getLeaderboardEntry(a.userId)?.totalScore ?? 0
    const scoreB = getLeaderboardEntry(b.userId)?.totalScore ?? 0
    return scoreB - scoreA
  })
})

const filteredStudents = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return sortedStudents.value
  return sortedStudents.value.filter((s) => s.displayName?.toLowerCase().includes(q))
})

const approve = useMutation({
  ...approveMutation(),
  onSuccess: () =>
    queryClient.invalidateQueries({ queryKey: getStudentsQueryKey({ path: { classroomId } }) }),
})

const remove = useMutation({
  ...removeMutation(),
  onSuccess: () => {
    confirmTarget.value = null
    queryClient.invalidateQueries({ queryKey: getStudentsQueryKey({ path: { classroomId } }) })
  },
})

function approveStudent(userId: string, event: Event) {
  event.stopPropagation()
  approve.mutate({ path: { classroomId, userId } })
}

const resetName = useMutation({
  mutationFn: async ({ userId }: { userId: string }) => {
    const { data } = await apiClient.post(
      `/classrooms/${classroomId}/students/${userId}/reset-name`,
    )
    return data
  },
  onSuccess: () => {
    resetTarget.value = null
    queryClient.invalidateQueries({ queryKey: getStudentsQueryKey({ path: { classroomId } }) })
  },
})

const resetTarget = ref<{ userId: string; displayName: string } | null>(null)
const resetTriggerRef = ref<HTMLButtonElement | null>(null)

function requestReset(userId: string, displayName: string, event: Event) {
  event.stopPropagation()
  resetTriggerRef.value = event.currentTarget as HTMLButtonElement
  resetTarget.value = { userId, displayName }
  nextTick(() => {
    const dialog = document.getElementById('reset-dialog')
    const cancelBtn = dialog?.querySelector<HTMLButtonElement>('[data-action="cancel"]')
    cancelBtn?.focus()
  })
}

function confirmReset() {
  if (!resetTarget.value) return
  resetName.mutate({ userId: resetTarget.value.userId })
}

function cancelReset() {
  resetTarget.value = null
  nextTick(() => resetTriggerRef.value?.focus())
}

const { onKeydown: onResetDialogKeydown } = useFocusTrap('reset-dialog', cancelReset)

const confirmTarget = ref<{ userId: string; displayName: string } | null>(null)
const confirmTriggerRef = ref<HTMLButtonElement | null>(null)

function requestRemove(userId: string, displayName: string, event: Event) {
  event.stopPropagation()
  confirmTriggerRef.value = event.currentTarget as HTMLButtonElement
  confirmTarget.value = { userId, displayName }
  nextTick(() => {
    const dialog = document.getElementById('remove-dialog')
    const cancelBtn = dialog?.querySelector<HTMLButtonElement>('[data-action="cancel"]')
    cancelBtn?.focus()
  })
}

function confirmRemove() {
  if (!confirmTarget.value) return
  remove.mutate({ path: { classroomId, userId: confirmTarget.value.userId } })
}

function cancelRemove() {
  confirmTarget.value = null
  nextTick(() => confirmTriggerRef.value?.focus())
}

const { onKeydown: onDialogKeydown } = useFocusTrap('remove-dialog', cancelRemove)

// --- Helpers ---
function studentAriaLabel(s: {
  displayName?: string
  userId?: string
  isAllowed?: boolean
}): string {
  if (!s.isAllowed) return `${s.displayName}, venter pa godkjenning`
  const entry = getLeaderboardEntry(s.userId)
  const score = entry ? `, poeng ${entry.totalScore}` : ''
  const rank = entry?.rank ? `, rangert ${entry.rank}` : ''
  return `Se progresjon for ${s.displayName}${score}${rank}`
}

function navigateToStudent(student: { userId?: string; isAllowed?: boolean }) {
  if (!student.isAllowed) return
  router.push({
    name: 'classroom-student-progress',
    params: { id: classroomId, userId: student.userId },
  })
}

const menuItems = computed<SidePanelMenuItem[]>(() => [
  {
    key: 'notifications',
    title: 'Varsler',
    subtitle: 'Meldinger fra klassen',
    icon: Bell,
    badge: unreadCount.value,
  },
  {
    key: 'classroom-settings',
    title: 'Klasseromsstyring',
    subtitle: 'Rediger innstillinger',
    icon: Settings,
  },
  { key: 'classroom-test-game', title: 'Test Spillet', subtitle: 'Spill som elev', icon: Gamepad2 },
])

function handleMenuItem(item: SidePanelMenuItem) {
  if (item.key === 'notifications') {
    router.push({ name: 'teacher-notifications', query: { classroomId } })
    return
  }
  router.push({ name: item.key, params: { id: classroomId } })
}

type DetailTab = 'students' | 'tasks' | 'mysteries'
const activeTab = ref<DetailTab>('students')
const tabs: { key: DetailTab; label: string }[] = [
  { key: 'students', label: 'Elever' },
  { key: 'tasks', label: 'Oppgaver' },
  { key: 'mysteries', label: 'Ukens mysterium' },
]
const tabRefs = ref<Record<DetailTab, HTMLButtonElement | null>>({
  students: null,
  tasks: null,
  mysteries: null,
})

function setTabRef(key: DetailTab, el: Element | null) {
  tabRefs.value[key] = el as HTMLButtonElement | null
}

function onTabKeydown(e: KeyboardEvent) {
  if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return
  e.preventDefault()
  const order: DetailTab[] = ['students', 'tasks', 'mysteries']
  const i = order.indexOf(activeTab.value)
  const next =
    e.key === 'ArrowRight'
      ? order[(i + 1) % order.length]!
      : order[(i - 1 + order.length) % order.length]!
  activeTab.value = next
  nextTick(() => tabRefs.value[next]?.focus())
}
</script>

<template>
  <SceneComponent
    :title="classroom?.title || 'Klasserom'"
    previous="/teacher-home"
    background="/StartBackground.jpg"
    dimOverlay
    lockViewport
  >
    <div class="scene-split-layout">
      <div class="scene-split-grid">
        <section class="scene-split-main" aria-label="Klasseromsinnhold">
          <div class="tab-bar" role="tablist" aria-label="Innholdsvelger" @keydown="onTabKeydown">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              :ref="(el) => setTabRef(tab.key, el as Element | null)"
              type="button"
              class="tab-btn btn-bare"
              :class="{ 'tab-btn--active': activeTab === tab.key }"
              role="tab"
              :aria-selected="activeTab === tab.key"
              :aria-controls="`tab-panel-${tab.key}`"
              :id="`tab-${tab.key}`"
              :tabindex="activeTab === tab.key ? 0 : -1"
              @click="activeTab = tab.key"
            >
              <span class="tab-label">{{ tab.label }}</span>
              <span class="tab-count">
                {{ tab.key === 'students' ? allStudents.length : tab.key === 'tasks' ? totalTasks : pendingCount }}
              </span>
            </button>
          </div>

          <div
            v-show="activeTab === 'tasks'"
            :id="`tab-panel-tasks`"
            role="tabpanel"
            aria-labelledby="tab-tasks"
            class="tab-panel-content"
          >
            <ClassroomTasksPanel :classroom-id="classroomId" />
          </div>

          <div
            v-show="activeTab === 'mysteries'"
            :id="`tab-panel-mysteries`"
            role="tabpanel"
            aria-labelledby="tab-mysteries"
            class="tab-panel-content"
          >
            <ClassroomMysteriesPanel :classroom-id="classroomId" />
          </div>

          <div
            v-show="activeTab === 'students'"
            :id="`tab-panel-students`"
            role="tabpanel"
            aria-labelledby="tab-students"
            class="students-panel tab-panel-content"
          >
            <div v-if="showSearch" class="search-row">
              <Search :size="14" class="search-icon" aria-hidden="true" />
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="Søk etter elev..."
                aria-label="Søk etter elev"
              />
            </div>

            <div v-if="studentsLoading" class="section-empty">Laster elever...</div>

            <div v-else-if="allStudents.length === 0" class="empty-state">
              <p class="empty-title">Ingen elever ennå</p>
              <p class="empty-hint">
                Del klasseromskoden
                <span class="empty-code">{{ classroom?.joinCode ?? '---' }}</span>
                så elevene kan bli med.
              </p>
            </div>

            <ul v-else class="student-list" role="list" aria-label="Elever i klasserommet">
              <li
                v-for="(student, i) in filteredStudents"
                :key="student.userId"
                class="student-row-wrapper"
                :style="{ animationDelay: `${0.04 + i * 0.02}s` }"
              >
                <div v-if="!student.isAllowed" class="student-row student-row--pending">
                  <span class="sr-name">{{ student.displayName }}</span>
                  <span class="status-badge status-badge--pending">
                    <span class="status-dot status-dot--amber" aria-hidden="true" />
                    Venter
                  </span>
                  <div class="sr-actions">
                    <button
                      class="btn-bare sr-btn sr-btn--approve"
                      :aria-label="`Godkjenn ${student.displayName}`"
                      @click="student.userId && approveStudent(student.userId, $event)"
                    >
                      Godkjenn
                    </button>
                    <button
                      class="btn-bare sr-btn sr-btn--icon"
                      :aria-label="`Fjern ${student.displayName} fra klasserommet`"
                      @click="
                        student.userId &&
                        requestRemove(student.userId, student.displayName ?? 'Elev', $event)
                      "
                    >
                      <X :size="18" aria-hidden="true" />
                    </button>
                  </div>
                </div>

                <button
                  v-else
                  class="btn-bare student-row student-row--approved"
                  :aria-label="studentAriaLabel(student)"
                  @click="navigateToStudent(student)"
                >
                  <span class="sr-name">{{ student.displayName }}</span>

                  <span class="sr-stat"
                    >{{ getLeaderboardEntry(student.userId)?.totalScore ?? 0 }} p</span
                  >
                  <span class="sr-stat"
                    >Lv. {{ getLeaderboardEntry(student.userId)?.xpLevel ?? 1 }}</span
                  >
                  <span class="sr-stat"
                    >#{{ getLeaderboardEntry(student.userId)?.rank ?? '-' }}</span
                  >

                  <div v-if="totalTasks > 0" class="sr-progress">
                    <div class="sr-progress-bar">
                      <div
                        class="sr-progress-fill"
                        :style="{
                          width: `${Math.min(100, ((getLeaderboardEntry(student.userId)?.totalScore ?? 0) / (totalTasks * 100)) * 100)}%`,
                        }"
                      ></div>
                    </div>
                  </div>

                  <div class="sr-actions">
                    <button
                      class="btn-bare sr-btn sr-btn--reset"
                      :aria-label="`Fjern brukernavn for ${student.displayName}`"
                      @click="
                        student.userId &&
                        requestReset(student.userId, student.displayName ?? 'Elev', $event)
                      "
                    >
                      Fjern navn
                    </button>
                    <button
                      class="btn-bare sr-btn sr-btn--icon"
                      :aria-label="`Fjern ${student.displayName} fra klasserommet`"
                      @click="
                        student.userId &&
                        requestRemove(student.userId, student.displayName ?? 'Elev', $event)
                      "
                    >
                      <X :size="18" aria-hidden="true" />
                    </button>
                  </div>

                  <span class="sr-chevron" aria-hidden="true">&rsaquo;</span>
                </button>
              </li>

              <li v-if="showSearch && filteredStudents.length === 0" class="section-empty">
                Ingen treff for &laquo;{{ searchQuery }}&raquo;
              </li>
            </ul>
          </div>
        </section>

        <aside class="scene-split-side">
          <SidePanelMenu
            :items="menuItems"
            :code="{ label: 'Klasseromskode', value: classroom?.joinCode ?? '---' }"
            aria-label="Klasseromsmeny"
            @select="handleMenuItem"
          />
        </aside>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="confirmTarget"
        class="dialog-backdrop"
        @click.self="cancelRemove"
        @keydown="onDialogKeydown"
      >
        <div
          id="remove-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="remove-dialog-title"
          class="dialog-box"
        >
          <h3 id="remove-dialog-title" class="dialog-title">
            Fjern {{ confirmTarget.displayName }}?
          </h3>
          <p class="dialog-body">Eleven vil miste tilgang til klasserommet.</p>
          <div class="dialog-actions">
            <button
              data-action="cancel"
              class="btn-bare dialog-btn dialog-btn--cancel"
              @click="cancelRemove"
            >
              Avbryt
            </button>
            <button
              class="btn-bare dialog-btn dialog-btn--danger"
              :disabled="remove.isPending.value"
              @click="confirmRemove"
            >
              {{ remove.isPending.value ? 'Fjerner...' : 'Fjern' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="resetTarget"
        class="dialog-backdrop"
        @click.self="cancelReset"
        @keydown="onResetDialogKeydown"
      >
        <div
          id="reset-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="reset-dialog-title"
          class="dialog-box"
        >
          <h3 id="reset-dialog-title" class="dialog-title">
            Fjern brukernavn for {{ resetTarget.displayName }}?
          </h3>
          <p class="dialog-body">Brukernavnet blir tilbakestilt til e-postprefikset.</p>
          <div class="dialog-actions">
            <button
              data-action="cancel"
              class="btn-bare dialog-btn dialog-btn--cancel"
              @click="cancelReset"
            >
              Avbryt
            </button>
            <button
              class="btn-bare dialog-btn dialog-btn--danger"
              :disabled="resetName.isPending.value"
              @click="confirmReset"
            >
              {{ resetName.isPending.value ? 'Tilbakestiller...' : 'Fjern Brukernavn' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </SceneComponent>
</template>

<style scoped>
.tab-bar {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 1rem;
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
  transition:
    color 0.15s,
    border-color 0.15s;
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

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.35rem;
  padding: 0 0.35rem;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.15);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0;
  color: rgba(255, 255, 255, 0.7);
}

.tab-btn--active .tab-count {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
}

.students-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.tab-panel-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.search-row {
  position: relative;
  isolation: isolate;
  margin-bottom: 0.625rem;
  animation: slideUp 0.5s ease-out both;
  clip-path: var(--notch-clip);
}

.search-row::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.9);
  z-index: -2;
}

.search-row::after {
  content: '';
  position: absolute;
  inset: 2px;
  clip-path: var(--notch-clip);
  background: rgba(20, 15, 10, 0.75);
  backdrop-filter: blur(2px);
  z-index: -1;
}

.search-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.7);
  pointer-events: none;
  z-index: 1;
}

.search-input {
  position: relative;
  width: 100%;
  padding: 0.75rem 0.875rem 0.75rem 2.75rem !important;
  border: none !important;
  border-radius: 0 !important;
  background: transparent !important;
  color: white !important;
  font-size: 1rem;
  font-weight: 400 !important;
  outline: none;
  box-sizing: border-box;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.7) !important;
}

.search-input:focus {
  outline: none;
}

.student-list {
  list-style: none;
  margin: 0;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 0.875rem;
  backdrop-filter: blur(4px);
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.student-list::-webkit-scrollbar {
  width: 8px;
}
.student-list::-webkit-scrollbar-track {
  background: transparent;
}
.student-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.18);
  border-radius: 4px;
}
.student-list::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

.student-row-wrapper {
  animation: slideUp 0.4s ease-out both;
}

.student-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.85rem 1.25rem;
  border: none;
  background: rgba(255, 255, 255, 0.95);
  clip-path: var(--notch-clip);
  text-align: left;
  font-family: inherit;
  font-size: inherit;
  position: relative;
}

.student-row--approved {
  cursor: pointer;
  transition: background 0.15s;
}

.student-row--approved:hover {
  background: rgba(255, 255, 255, 1);
}

.student-row--approved:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

.student-row--pending {
  background: rgba(255, 255, 255, 0.88);
}

.sr-name {
  font-size: 1.125rem;
  font-weight: 800;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 5rem;
  flex: 1;
  flex-shrink: 1;
}

.sr-stat {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #4b5563;
  white-space: nowrap;
  flex-shrink: 0;
}

.sr-progress {
  flex: 1;
  min-width: 3rem;
  max-width: 6rem;
}

.sr-progress-bar {
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.sr-progress-fill {
  height: 100%;
  background: #2563eb;
  border-radius: 2px;
  transition: width 0.4s ease;
}

.sr-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
  margin-left: auto;
}

.sr-btn {
  border: none;
  border-radius: 0.5rem;
  font-size: 0.9375rem;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.15s,
    color 0.15s,
    border-color 0.15s;
  white-space: nowrap;
  min-height: 2.5rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.sr-btn:focus-visible {
  outline: 2px solid #d97706;
  outline-offset: 2px;
}

.sr-btn--approve {
  min-height: 2.75rem;
  padding: 0.55rem 1.15rem;
  background: #15803d;
  color: white;
  font-size: 0.9375rem;
}

.sr-btn--approve:hover {
  background: #166534;
}

.sr-btn--reset {
  min-height: 2.75rem;
  padding: 0.55rem 1.15rem;
  background: #fee2e2;
  color: #b91c1c;
  border: 1.5px solid #fca5a5;
  border-radius: 0.5rem;
  font-size: 0.9375rem;
}

.sr-btn--reset:hover {
  background: #fecaca;
  color: #991b1b;
  border-color: #f87171;
}

.sr-btn--icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.75rem;
  height: 2.75rem;
  padding: 0;
  background: transparent;
  color: #6b7280;
  border-radius: 0.5rem;
}

.sr-btn--icon:hover {
  color: #dc2626;
  background: #fef2f2;
}

.sr-chevron {
  color: #9ca3af;
  font-size: 1.5rem;
  font-weight: 700;
  flex-shrink: 0;
  transition:
    transform 0.15s,
    color 0.15s;
  margin-left: 0.25rem;
}

.student-row--approved:hover .sr-chevron {
  transform: translateX(2px);
  color: #9ca3af;
}

/* ---- Status badge ---- */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
  font-weight: 700;
  padding: 0.3rem 0.75rem;
  border-radius: 9999px;
  white-space: nowrap;
}

.status-badge--pending {
  background: #fef3c7;
  color: #92400e;
}

.status-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot--amber {
  background: #d97706;
}

.empty-state {
  text-align: center;
  padding: 2.5rem 1rem;
  animation: slideUp 0.5s ease-out both;
}

.empty-title {
  font-size: 1.15rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 0.5rem;
}

.empty-hint {
  font-size: 1.05rem;
  color: rgba(255, 255, 255, 0.65);
  margin: 0;
  line-height: 1.5;
}

.empty-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  letter-spacing: 0.08em;
}

.section-empty {
  text-align: center;
  color: rgba(255, 255, 255, 0.75);
  font-size: 0.8125rem;
  margin: 0.75rem 0;
  animation: slideUp 0.5s ease-out both;
}

@media (max-width: 950px) {
  .student-row {
    flex-wrap: wrap;
    gap: 0.5rem;
    padding: 0.75rem 1.25rem;
  }

  .sr-name {
    width: 100%;
    white-space: normal;
    min-width: 0;
  }

  .sr-stat {
    font-size: 0.8125rem;
  }

  .sr-progress {
    display: none;
  }

  .sr-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .sr-btn {
    font-size: 0.8125rem;
    min-height: 2.25rem;
  }

  .sr-btn--approve {
    font-size: 0.8125rem;
    padding: 0.45rem 0.85rem;
    min-height: 2.25rem;
  }

  .sr-btn--reset {
    font-size: 0.8125rem;
    padding: 0.45rem 0.85rem;
    min-height: 2.25rem;
  }

  .sr-chevron {
    display: none;
  }

  .student-list {
    padding: 0.75rem;
  }

  .tab-bar {
    padding: 0 0.5rem;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
