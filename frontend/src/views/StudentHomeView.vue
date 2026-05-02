<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick, onBeforeUnmount, watch, type ComponentPublicInstance } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import SceneComponent from '../components/SceneComponent.vue'
import BoardNote from '../components/BoardNote.vue'
import AvatarPreview from '@/components/avatar/AvatarPreview.vue'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import ClassroomService from '@/services/ClassroomService'
import { getProfileOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import { parseAvatarConfig } from '@/utils/avatar'
import { useDialogRunner } from '@/composables/UseDialogRunner'
import { introductionDialogs, introductionStartId } from '@/data/IntroductionDialogs'
import { useStoryProgressStore, StoryStep } from '@/stores/StoryProgressStore'
import { useAuthStore } from '@/stores/AuthStore'
import { useAudioStore } from '@/stores/AudioStore'
import type { DialogChoice } from '@/types/cutscene'
import { useToast } from '@/composables/UseToast'

const mapImage = '/map.jpg'
const badgeImage = '/unlockedBadge.png'
const questionmarkImage = '/questionmarkIcon.png'
const notebookImage = '/notebook.jpg'
const leaderboardImage = '/leaderboard.jpg'
const mysteryImage = '/mystery-magnifier.png'

const router = useRouter()
const { showToast } = useToast()
const storyStore = useStoryProgressStore()
const authStore = useAuthStore()
const audio = useAudioStore()
const introRunner = useDialogRunner(introductionDialogs, introductionStartId)
watch(() => introRunner.currentScene.value?.id, (id) => {
  if (id === 'detective-success') audio.playOneShot('/music/FunFunFunFun.mp3')
})
const showIntro = ref(false)
const classroomId = ref<string | null>(null)
const classroomLoading = ref(true)
const showHelp = ref(false)

function markIntroSeen() {
  const key = `nettdetektivene:introSeen:${authStore.user?.id ?? 'anon'}`
  localStorage.setItem(key, '1')
}

function onIntroDone() {
  introRunner.advance()
  if (introRunner.isFinished.value) {
    showIntro.value = false
    markIntroSeen()
    storyStore.advance(StoryStep.INTRODUCTION)
  }
}

function onIntroChoose(choice: DialogChoice) {
  introRunner.advance(choice)
  if (introRunner.isFinished.value) {
    showIntro.value = false
    markIntroSeen()
    storyStore.advance(StoryStep.INTRODUCTION)
  }
}
const boardFrame = ref<HTMLElement | null>(null)
const stringPaths = ref<{ d: string; color: string }[]>([])
let boardResizeObserver: ResizeObserver | null = null

const noteOrder = [
  'note-profile',
  'note-map',
  'note-achievements',
  'note-help',
  'note-notebook',
  'note-leaderboard',
  'note-mystery',
] as const

type NoteKey = (typeof noteOrder)[number]
const noteRefs: Partial<Record<NoteKey, HTMLElement>> = {}
const stringConnections: Array<[NoteKey, NoteKey, string]> = [
  ['note-profile', 'note-map', '#9b1c1c'],
  ['note-map', 'note-achievements', '#9b1c1c'],
  ['note-achievements', 'note-notebook', '#9b1c1c'],
  ['note-notebook', 'note-help', '#9b1c1c'],
  ['note-notebook', 'note-mystery', '#9b1c1c'],
  ['note-mystery', 'note-leaderboard', '#9b1c1c'],
  ['note-leaderboard', 'note-map', '#9b1c1c'],
]
const connectionSagOverrides: Partial<Record<`${NoteKey}->${NoteKey}`, number>> = {
  'note-notebook->note-help': 4,
}

function setNoteRef(key: NoteKey) {
  return (el: Element | ComponentPublicInstance | null) => {
    if (el instanceof HTMLElement) noteRefs[key] = el
    else delete noteRefs[key]
  }
}

const { data: profileData } = useQuery(getProfileOptions())
const profile = computed(() => profileData.value?.data ?? null)
const avatarCfg = computed(() => parseAvatarConfig(profile.value?.avatarConfig ?? null))
const studentName = computed(() => profile.value?.displayName || 'Detektiv')

function goTo(route: string) {
  router.push(route)
}

function buildSaggingPath(
  start: { x: number; y: number },
  end: { x: number; y: number },
  sag = 7,
) {
  const dx = end.x - start.x
  const dy = end.y - start.y
  const length = Math.hypot(dx, dy)
  const normalizedSag = Math.min(12, Math.max(sag, length * 0.08))

  const control1 = {
    x: start.x + dx * 0.25,
    y: start.y + dy * 0.2 + normalizedSag,
  }
  const control2 = {
    x: end.x - dx * 0.25,
    y: end.y - dy * 0.2 + normalizedSag,
  }

  return `M ${start.x} ${start.y} C ${control1.x} ${control1.y}, ${control2.x} ${control2.y}, ${end.x} ${end.y}`
}

async function updateStringPaths() {
  await nextTick()

  const board = boardFrame.value
  if (!board) return

  const svgEl = board.querySelector('.string-layer')
  const refRect = svgEl ? svgEl.getBoundingClientRect() : board.getBoundingClientRect()
  if (!refRect.width || !refRect.height) return

  const pinCenters = new Map<NoteKey, { x: number; y: number }>()

  noteOrder.forEach((key) => {
    const noteEl = noteRefs[key]
    if (!noteEl) return
    const pin = noteEl.querySelector('.board-note-pin') as HTMLElement | null
    if (!pin) return
    const pinRect = pin.getBoundingClientRect()
    pinCenters.set(key, {
      x: ((pinRect.left + pinRect.width / 2 - refRect.left) / refRect.width) * 100,
      y: ((pinRect.top + pinRect.height / 2 - refRect.top) / refRect.height) * 100,
    })
  })

  stringPaths.value = stringConnections
    .map(([startKey, endKey, color]) => {
      const start = pinCenters.get(startKey)
      const end = pinCenters.get(endKey)
      const sag = connectionSagOverrides[`${startKey}->${endKey}`]
      return start && end ? { d: buildSaggingPath(start, end, sag), color } : null
    })
    .filter((p): p is { d: string; color: string } => p !== null)
}

async function goToNotebook() {
  if (!classroomId.value) {
    router.push({ name: 'notebook' })
    return
  }
  router.push({ name: 'notebook', query: { classroomId: classroomId.value } })
}

function goToMystery() {
  if (classroomLoading.value) return
  if (!classroomId.value) {
    showToast('Du er ikke med i noen klasse ennå.', 'error')
    return
  }
  router.push({ name: 'student-weekly-mystery', params: { classroomId: classroomId.value } })
}

function toggleHelp() {
  showHelp.value = !showHelp.value
}

function closeHelp() {
  showHelp.value = false
}

function onKeydown(e: Event) {
  if ('key' in e && e.key === 'Escape') closeHelp()
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('resize', updateStringPaths)

  await nextTick()
  updateStringPaths()

  boardFrame.value?.querySelectorAll('img').forEach((img) => {
    if (!img.complete) img.addEventListener('load', updateStringPaths, { once: true })
  })

  try {
    const classrooms = await ClassroomService.getMyClassrooms()
    classroomId.value = classrooms[0]?.id ?? null
  } catch {
    classroomId.value = null
  } finally {
    classroomLoading.value = false
  }

  if (boardFrame.value) {
    boardResizeObserver = new ResizeObserver(updateStringPaths)
    boardResizeObserver.observe(boardFrame.value)
  }

  if (!storyStore.loaded) await storyStore.load()
  const introLocalKey = `nettdetektivene:introSeen:${authStore.user?.id ?? 'anon'}`
  if (!storyStore.hasSeen(StoryStep.INTRODUCTION) && !localStorage.getItem(introLocalKey)) {
    showIntro.value = true
  }
})

