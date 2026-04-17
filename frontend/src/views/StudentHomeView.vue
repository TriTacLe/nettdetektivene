<script setup lang="ts">
import SceneComponent from '../components/SceneComponent.vue'

import { useRouter } from 'vue-router'

const corkboardImage = '/corkboard.jpg'
const router = useRouter()

const notes = [
  { id: 1, label: 'Profil', top: '20%', left: '10%', route: '/profile' },
  { id: 2, label: 'Ledertavle', top: '60%', left: '10%', route: '/leaderboard' },
  { id: 3, label: 'Hjelp', top: '20%', left: '40%', route: '/help' },
  { id: 4, label: 'Kart', top: '50%', left: '50%', route: '/game-map' },
  { id: 5, label: 'Ukens Mysterium', top: '80%', left: '40%', route: '/mystery' },
  { id: 6, label: 'Medaljer', top: '20%', left: '80%', route: '/medals' },
  { id: 7, label: 'Notatblokk', top: '50%', left: '80%', route: '/notepad' },
  { id: 8, label: 'Innstillinger', top: '80%', left: '90%', route: '/settings' },
]

function goTo(route: string) {
  router.push(route)
}

</script>



<template>
  <SceneComponent title="Tavle">
    <div class="student-home">
      <div class="board-frame">
        <img :src="corkboardImage" alt="Korktavle" class="corkboard-image" />
        <button
          v-for="note in notes"
          :key="note.id"
          class="note-button"
          :style="{ top: note.top, left: note.left }"
          @click="goTo(note.route)"
        >
          {{ note.label }}
        </button>
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
  flex: 1;
  width: 100%;
  min-height: 0;
  overflow: hidden;
  border-radius: 1rem;
  display: flex;
  align-items: stretch;
  justify-content: stretch;
}

.corkboard-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;

}

.note-button {
  position: absolute;
  transform: translate(-50%, -50%) rotate(-5deg);
  padding: 0.8rem 1rem;
  background: rgba(218, 192, 127, 0.8);
  border: none;
  border-radius: 0.35rem;
  font-weight: bold;
}


</style>
