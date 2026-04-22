<script setup lang="ts">
defineProps <{

    title: string
    description: string
    unlocked?: boolean

}>()
</script>

<template>
  <div class="badge-card" :class="{ locked: !unlocked}">
    <div class="badge-icon">
      <img
        :src="unlocked ? '/unlockedBadge.png' : '/lockedBadge.png'" alt="Badge Icon"/>
    </div>
    <div class="badge-content">
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
    </div>
  </div>
</template>

<style scoped>
.badge-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 14px;
  background: white;
  border: 2px solid #d9d9d9;
  min-width: 150px;
  width: calc(50% - 0.5rem);
  box-sizing: border-box;
}

.badge-icon {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* Locked state */
.locked {
  opacity: 0.6;
  filter: grayscale(100%);
}
</style>