watch(studentName, () => updateStringPaths())

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateStringPaths)
  boardResizeObserver?.disconnect()
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <SceneComponent :title="showIntro ? 'Introduksjon' : 'Detektivkontoret'" :background="showIntro ? '/StartBackground.jpg' : undefined">
    <CutsceneDialog
      v-if="showIntro && introRunner.currentScene.value"
      :key="introRunner.currentScene.value.id"
      :scene="introRunner.currentScene.value"
      @done="onIntroDone"
      @choose="onIntroChoose"
    />
    <div v-show="!showIntro" class="student-home">
      <div ref="boardFrame" class="board-frame">
        <svg class="string-layer" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          <path
            v-for="(path, index) in stringPaths"
            :key="index"
            :d="path.d"
            :stroke="path.color"
            class="string-line"
          />
        </svg>

        <div :ref="setNoteRef('note-profile')" class="note-slot note-profile">
          <BoardNote
            :rotation="-5"
            pin-color="#2f9e44"
            width="clamp(9rem, 22cqw, 14rem)"
            @click="goTo('/student-profile')"
          >
            <div class="profile-note">
              <AvatarPreview :config="avatarCfg" :size="104" :lazy-load="false" />
              <p class="image-note-title">{{ studentName }}</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-help')" class="note-slot note-help">
          <BoardNote
            :rotation="4"
            pin-color="#1c7ed6"
            width="clamp(7rem, 14cqw, 12rem)"
            @click="toggleHelp"
          >
            <div class="image-note">
              <img :src="questionmarkImage" :alt="showHelp ? 'Lukk hjelp' : 'Hjelp'" class="help-image image-note-bg" />
              <p class="image-note-title">{{ showHelp ? 'Lukk' : 'Hjelp' }}</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-map')" class="note-slot note-map">
          <BoardNote
            :rotation="-2"
            pin-color="#1c7ed6"
            width="clamp(16rem, 23cqw, 26rem)"
            @click="goTo('/game-map')"
          >
            <div class="image-note">
              <img :src="mapImage" alt="kart" class="map-image" />
              <p class="image-note-title">Spill</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-achievements')" class="note-slot note-achievements">
          <BoardNote
            :rotation="5"
            pin-color="#f59f00"
            width="clamp(7rem, 14cqw, 12rem)"
            @click="goTo('/achievements')"
          >
            <div class="image-note">
              <img :src="badgeImage" alt="Medaljer" class="image-note-bg achievements-image">
              <p class="image-note-title">Medaljer</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-notebook')" class="note-slot note-notebook">
          <BoardNote
            :rotation="-4"
            pin-color="#7048e8"
            width="clamp(7rem, 17cqw, 14rem)"
            @click="goToNotebook"
          >
            <div class="image-note">
              <img :src="notebookImage" alt="Notatblokk" class="image-note-bg">
              <p class="image-note-title">Notatbok</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-leaderboard')" class="note-slot note-leaderboard">
          <BoardNote
            :rotation="3"
            pin-color="#f59f00"
            width="clamp(7rem, 17cqw, 14rem)"
            @click="goTo('/student-leaderboard')"
          >
            <div class="image-note">
              <img :src="leaderboardImage" alt="Ledertavle" class="image-note-bg leaderboard-image">
              <p class="image-note-title">Ledertavle</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-mystery')" class="note-slot note-mystery">
          <BoardNote
            :rotation="-1"
            pin-color="#e03131"
            width="clamp(7rem, 14cqw, 11rem)"
            @click="goToMystery"
          >
            <div class="image-note">
              <img :src="mysteryImage" alt="Ukens Mysterium" class="mystery-image image-note-bg" />
              <p class="image-note-title">Ukens Mysterium</p>
            </div>
          </BoardNote>
        </div>

        <Transition name="fade">
          <div v-if="showHelp" class="help-overlay" @click.self="closeHelp">
            <div class="help-note help-note--overlay" style="top: 1%; left: 15%; transform: translate(-50%, 30%) rotate(-4deg)">
              <p class="help-note-title">Detektivprofil</p>
              <p class="help-note-text">Se profilen din, endre avatar og navn</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 60%; left: 49%; transform: translate(-50%, -130%) rotate(1.5deg)">
              <p class="help-note-title">Kartet</p>
              <p class="help-note-text">Utforsk kartet og finn detektivoppdrag</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 1%; left: 77%; transform: translate(-50%, 30%) rotate(3deg)">
              <p class="help-note-title">Medaljer</p>
              <p class="help-note-text">Se medaljer du har oppnådd</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 97%; left: 73%; transform: translate(-50%, -130%) rotate(-2deg)">
              <p class="help-note-title">Notatblokk</p>
              <p class="help-note-text">Skriv og les egne notater fra oppdragene</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 90%; left: 10%; transform: translate(-50%, -130%) rotate(2.5deg)">
              <p class="help-note-title">Ledertavle</p>
              <p class="help-note-text">Se hvilken klasse på skolen din som har flest poeng</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 97%; left: 29%; transform: translate(-50%, -130%) rotate(-1deg)">
              <p class="help-note-title">Ukens Mysterium</p>
              <p class="help-note-text">Send inn funn fra nettet og vinn en medalje</p>
            </div>
          </div>
        </Transition>
      </div>
    </div>

    <div v-show="!showIntro" class="mobile-nav">
      <div class="mobile-nav-grid">
        <BoardNote class="mobile-note-featured" :rotation="-0.5" pin-color="#1c7ed6" width="100%" @click="goTo('/game-map')">
          <div class="image-note">
            <img :src="mapImage" alt="Spill" class="mobile-map-img" />
            <p class="image-note-title">Spill</p>
          </div>
        </BoardNote>
        <BoardNote :rotation="2" pin-color="#2f9e44" width="100%" @click="goTo('/student-profile')">
          <div class="profile-note">
            <AvatarPreview :config="avatarCfg" :size="48" />
            <p class="image-note-title">{{ studentName }}</p>
          </div>
        </BoardNote>
        <BoardNote :rotation="-2" pin-color="#f59f00" width="100%" @click="goTo('/achievements')">
          <div class="image-note">
            <img :src="badgeImage" alt="Medaljer" class="mobile-nav-img achievements-image" />
            <p class="image-note-title">Medaljer</p>
          </div>
        </BoardNote>
        <BoardNote :rotation="1.5" pin-color="#f59f00" width="100%" @click="goTo('/student-leaderboard')">
          <div class="image-note">
            <img :src="leaderboardImage" alt="Ledertavle" class="mobile-nav-img" />
            <p class="image-note-title">Ledertavle</p>
          </div>
        </BoardNote>
        <BoardNote :rotation="-1.5" pin-color="#7048e8" width="100%" @click="goToNotebook">
          <div class="image-note">
            <img :src="notebookImage" alt="Notatbok" class="mobile-nav-img mobile-notebook-img" />
            <p class="image-note-title">Notatbok</p>
          </div>
        </BoardNote>
        <BoardNote :rotation="2.5" pin-color="#e03131" width="100%" @click="goToMystery">
          <div class="image-note">
            <img :src="mysteryImage" alt="Ukens Mysterium" class="mystery-image mobile-nav-img" />
            <p class="image-note-title">Ukens Mysterium</p>
          </div>
        </BoardNote>
        <BoardNote :rotation="-1" pin-color="#1c7ed6" width="100%" @click="toggleHelp">
          <div class="image-note">
            <img :src="questionmarkImage" alt="Hjelp" class="mobile-nav-img" />
            <p class="image-note-title">Hjelp</p>
          </div>
        </BoardNote>
      </div>

      <Transition name="fade">
        <div v-if="showHelp" class="mobile-help-overlay" @click.self="closeHelp">
          <div class="mobile-help-content">
            <span class="mobile-help-pin" aria-hidden="true" />
            <p class="mobile-help-title">Hjelp</p>
            <ul class="mobile-help-list">
              <li><strong>Spill</strong> — Utforsk kartet og finn detektivoppdrag</li>
              <li><strong>Detektivprofil</strong> — Se profilen din, endre avatar og navn</li>
              <li><strong>Medaljer</strong> — Se medaljer du har oppnådd</li>
              <li><strong>Ledertavle</strong> — Se hvilken klasse som har flest poeng</li>
              <li><strong>Notatbok</strong> — Skriv og les egne notater fra oppdragene</li>
              <li><strong>Ukens Mysterium</strong> — Send inn funn fra nettet og vinn en medalje</li>
            </ul>
            <button class="mobile-help-close btn-bare" @click="closeHelp">Lukk</button>
          </div>
        </div>
      </Transition>
    </div>
  </SceneComponent>
