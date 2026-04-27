<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onBeforeUnmount, watch } from 'vue'
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
const boardFrame = ref<HTMLElement | null>(null)
const stringPaths = ref<string[]>([])
let boardResizeObserver: ResizeObserver | null = null
let boardMutationObserver: MutationObserver | null = null

const initialPinCenters = {
  'note-profile': { x: 16, y: 9.5 },
  'note-map': { x: 50, y: 34 },
  'note-achievements': { x: 81, y: 13 },
  'note-help': { x: 94, y: 42.5 },
  'note-notebook': { x: 84, y: 74.5 },
  'note-leaderboard': { x: 16, y: 67.5 },
  'note-mystery': { x: 52, y: 84.5 },
} as const

const noteOrder = [
  'note-profile',
  'note-map',
  'note-achievements',
  'note-help',
  'note-notebook',
  'note-leaderboard',
  'note-mystery',
] as const

// Fetch profile data from the same source as StudentProfileView.
const { data: profileData } = useQuery(getProfileOptions())
const profile = computed(() => profileData.value?.data ?? null)
const avatarCfg = computed(() => parseAvatarConfig(profile.value?.avatarConfig ?? null))
const studentName = computed(() => {
  return profile.value?.displayName || 'Detektiv'
})

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

function buildPathsFromPinCenters(
  pinCenters: Array<{ x: number; y: number }>,
) {
  const nextPaths: string[] = []
  for (let index = 0; index < pinCenters.length - 1; index += 1) {
    const start = pinCenters[index]
    const end = pinCenters[index + 1]
    if (!start || !end) continue
    nextPaths.push(buildSaggingPath(start, end))
  }
  return nextPaths
}

function setInitialStringPaths() {
  stringPaths.value = buildPathsFromPinCenters(
    noteOrder.map((className) => initialPinCenters[className]),
  )
}

async function updateStringPaths() {
  await nextTick()

  const board = boardFrame.value
  if (!board) return

  const boardRect = board.getBoundingClientRect()
  if (!boardRect.width || !boardRect.height) {
    setInitialStringPaths()
    return
  }

  const pinCenters = noteOrder
    .map((className) => {
      const pin = board.querySelector(`.${className} .board-note-pin`) as HTMLElement | null
      if (!pin) return initialPinCenters[className]

      const pinRect = pin.getBoundingClientRect()
      return {
        x: ((pinRect.left + pinRect.width / 2 - boardRect.left) / boardRect.width) * 100,
        y: ((pinRect.top + pinRect.height / 2 - boardRect.top) / boardRect.height) * 100,
      }
    })
    .filter((point): point is { x: number; y: number } => point !== null)

  stringPaths.value = buildPathsFromPinCenters(pinCenters)
}

async function goToNotebook() {
  if (!classroomId.value) {
    router.push({ name: 'notebook' })
    return
  }

  router.push({ name: 'notebook', query: { classroomId: classroomId.value } })
}

onMounted(async () => {
  setInitialStringPaths()

  updateStringPaths()

  try {
    const classrooms = await ClassroomService.getMyClassrooms()
    classroomId.value = classrooms[0]?.id ?? null
  } catch {
    classroomId.value = null
  }
  window.addEventListener('resize', updateStringPaths)

  if (boardFrame.value) {
    boardResizeObserver = new ResizeObserver(() => {
      updateStringPaths()
    })
    boardResizeObserver.observe(boardFrame.value)

    boardMutationObserver = new MutationObserver(() => {
      updateStringPaths()
    })
    boardMutationObserver.observe(boardFrame.value, {
      subtree: true,
      childList: true,
      attributes: true,
      attributeFilter: ['class', 'style'],
    })
  }
})

watch(studentName, () => {
  updateStringPaths()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateStringPaths)
  boardResizeObserver?.disconnect()
  boardMutationObserver?.disconnect()
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

        <div class="note-slot note-profile">
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

        <div class="note-slot note-help">
          <BoardNote
            :rotation="4"
            pin-color="#1c7ed6"
            width="clamp(4rem, 6vw, 6rem)"
            @click="goTo('/help')"
          >
            <div class="help-note">
              <img :src="questionmarkImage" alt="Hjelp" class="help-image" />
              <p class="note-title">Hjelp</p>
            </div>
          </BoardNote>
        </div>

        <div class="note-slot note-map">
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

        <div class="note-slot note-achievements">
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

        <div class="note-slot note-notebook">
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

        <div class="note-slot note-leaderboard">
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

        <div class="note-slot note-mystery">
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

</style>
