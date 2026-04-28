<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import SceneComponent from '../components/SceneComponent.vue'
import BoardNote from '../components/BoardNote.vue'
import AvatarPreview from '@/components/avatar/AvatarPreview.vue'
import ClassroomService from '@/services/ClassroomService'
import { getProfileOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'
import { parseAvatarConfig } from '@/utils/avatar'

const corkboardImage = '/corkboard.jpg'
const mapImage = '/map.jpg'
const badgeImage = '/unlockedBadge.png'
const questionmarkImage = '/questionmarkIcon.png'
const notebookImage = '/notebook.jpg'
const leaderboardImage = '/leaderboard.jpg'

const router = useRouter()
const classroomId = ref<string | null>(null)
const showHelp = ref(false)
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

function setNoteRef(key: NoteKey) {
  return (el: any) => {
    if (el && el instanceof HTMLElement) noteRefs[key] = el
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

  const pinCenters = noteOrder
    .map((key) => {
      const noteEl = noteRefs[key]
      if (!noteEl) return null
      const pin = noteEl.querySelector('.board-note-pin') as HTMLElement | null
      if (!pin) return null
      const pinRect = pin.getBoundingClientRect()
      return {
        x: ((pinRect.left + pinRect.width / 2 - boardRect.left) / boardRect.width) * 100,
        y: ((pinRect.top + pinRect.height / 2 - boardRect.top) / boardRect.height) * 100,
      }
    })
    .filter((p): p is { x: number; y: number } => p !== null)

  if (pinCenters.length >= 2) {
    stringPaths.value = buildPathsFromPinCenters(pinCenters)
  }
}

async function goToNotebook() {
  if (!classroomId.value) {
    router.push({ name: 'notebook' })
    return
  }
  router.push({ name: 'notebook', query: { classroomId: classroomId.value } })
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
  <SceneComponent title="Tavle">
    <div class="student-home">
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
            width="clamp(8rem, 16vw, 10rem)"
            @click="goTo('/student-profile')"
          >
            <div class="profile-note">
              <AvatarPreview :config="avatarCfg" :size="84" />
              <p class="note-title">{{ studentName }}</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-help')" class="note-slot note-help">
          <BoardNote
            :rotation="4"
            pin-color="#1c7ed6"
            width="clamp(4rem, 6vw, 6rem)"
            @click="toggleHelp"
          >
            <div class="help-note">
              <img :src="questionmarkImage" alt="Hjelp" class="help-image" />
              <p class="note-title">{{ showHelp ? 'Lukk' : 'Hjelp' }}</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-map')" class="note-slot note-map">
          <BoardNote
            :rotation="-2"
            pin-color="#1c7ed6"
            width="clamp(15rem, 15vw, 20rem)"
            @click="goTo('/game-map')"
          >
            <div class="image-note">
              <img :src="mapImage" alt="kart" class="map-image" />
              <p class="image-note-title">Kartet</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-achievements')" class="note-slot note-achievements">
          <BoardNote
            :rotation="5"
            pin-color="#f59f00"
            width="clamp(5.5rem, 9vw, 8.5rem)"
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
            width="clamp(5rem, 12vw, 10rem)"
            @click="goToNotebook"
          >
            <div class="image-note">
              <img :src="notebookImage" alt="Notatblokk" class="image-note-bg">
              <p class="image-note-title">Notatblokk</p>
            </div>
          </BoardNote>
        </div>

        <div :ref="setNoteRef('note-leaderboard')" class="note-slot note-leaderboard">
          <BoardNote
            :rotation="3"
            pin-color="#f59f00"
            width="clamp(6rem, 15vw, 10rem)"
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
            width="auto"
            height="auto"
            @click="goTo('/mystery')"
          >
            <p class="note-title">Ukens Mysterium</p>
          </BoardNote>
        </div>

        <Transition name="fade">
          <div v-if="showHelp" class="help-overlay" @click.self="closeHelp">
            <div class="help-note help-note--overlay" style="top: 12%; left: 13%; transform: translate(-50%, 30%) rotate(-2deg)">
              <p class="help-note-title">Detektivprofil</p>
              <p class="help-note-text">Se profilen din, endre avatar og navn</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 50%; left: 52%">
              <p class="help-note-title">Kartet</p>
              <p class="help-note-text">Utforsk kartet og finn detektivoppdrag ute</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 10%; left: 86%; transform: translate(-50%, 30%) rotate(-2deg)">
              <p class="help-note-title">Prestasjoner</p>
              <p class="help-note-text">Se medaljer og prestasjoner du har oppnådd</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 85%; left: 86%">
              <p class="help-note-title">Notatblokk</p>
              <p class="help-note-text">Skriv og les egne notater fra oppdragene</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 89%; left: 14%">
              <p class="help-note-title">Ledertavle</p>
              <p class="help-note-text">Se hvilken klasse på skolen din som har flest poeng</p>
            </div>
            <div class="help-note help-note--overlay" style="top: 90%; left: 52%">
              <p class="help-note-title">Ukens Mysterium</p>
              <p class="help-note-text">Send inn funn fra nettet og vinn en medalje</p>
            </div>
          </div>
        </Transition>
      </div>
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

.image-note {
  position: relative;
  display: inline-block;
  width: 100%;
  height: auto;
  min-height: 0;
  padding: 0;
  line-height: 0;
  overflow: visible;
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
  position: relative;
  height: 100%;
  aspect-ratio: 16 / 9;
  width: min(100%, calc((100dvh - 8rem) * 16 / 9));
  max-height: 100%;
  margin: 0 auto;
  overflow: hidden;
  border: 0.6rem solid #5c3a1e;
  box-shadow: 0 0 0 2px #3b2512 inset;
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
  z-index: 3;
}

.string-line {
  fill: none;
  stroke: #9b1c1c;
  stroke-width: 0.6;
  stroke-linecap: round;
  opacity: 0.9;
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.35));
}

.note-slot {
  position: absolute;
  transform: translate(-50%, -50%);
  max-width: 28%;
  z-index: 2;
}

.note-title {
  margin: 0;
  font-weight: 800;
  font-size: clamp(1.125rem, 1.2vw, 1.35rem);
  line-height: 1.15;
  text-align: center;
  color: #fff;
  text-shadow:
    -1px -1px 0 #000,
    1px -1px 0 #000,
    -1px 1px 0 #000,
    1px 1px 0 #000,
    0 2px 4px rgba(0, 0, 0, 0.45);
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
  position: absolute;
  left: 50%;
  bottom: clamp(0.2rem, 0.8vw, 0.7rem);
  transform: translateX(-50%);
  z-index: 1;
  margin: 0;
  padding: 0;
  font-size: clamp(1.125rem, 1.2vw, 1.35rem);
  font-weight: 800;
  line-height: 1.15;
  white-space: nowrap;
  color: #fff;
  text-shadow:
    -1px -1px 0 #000,
    1px -1px 0 #000,
    -1px 1px 0 #000,
    1px 1px 0 #000,
    0 2px 4px rgba(0, 0, 0, 0.45);
  background: transparent;
  border: none;
  border-radius: 0;
}

.profile-note {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.note-profile {
  top: 17%;
  left: 16%;
}
.note-help {
  top: 48%;
  left: 94%;
}

.help-note--overlay {
  position: absolute;
  transform: translate(-50%, -130%) rotate(-2deg);
  background: linear-gradient(180deg, #fffdf0 0%, #fdf6d8 100%);
  padding: 0.6rem 0.9rem 0.7rem;
  box-shadow:
    0 6px 12px rgba(0, 0, 0, 0.3),
    0 2px 4px rgba(0, 0, 0, 0.18);
  border-radius: 2px;
  width: 11rem;
  pointer-events: none;
}

.note-map {
  top: 50%;
  left: 50%;
}
.note-achievements {
  top: 20%;
  left: 81%;
}
.note-notebook {
  top: 82%;
  left: 84%;
}

.note-leaderboard {
  top: 75%;
  left: 16%;
}

.note-mystery {
  top: 90%;
  left: 52%;
}


.help-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 50;
}



.help-note-title {
  margin: 0 0 0.2rem;
  font-weight: 700;
  font-size: 0.85rem;
  color: #1a365d;
}

.help-note-text {
  margin: 0;
  font-size: 0.75rem;
  color: #2d3748;
  line-height: 1.3;
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