</template>

<style scoped>
:deep(.scene) {
  height: 100dvh;
  min-height: 100dvh;
  overflow: hidden;
}

:deep(.scene-content) {
  height: 100dvh;
  min-height: 0;
}

:deep(.scene-body) {
  min-height: 0;
  overflow: hidden;
}

.mobile-nav {
  display: none;
}

@media (max-width: 1024px) {
  :deep(.scene) {
    height: auto;
    min-height: 100dvh;
    overflow-y: auto;
  }
  :deep(.scene-content) {
    height: auto;
    min-height: 100dvh;
  }
  :deep(.scene-body) {
    overflow: visible;
  }

  .student-home {
    display: none !important;
  }

  .mobile-nav {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 0.75rem;
  }
}

.mobile-nav-grid {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto repeat(3, 1fr);
  gap: 1.1rem;
  width: 100%;
  max-width: 480px;
  padding: 1.4rem 1.2rem 1.4rem;
}

.mobile-note-featured {
  grid-column: 1 / -1;
}

.mobile-nav-grid :deep(.board-note) {
  padding: 0.7rem 0.4rem 0.45rem;
  width: 100% !important;
}

.mobile-nav-grid :deep(.image-note-title),
.mobile-nav-grid :deep(.profile-note .image-note-title) {
  font-size: 0.72rem;
  min-height: unset;
  padding: 0.2rem 0.4rem 0.25rem;
}

