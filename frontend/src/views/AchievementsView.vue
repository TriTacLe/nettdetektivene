<script setup lang="ts">
import SceneComponent from '@/components/SceneComponent.vue'
import BadgeCard from '@/components/BadgeCard.vue'

import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import {
  getProfileOptions,
  getAllAchievementsOptions,
  getMyAchievementsOptions,
} from '../generated-client/@tanstack/vue-query.gen'

const { data: profileData, isLoading: isLoadingProfile, isError: isErrorProfile } = useQuery(getProfileOptions())
const { data: allAchivementsData, isLoading: isLoadingAll, isError: isErrorAll } = useQuery(getAllAchievementsOptions())
const { data: myAchievementsData, isLoading: isLoadingMine, isError: isErrorMine } = useQuery(getMyAchievementsOptions())

const isLoading = computed(() => isLoadingProfile.value || isLoadingAll.value || isLoadingMine.value)
const isError = computed(() => isErrorProfile.value || isErrorAll.value || isErrorMine.value)

const profile = computed(() => profileData.value?.data)
const allAchievements = computed(() => allAchivementsData.value?.data ?? [])
const earnedIds = computed(
  () => new Set(myAchievementsData.value?.data?.map((a) => a.achievementId)),
)

const earned = computed(() => earnedIds.value.size)
const total = computed(() => allAchievements.value.length)
const progressPercent = computed(() =>
  total.value ? Math.round((earned.value / total.value) * 100) : 0,
)

const earnedAchievements = computed(() =>
  allAchievements.value.filter((a) => a.id && earnedIds.value.has(a.id))
)

const notEarnedAchievements = computed(() =>
  allAchievements.value.filter((a) => a.id && !earnedIds.value.has(a.id))
)
</script>

<template>
  <SceneComponent title="Resultater" previous="/student-home" background="/trophy.jpg">
    <div class="achievements-container">
      <div class="achievements-card">
        <h1>Mine resultater</h1>

        <!-- Loading state -->
        <div v-if="isLoading" class="loading-state">
          <p>Laster medaljer...</p>
        </div>

        <!-- Error state -->
        <div v-else-if="isError" class="error-state">
          <p>Kunne ikke laste medaljer. Prøv igjen senere.</p>
        </div>

        <!-- Content -->
        <template v-else>
          <!-- Bruker info -->
          <section class="section">
            <div class="profile-info">
              <p><strong>Detektiv:</strong> {{ profile?.displayName }}</p>
              <p><strong>Level:</strong> {{ profile?.xpLevel }}</p>
              <p><strong>Exp:</strong> {{ profile?.xp }}</p>
            </div>
          </section>

          <!-- total fremgang -->
          <section class="section">
            <h2>Fullførte oppgaver</h2>
            <div class="total-progress">
              <div class="progress-info">
                <p><strong>Fullført:</strong> {{ earned }} / {{ total }}</p>
              </div>

              <div class="progress-bar-container">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
                </div>
                <div class="progress-percent">{{ progressPercent }}%</div>
              </div>
            </div>
          </section>

          <!-- Opna medaljer -->
          <section class="section" v-if="earnedAchievements.length > 0">
            <h2>Mine medaljer</h2>
            <div class="badges-container">
              <BadgeCard
                v-for="achievement in earnedAchievements"
                :key="achievement.id"
                :title="achievement.title ?? ''"
                :description="achievement.descAch ?? ''"
                :unlocked="true"
              />
            </div>
          </section>

          <!-- Låste medaljer -->
          <section class="section" v-if="notEarnedAchievements.length > 0">
            <h2>Låste medaljer</h2>
            <div class="badges-container locked-container">
              <BadgeCard
                v-for="achievement in notEarnedAchievements"
                :key="achievement.id"
                :title="achievement.title ?? ''"
                :description="achievement.descAch ?? ''"
                :unlocked="false"
              />
            </div>
          </section>

          <!-- Ingen medaljer -->
          <section class="section" v-if="allAchievements.length === 0">
            <div class="empty-state">
              <p>Ingen medaljer tilgjengelig.</p>
            </div>
          </section>
        </template>
      </div>
    </div>
  </SceneComponent>
</template>

<style scoped>
.achievements-container {
  display: flex;
  justify-content: center;
  padding: 1rem;
  flex: 1;
  min-height: 0;
}

.achievements-card {
  width: 100%;
  max-width: 900px;
  background: white;
  border-radius: 16px;
  padding: 2rem;
  overflow-y: auto;
  max-height: 100%;
}

.achievements-card h1 {
  text-align: center;
  margin-bottom: 0.75rem;
}

.section {
  margin-bottom: 0.75rem;
}

.section h2 {
  margin-bottom: 0.75rem;
}

.badges-container {
  min-height: 100px;
  border: 1px dashed #000000;
  border-radius: 12px;
  padding: 0.75rem;
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
}

.locked-container {
  border-color: #999;
  background-color: #f5f5f5;
  opacity: 0.8;
}

.profile-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;

  border: 1px solid #ddd;
  border-radius: 12px;
  background-color: #f8f8f8;
}

.profile-info p {
  margin: 0.5rem 0;
}

.total-progress {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 2rem;

  border: 1px solid #ddd;
  border-radius: 12px;
  padding: 1rem;
  background-color: #f8f8f8;
}

.progress-info {
  white-space: nowrap;
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
}

.progress-bar {
  flex: 1;
  height: 20px;
  background-color: #ddd;
  border-radius: 10px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #4caf50;
  transition: width 0.3s ease;
}

.progress-percent {
  min-width: 40px;
  text-align: right;
  font-weight: bold;
}

.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.error-state {
  color: #e03131;
}

.achievements-card::-webkit-scrollbar-track {
  background: transparent;
  margin-block: 12px;
}
.achievements-card::-webkit-scrollbar {
  width: 10px;
}
.achievements-card::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 999px;
}
</style>
