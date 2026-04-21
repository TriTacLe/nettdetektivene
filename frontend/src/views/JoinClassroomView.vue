<script setup lang="ts">
import { ref } from 'vue'
import SceneComponent from '../components/SceneComponent.vue'

const classroomCode = ref('')

const handleJoin = () => {
  console.log('Joining classroom with code:', classroomCode.value)
}
</script>

<template>
  <SceneComponent title="Bli med i klasserom" previous="/student-login" next="/introduction">
    <div class="join-form">
      <input v-model="classroomCode" type="text" placeholder="Klasseromskode" />
      <button @click="handleJoin">Bli med</button>
    </div>
  </SceneComponent>
</template>

<style scoped>
.join-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 2rem;
}
</style>