.mobile-map-img {
  display: block;
  width: 40%;
  height: auto;
  margin: 0 auto;
  border-radius: 2px;
  mix-blend-mode: multiply;
}

.mobile-nav-img {
  display: block;
  width: 40px;
  height: 40px;
  object-fit: contain;
  margin: 0 auto;
  mix-blend-mode: multiply;
}

.mobile-notebook-img {
  width: 52px;
  height: 52px;
}

.mobile-help-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.mobile-help-content {
  position: relative;
  background-image:
    repeating-linear-gradient(
      transparent,
      transparent calc(1.65rem - 1px),
      rgba(100, 149, 237, 0.2) calc(1.65rem - 1px),
      rgba(100, 149, 237, 0.2) 1.65rem
    ),
    linear-gradient(
      to right,
      transparent 2.4rem,
      rgba(211, 47, 47, 0.28) 2.4rem,
      rgba(211, 47, 47, 0.28) calc(2.4rem + 1px),
      transparent calc(2.4rem + 1px)
    ),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='300'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='300' height='300' filter='url(%23n)' opacity='0.045'/%3E%3C/svg%3E"),
    linear-gradient(160deg, #fefcf5 0%, #faf5e4 60%, #f5ecce 100%);
  background-size: auto, auto, 300px 300px, 100% 100%;
  border-radius: 1px;
  padding: 1.6rem 1.25rem 1.25rem 3rem;
  max-width: 340px;
  width: 100%;
  box-shadow:
    0 2px 2px rgba(0, 0, 0, 0.12),
    0 8px 18px rgba(0, 0, 0, 0.28),
    0 16px 28px -4px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    inset -1px 0 0 rgba(0, 0, 0, 0.04),
    inset 0 -1px 0 rgba(0, 0, 0, 0.06);
}

.mobile-help-pin {
  position: absolute;
  top: -0.45rem;
  left: 50%;
  transform: translateX(-50%);
  width: 0.9rem;
  height: 0.9rem;
  border-radius: 50%;
  background: #1c7ed6;
  box-shadow:
    inset -2px -2px 3px rgba(0, 0, 0, 0.35),
    inset 2px 2px 3px rgba(255, 255, 255, 0.45),
    0 2px 3px rgba(0, 0, 0, 0.4);
}

.mobile-help-title {
  margin: 0 0 0.85rem;
  font-weight: 800;
  font-size: 1.1rem;
  color: #2f2418;
  text-align: left;
  border-bottom: 2px solid rgba(125, 92, 45, 0.2);
  padding-bottom: 0.4rem;
}

.mobile-help-list {
  margin: 0 0 1.1rem;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0;
  font-size: 0.88rem;
  color: #2f2418;
  line-height: 1.65rem;
}

.mobile-help-list li {
  border-bottom: none;
  padding: 0;
}

.mobile-help-close {
  display: block;
  width: 100%;
  padding: 0.55rem;
  background: rgba(47, 36, 24, 0.08);
  color: #2f2418;
  border: 1px solid rgba(125, 92, 45, 0.3);
  border-radius: 2px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  text-align: center;
  letter-spacing: 0.03em;
}

.mobile-help-close:hover {
  background: rgba(47, 36, 24, 0.14);
}

.image-note {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.55rem;
  width: 100%;
  padding: 0;
  line-height: normal;
}

.student-home {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.75rem 1rem 1rem;
  overflow: hidden;
}

.board-frame {
  container-type: inline-size;
  position: relative;
  width: min(100%, calc((100dvh - 8rem) * 16 / 9));
  aspect-ratio: 16 / 9;
  max-height: calc(100dvh - 8rem);
  margin: 0 auto;
  overflow: hidden;
}

.board-frame::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
  background:
    radial-gradient(ellipse 60% 50% at 28% 18%, rgba(255, 235, 180, 0.18), rgba(255, 235, 180, 0) 70%),
    radial-gradient(ellipse 80% 80% at 50% 110%, rgba(0, 0, 0, 0.35), rgba(0, 0, 0, 0) 60%),
    radial-gradient(ellipse 80% 80% at 50% -10%, rgba(0, 0, 0, 0.25), rgba(0, 0, 0, 0) 60%);
  mix-blend-mode: multiply;
}

