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
import type { DialogChoice } from '@/types/cutscene'

const corkboardImage = '/corkboard.jpg'
const mapImage = '/map.jpg'
const badgeImage = '/unlockedBadge.png'
const questionmarkImage = '/questionmarkIcon.png'
const notebookImage = '/notebook.jpg'
const leaderboardImage = '/leaderboard.jpg'
const mysteryImage = '/mystery-magnifier.png'

const router = useRouter()
const storyStore = useStoryProgressStore()
const introRunner = useDialogRunner(introductionDialogs, introductionStartId)
const showIntro = ref(false)
const classroomId = ref<string | null>(null)
const showHelp = ref(false)

function onIntroDone() {
  introRunner.advance()
  if (introRunner.isFinished.value) {
    showIntro.value = false
    storyStore.advance(StoryStep.INTRODUCTION)
  }
}

function onIntroChoose(choice: DialogChoice) {
  introRunner.advance(choice)
  if (introRunner.isFinished.value) {
    showIntro.value = false
    storyStore.advance(StoryStep.INTRODUCTION)
  }
}
const boardFrame = ref<HTMLElement | null>(null)
const stringPaths = ref<string[]>([])
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
const stringConnections: Array<[NoteKey, NoteKey]> = [
  ['note-profile', 'note-map'],
  ['note-map', 'note-achievements'],
  ['note-achievements', 'note-notebook'],
  ['note-notebook', 'note-help'],
  ['note-notebook', 'note-mystery'],
  ['note-mystery', 'note-leaderboard'],
  ['note-leaderboard', 'note-map'],
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

function buildPathsFromPinCenters(pinCenters: Array<{ x: number; y: number }>) {
  const paths: string[] = []
  for (let i = 0; i < pinCenters.length - 1; i++) {
    const start = pinCenters[i]
    const end = pinCenters[i + 1]
    if (start && end) paths.push(buildSaggingPath(start, end))
  }
  return paths
}

async function updateStringPaths() {
  await nextTick()

  const board = boardFrame.value
  if (!board) return

  const boardRect = board.getBoundingClientRect()
  if (!boardRect.width || !boardRect.height) return

  const pinCenters = new Map<NoteKey, { x: number; y: number }>()

  noteOrder.forEach((key) => {
    const noteEl = noteRefs[key]
    if (!noteEl) return
    const pin = noteEl.querySelector('.board-note-pin') as HTMLElement | null
    if (!pin) return
    const pinRect = pin.getBoundingClientRect()
    pinCenters.set(key, {
      x: ((pinRect.left + pinRect.width / 2 - boardRect.left) / boardRect.width) * 100,
      y: ((pinRect.top + pinRect.height / 2 - boardRect.top) / boardRect.height) * 100,
    })
  })

  stringPaths.value = stringConnections
  .map(([startKey, endKey]) => {
    const start = pinCenters.get(startKey)
    const end = pinCenters.get(endKey)
    const sag = connectionSagOverrides[`${startKey}->${endKey}`]
    return start && end ? buildSaggingPath(start, end, sag) : null
  })
  .filter((path): path is string => path !== null)
}

async function goToNotebook() {
  if (!classroomId.value) {
    router.push({ name: 'notebook' })
    return
  }
  router.push({ name: 'notebook', query: { classroomId: classroomId.value } })
}

function goToMystery() {
  if (!classroomId.value) return
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
  }

  if (boardFrame.value) {
    boardResizeObserver = new ResizeObserver(updateStringPaths)
    boardResizeObserver.observe(boardFrame.value)
  }

  if (!storyStore.loaded) await storyStore.load()
  if (!storyStore.hasSeen(StoryStep.INTRODUCTION)) {
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
        <img :src="corkboardImage" alt="Korktavle" class="corkboard-image" />
        <svg class="string-layer" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
          <path
            v-for="(path, index) in stringPaths"
            :key="index"
            :d="path"
            class="string-line"
          />
        </svg>

        <div :ref="setNoteRef('note-profile')" class="note-slot note-profile">
          <BoardNote
            :rotation="-5"
            pin-color="#2f9e44"
            width="clamp(7rem, 16cqw, 10rem)"
            @click="goTo('/student-profile')"
          >
            <div class="profile-note">
              <AvatarPreview :config="avatarCfg" :size="84" />
              <p class="image-note-title">{{ studentName }}</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-help')" class="note-slot note-help">
          <BoardNote
            :rotation="4"
            pin-color="#1c7ed6"
            width="clamp(4.5rem, 8cqw, 7rem)"
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
            width="clamp(12rem, 17cqw, 20rem)"
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
            width="clamp(5rem, 10cqw, 8.5rem)"
            @click="goTo('/achievements')"
          >
            <div class="image-note">
              <img :src="badgeImage" alt="Resultater" class="image-note-bg achievements-image">
              <p class="image-note-title">Resultater</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-notebook')" class="note-slot note-notebook">
          <BoardNote
            :rotation="-4"
            pin-color="#7048e8"
            width="clamp(5rem, 13cqw, 10rem)"
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
            width="clamp(5rem, 13cqw, 10rem)"
            @click="goTo('/student-leaderboard')"
          >
            <div class="image-note">
              <img :src="leaderboardImage" alt="Ledertavle" class="image-note-bg">
              <p class="image-note-title">Ledertavle</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-mystery')" class="note-slot note-mystery">
          <BoardNote
            :rotation="-1"
            pin-color="#e03131"
            width="clamp(5rem, 10cqw, 7.85rem)"
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
            <div class="help-note help-note--overlay" style="top: 8%; left: 13%; transform: translate(-50%, 30%) rotate(-4deg)">
              <p class="help-note-title">Detektivprofil</p>
              <p class="help-note-text">Se profilen din, endre avatar og navn</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 52%; left: 50%; transform: translate(-50%, -130%) rotate(1.5deg)">
              <p class="help-note-title">Kartet</p>
              <p class="help-note-text">Utforsk kartet og finn detektivoppdrag</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 5%; left: 86%; transform: translate(-50%, 30%) rotate(3deg)">
              <p class="help-note-title">Resultater</p>
              <p class="help-note-text">Se medaljer og resultater du har oppnådd</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 88%; left: 84%; transform: translate(-50%, -130%) rotate(-2deg)">
              <p class="help-note-title">Notatblokk</p>
              <p class="help-note-text">Skriv og les egne notater fra oppdragene</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 85%; left: 14%; transform: translate(-50%, -130%) rotate(2.5deg)">
              <p class="help-note-title">Ledertavle</p>
              <p class="help-note-text">Se hvilken klasse på skolen din som har flest poeng</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 92%; left: 50%; transform: translate(-50%, -130%) rotate(-1deg)">
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
            <img :src="badgeImage" alt="Resultater" class="mobile-nav-img achievements-image" />
            <p class="image-note-title">Resultater</p>
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
        <BoardNote :rotation="2.5" pin-color="#e03131" width="100%" @click="goTo('/mystery')">
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
            <p class="mobile-help-title">Hjelp</p>
            <ul class="mobile-help-list">
              <li><strong>Spill</strong> — Utforsk kartet og finn detektivoppdrag</li>
              <li><strong>Detektivprofil</strong> — Se profilen din, endre avatar og navn</li>
              <li><strong>Resultater</strong> — Se medaljer og resultater du har oppnådd</li>
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
  background-image: url('/corkboard.jpg');
  background-size: cover;
  background-position: center;
  border: 0.45rem solid #5c3a1e;
  box-shadow:
    0 0 0 2px #3b2512 inset,
    0 12px 32px rgba(0, 0, 0, 0.5),
    0 4px 10px rgba(0, 0, 0, 0.3);
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
}

.mobile-nav-img {
  display: block;
  width: 40px;
  height: 40px;
  object-fit: contain;
  margin: 0 auto;
}

.mobile-notebook-img {
  width: 52px;
  height: 52px;
}

.mobile-help-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.mobile-help-content {
  background: linear-gradient(180deg, #fffce8 0%, #fbeaaa 100%);
  border-radius: 8px;
  padding: 1.5rem;
  max-width: 360px;
  width: 100%;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.45);
}

.mobile-help-title {
  margin: 0 0 1rem;
  font-weight: 800;
  font-size: 1.2rem;
  color: #5d430d;
  text-align: center;
}

.mobile-help-list {
  margin: 0 0 1.25rem;
  padding: 0 0 0 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #5d430d;
  line-height: 1.4;
}

.mobile-help-close {
  display: block;
  width: 100%;
  padding: 0.65rem;
  background: #5d430d;
  color: #fff;
  border-radius: 6px;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  text-align: center;
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
  container-type: size;
  position: relative;
  width: min(100%, calc((100dvh - 8rem) * 16 / 9));
  height: auto;
  aspect-ratio: 16 / 9;
  max-height: calc(100dvh - 8rem);
  margin: 0 auto;
  overflow: hidden;
  border: 0.6rem solid #5c3a1e;
  box-shadow:
    0 0 0 2px #3b2512 inset,
    0 24px 48px rgba(0, 0, 0, 0.45),
    0 8px 16px rgba(0, 0, 0, 0.3);
}

/* Soft directional light from upper-left, plus a corner vignette so the
   board has visible depth. Sits above the cork image, below the notes. */
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

.corkboard-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
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
  stroke: #9b1c1c;
  stroke-width: 2.8;
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
}

.achievements-image {
  display: block;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: contain;
  border-radius: 2px;
}

.image-note-title {
  margin: 0;
  width: fit-content;
  min-height: clamp(2rem, 3.5cqw, 3.1rem);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.85rem 0.35rem;
  font-size: clamp(0.75rem, 1.25cqw, 1.25rem);
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
  top: 20%;
  left: 18%;
}
.note-help {
  top: 46%;
  left: 92%;
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
  width: 10.25rem;
  pointer-events: none;
  border: 1px solid rgba(167, 133, 28, 0.12);
}


.note-map {
  top: 46%;
  left: 50%;
}
.note-achievements {
  top: 20%;
  left: 81%;
}
.note-notebook {
  top: 82%;
  left: 79.5%;
}

.note-leaderboard {
  top: 75%;
  left: 16%;
}

.note-mystery {
  top: 86.5%;
  left: 46.5%;
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
  font-size: 0.95rem;
  color: #5d430d;
}

.help-note-text {
  margin: 0;
  font-size: 0.82rem;
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


