<script setup lang="ts">
defineProps<{
  title: string
  description: string
  unlocked?: boolean
}>()
</script>

<template>
  <div class="badge-card" :class="{ locked: !unlocked }">
    <div class="badge-icon">
      <img
        :src="unlocked ? '/unlockedBadge.png' : '/lockedBadge.png'"
        :alt="unlocked ? 'Ulåst medalje' : 'Låst medalje'"
      />
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
  gap: 0.85rem;
  padding: 0.85rem 1rem;
  border-radius: 12px;
  background: white;
  border: 1.5px solid #e0d5c0;
  box-sizing: border-box;
  width: 100%;
}

.badge-icon {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.badge-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.badge-content {
  min-width: 0;
  flex: 1;
}

.badge-content h3 {
  font-size: 0.88rem;
  font-weight: 700;
  margin: 0 0 0.2rem;
  color: #1a1a1a;
  -webkit-text-stroke: 0;
  paint-order: normal;
  text-shadow: none;
}

.badge-content p {
  font-size: 0.82rem;
  margin: 0;
  color: #555;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Låst — ingen opacity-stacking, bare visuell dempning */
.locked {
  background: #f0ece4;
  border-color: #ccc5b5;
}

.locked .badge-icon img {
  filter: grayscale(100%) opacity(0.55);
}

.locked .badge-content h3 {
  color: #666;
}

.locked .badge-content p {
  color: #5f5f5f;
}
</style>