.string-layer {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 2;
}

.string-line {
  fill: none;
  stroke-width: 4.5;
  stroke-linecap: round;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
  opacity: 0.9;
  filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.4));
}

.note-slot {
  position: absolute;
  transform: translate(-50%, -50%);
  max-width: 28%;
  z-index: 4;
}

.note-slot :deep(.board-note) {
  padding: 1rem 0.75rem 0.85rem;
  background-image:
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='300' height='300'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.75' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='300' height='300' filter='url(%23n)' opacity='0.09'/%3E%3C/svg%3E"),
    linear-gradient(170deg, #fefdf8 0%, #f8f2e0 40%, #f2e8cc 75%, #ede0bc 100%);
  background-size: 200px 200px, 100% 100%;
}

.note-profile :deep(.board-note) {
  padding: 0.75rem 0.55rem 0.65rem;
}

.note-slot :deep(.board-note-pin) {
  width: 1.2rem;
  height: 1.2rem;
  top: -0.55rem;
}

.note-title {
  margin: 0;
  font-weight: 800;
  font-size: clamp(0.9rem, 1.35cqw, 1.35rem);
  line-height: 1.15;
  text-align: center;
  max-width: 100%;
  color: #2f2418;
  text-wrap: balance;
}

.image-note-bg,
.map-image {
  display: block;
  width: 100%;
  height: auto;
  border-radius: 2px;
  mix-blend-mode: multiply;
}

.leaderboard-image {
  width: 76%;
  margin: 0 auto;
}

.achievements-image {
  display: block;
  width: 74%;
  aspect-ratio: 1 / 1;
  object-fit: contain;
  border-radius: 2px;
  margin: 0 auto;
}

.image-note-title {
  margin: 0;
  width: fit-content;
  min-height: clamp(2.2rem, 4cqw, 3.5rem);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.85rem 0.35rem;
  font-size: clamp(0.9rem, 1.55cqw, 1.5rem);
  font-weight: 800;
  line-height: 1.15;
  text-align: center;
  white-space: normal;
  min-width: 68%;
  max-width: calc(100% - 0.15rem);
  align-self: center;
  color: #2f2418;
  background: linear-gradient(180deg, rgba(255, 252, 237, 0.96), rgba(245, 235, 205, 0.96));
  border-radius: 2px;
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.55) inset,
    0 -1px 0 rgba(130, 97, 48, 0.08) inset;
  border-top: 1px solid rgba(125, 92, 45, 0.12);
  text-wrap: balance;
}

