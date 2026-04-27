<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import SceneComponent from '../components/SceneComponent.vue'
import BoardNote from '../components/BoardNote.vue'
import ClassroomService from '@/services/ClassroomService'
import { getProfileOptions } from '@/generated-client/@tanstack/vue-query.gen.ts'

const corkboardImage = '/corkboard.jpg'
const router = useRouter()
const classroomId = ref<string | null>(null)
const showHelp = ref(false)

// Fetch profile to get display name (same source as StudentProfileView)
const { data: profileData } = useQuery(getProfileOptions())
const studentName = computed(() => {
  return profileData.value?.data?.displayName || 'Detektiv'
})

function goTo(route: string) {
  router.push(route)
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
  try {
    const classrooms = await ClassroomService.getMyClassrooms()
    classroomId.value = classrooms[0]?.id ?? null
  } catch {
    classroomId.value = null
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <SceneComponent title="Tavle">
    <div class="student-home">
      <div class="board-frame">
        <img :src="corkboardImage" alt="Korktavle" class="corkboard-image" />

        <div class="note-slot" style="top: 12%; left: 13%">
          <BoardNote
            :rotation="-6"
            pin-color="#2f9e44"
            width="13rem"
            @click="goTo('/student-profile')"
          >
            <p class="note-title">{{ studentName }}</p>
          </BoardNote>
        </div>

        <div class="note-slot" style="top: 50%; left: 92%">
          <BoardNote :rotation="4" pin-color="#1c7ed6" width="7rem" @click="toggleHelp">
            <p class="note-title">{{ showHelp ? 'Lukk' : 'Hjelp' }}</p>
          </BoardNote>
        </div>

        <div class="note-slot" style="top: 50%; left: 52%">
          <BoardNote
            :rotation="-2"
            pin-color="#1c7ed6"
            width="28rem"
            height="16rem"
            @click="goTo('/game-map')"
          >
            <div class="map-note">
              <p class="note-title map-title">Kartet</p>
              <p class="map-subtitle">Gå utendørs</p>
            </div>
          </BoardNote>
        </div>

        <div class="note-slot" style="top: 10%; left: 86%">
          <BoardNote :rotation="5" pin-color="#f59f00" width="14rem" @click="goTo('/achievements')">
            <p class="note-title">Prestasjoner</p>
          </BoardNote>
        </div>

        <div class="note-slot" style="top: 85%; left: 86%">
          <BoardNote :rotation="-4" pin-color="#7048e8" width="14rem" @click="goToNotebook">
            <p class="note-title">Notatblokk</p>
          </BoardNote>
        </div>

        <div class="note-slot" style="top: 89%; left: 14%">
          <BoardNote
            :rotation="3"
            pin-color="#f59f00"
            width="14rem"
            @click="goTo('/student-leaderboard')"
          >
            <p class="note-title">Ledertavle</p>
          </BoardNote>
        </div>

        <div class="note-slot" style="top: 90%; left: 52%">
          <BoardNote :rotation="-1" pin-color="#e03131" width="16rem" @click="goTo('/mystery')">
            <p class="note-title">Ukens Mysterium</p>
          </BoardNote>
        </div>

        <Transition name="fade">
          <div v-if="showHelp" class="help-overlay" @click.self="closeHelp">
            <div class="help-note" style="top: 12%; left: 13%; transform: translate(-50%, 30%) rotate(-2deg)">
              <p class="help-note-title">Detektivprofil</p>
              <p class="help-note-text">Se profilen din, endre avatar og navn</p>
            </div>
            <div class="help-note" style="top: 50%; left: 52%">
              <p class="help-note-title">Kartet</p>
              <p class="help-note-text">Utforsk kartet og finn detektivoppdrag ute</p>
            </div>
            <div class="help-note" style="top: 10%; left: 86%; transform: translate(-50%, 30%) rotate(-2deg)">
              <p class="help-note-title">Prestasjoner</p>
              <p class="help-note-text">Se medaljer og prestasjoner du har oppnådd</p>
            </div>
            <div class="help-note" style="top: 85%; left: 86%">
              <p class="help-note-title">Notatblokk</p>
              <p class="help-note-text">Skriv og les egne notater fra oppdragene</p>
            </div>
            <div class="help-note" style="top: 89%; left: 14%">
              <p class="help-note-title">Ledertavle</p>
              <p class="help-note-text">Se hvilken klasse på skolen din som har flest poeng</p>
            </div>
            <div class="help-note" style="top: 90%; left: 52%">
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
.student-home {
  flex: 1;
  min-height: 0;
  display: flex;
  padding: 1.5rem;
}

.board-frame {
  position: relative;
  height: 100%;
  aspect-ratio: 16 / 9;
  max-width: 100%;
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

.note-slot {
  position: absolute;
  transform: translate(-50%, -50%);
}

.note-title {
  margin: 0;
  font-weight: 700;
  font-size: 1.1rem;
  text-align: center;
  color: #1a365d;
}

.map-note {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.map-title {
  font-size: 2rem;
}

.map-subtitle {
  margin: 0;
  color: #4a5568;
  font-size: 1rem;
  font-style: italic;
}

.help-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 50;
}

.help-note {
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
