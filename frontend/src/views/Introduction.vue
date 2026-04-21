<script setup lang="ts">

import SceneComponent from "@/components/SceneComponent.vue";
</script>

<template>
  <SceneComponent title="Oppgave informasjon" previous="/join-classroom" next="/student-home">
    <div class="introduction">
      <h1>Velkommen</h1>
      <h4>
        Oppdraget ditt er å beskytte internettbyen
        ved å bekjempe data-tyven som prøver å lure deg gjennom falske nyheter og bilder,
        stjeler passord og sender løgner på e-post.
        Du skal løse oppgaver,
        se etter spor og tenke for å løse saken og bekjempe data-tyven.
      </h4>
    </div>

  </SceneComponent>
</template>

<style scoped>
.introduction {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 1rem;
  padding: 2rem;
}

.introduction h1 {
  font-size: 2.5rem;
}
.introduction h4 {
  font-size: 1.25rem;
  max-width: 600px;
}
</style>
