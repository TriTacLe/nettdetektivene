<script setup lang="ts">
import { useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
import BoardNote from '../components/BoardNote.vue'

const corkboardImage = '/corkboard.jpg'
const router = useRouter()

function goTo(route: string) {
  router.push(route)
}
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
            <p class="note-title">Detektiv Kari</p>
          </BoardNote>
        </div>

        <div class="note-slot" style="top: 50%; left: 92%">
          <BoardNote
            :rotation="4"
            pin-color="#1c7ed6"
            width="7rem"
            @click="goTo('/help')"
          >
            <p class="note-title">Hjelp</p>
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
          <BoardNote
            :rotation="5"
            pin-color="#f59f00"
            width="14rem"
            @click="goTo('/achievements')"
          >
            <p class="note-title">Prestasjoner</p>
          </BoardNote>
        </div>

        <div class="note-slot" style="top: 85%; left: 86%">
          <BoardNote
            :rotation="-4"
            pin-color="#7048e8"
            width="14rem"
            @click="goTo('/notepad')"
          >
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
          <BoardNote
            :rotation="-1"
            pin-color="#e03131"
            width="16rem"
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
</style>
