<script setup lang="ts">
import { useRouter } from 'vue-router'
import SceneComponent from '../components/SceneComponent.vue'
const router = useRouter()

function goToStudent() {
  router.push('/student-login')
}

function goToTeacher() {
  router.push('/teacher-login')
}
</script>

<template>
  <SceneComponent
      title=""
      background="/StartBackground.jpg">

    <div class="start-content">
      <h1>Velkommen til Nettdetektivene!</h1>
      <h4>Velg om du er en elev eller en lærer for å komme i gang.</h4>

      <div class="btn-stack">
        <button class="btn-vn" @click="goToStudent">
          Jeg er en elev
        </button>
        <button class="btn-vn" @click="goToTeacher">
          Jeg er en lærer
        </button>
      </div>
    </div>

  </SceneComponent>
</template>

<style scoped>
.start-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  text-align: center;
  gap: 1.5rem;
  padding: 2rem;
  padding-top: 25vh;
}

.start-content h1 {
  font-size: 2.5rem;
}
.start-content h4 {
  font-size: 1.25rem;
}

.btn-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

</style>