.profile-note {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  text-align: center;
}

.help-note {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  text-align: center;
}

.help-image {
  display: block;
  width: clamp(2.7rem, 4vw, 3.4rem);
  height: auto;
  object-fit: contain;
}

.mystery-image {
  display: block;
  width: 64%;
  aspect-ratio: 1 / 1;
  object-fit: contain;
}

.note-profile {
  top: 17%;
  left: 14%;
}
.note-help {
  top: 52%;
  left: 91%;
}

.help-note--overlay {
  position: absolute;
  transform: translate(-50%, -130%) rotate(-2deg);
  background:
    linear-gradient(180deg, rgba(255, 248, 168, 0.98) 0%, rgba(251, 234, 122, 0.97) 100%);
  padding: 0.7rem 0.85rem 0.8rem;
  box-shadow:
    0 10px 18px rgba(0, 0, 0, 0.28),
    0 2px 5px rgba(0, 0, 0, 0.14);
  border-radius: 0;
  width: 11.25rem;
  pointer-events: none;
  border: 1px solid rgba(167, 133, 28, 0.12);
}

.note-map {
  top: 46%;
  left: 49%;
}
.note-achievements {
  top: 19%;
  left: 78%;
}
.note-notebook {
  top: 82%;
  left: 73%;
}

.note-leaderboard {
  top: 61%;
  left: 11%;
}

.note-mystery {
  top: 84%;
  left: 29%;
}

.help-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 50;
}

.help-note-title {
  margin: 0 0 0.25rem;
  font-weight: 800;
  font-size: 1.5rem;
  color: #5d430d;
}

.help-note-text {
  margin: 0;
  font-size: 1.5rem;
  color: #5d430d;
  line-height: 1.35;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>



