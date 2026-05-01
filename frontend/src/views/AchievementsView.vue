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
  allAchievements.value
    .filter((a) => a.id && !earnedIds.value.has(a.id))
    .map((a) =>
      a.triggerType === 'secret'
        ? { ...a, title: 'Skjult medalje', descAch: 'Utforsk for å avsløre denne medaljen.' }
        : a,
    ),
)
</script>

<template>
  <SceneComponent title="Resultater" previous="/student-home">
    <div class="achievements-container">
      <div class="achievements-card" tabindex="0" aria-label="Resultater, bruk piltaster for å bla">
        <h2 class="card-title">Mine resultater</h2>

        <!-- Loading state -->
        <div v-if="isLoading" class="loading-state" aria-live="polite">
          <p>Laster medaljer...</p>
        </div>

        <!-- Error state -->
        <div v-else-if="isError" class="error-state" role="alert">
          <p>Kunne ikke laste medaljer. Prøv igjen senere.</p>
        </div>

        <!-- Content -->
        <template v-else>
          <!-- Bruker info -->
          <section class="section" aria-label="Detektivinfo">
            <dl class="profile-info">
              <div class="profile-info-item">
                <dt>Detektiv</dt>
                <dd>{{ profile?.displayName }}</dd>
              </div>
              <div class="profile-info-item">
                <dt>Nivå</dt>
                <dd>{{ profile?.xpLevel }}</dd>
              </div>
              <div class="profile-info-item">
                <dt>XP</dt>
                <dd>{{ profile?.xp }}</dd>
              </div>
            </dl>
          </section>

          <!-- Total fremgang -->
          <section class="section" aria-label="Medaljefremdrift">
            <h2>Medaljer opptjent</h2>
            <div class="total-progress">
              <p class="progress-label">
                <strong>Opptjent:</strong> {{ earned }} / {{ total }}
              </p>
              <div class="progress-bar-container">
                <div
                  class="progress-bar"
                  role="progressbar"
                  :aria-valuenow="progressPercent"
                  aria-valuemin="0"
                  aria-valuemax="100"
                  :aria-label="`${progressPercent}% av medaljene opptjent`"
                >
                  <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
                </div>
                <span class="progress-percent" aria-hidden="true">{{ progressPercent }}%</span>
              </div>
            </div>
          </section>

          <!-- Opptjente medaljer -->
          <section class="section" v-if="earnedAchievements.length > 0" aria-label="Opptjente medaljer">
            <h2>Mine medaljer</h2>
            <ul class="badges-container" role="list" aria-label="Liste over opptjente medaljer">
              <li v-for="achievement in earnedAchievements" :key="achievement.id">
                <BadgeCard
                  :title="achievement.title ?? ''"
                  :description="achievement.descAch ?? ''"
                  :unlocked="true"
                />
              </li>
            </ul>
          </section>

          <!-- Låste medaljer -->
          <section class="section" v-if="notEarnedAchievements.length > 0" aria-label="Låste medaljer">
            <h2>Låste medaljer</h2>
            <ul class="badges-container locked-container" role="list" aria-label="Liste over låste medaljer">
              <li v-for="achievement in notEarnedAchievements" :key="achievement.id">
                <BadgeCard
                  :title="achievement.title ?? ''"
                  :description="achievement.descAch ?? ''"
                  :unlocked="false"
                />
              </li>
            </ul>
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
  background: #fdfaf4;
  border-radius: 16px;
  padding: 2rem;
  overflow-y: auto;
  max-height: 100%;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.18);
}

/* Reset global game heading styles inside the card */
.achievements-card h1,
.achievements-card h2,
.achievements-card h3 {
  -webkit-text-stroke: 0;
  paint-order: normal;
  text-shadow: none;
  color: #1a1a1a;
}

.card-title {
  text-align: center;
  margin-bottom: 1.5rem;
  font-size: 1.6rem;
  color: #1a1a1a;
  border-bottom: 2px solid #e8e0cc;
  padding-bottom: 1rem;
}

.section {
  margin-bottom: 1.5rem;
}

.section h2 {
  margin-bottom: 0.75rem;
  font-size: 1rem;
  font-weight: 700;
  color: #3b2a1a;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* Profil */
.profile-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border: 1px solid #e0d5c0;
  border-radius: 12px;
  background: #fff8ee;
  margin: 0;
}

.profile-info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
}

.profile-info-item dt {
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #7a6a50;
  font-weight: 600;
}

.profile-info-item dd {
  font-size: 1.15rem;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

/* Fremdrift */
.total-progress {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-wrap: wrap;
  border: 1px solid #e0d5c0;
  border-radius: 12px;
  padding: 1rem 1.25rem;
  background: #fff8ee;
}

.progress-label {
  white-space: nowrap;
  margin: 0;
  color: #3b2a1a;
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
  min-width: 140px;
}

.progress-bar {
  flex: 1;
  height: 18px;
  background-color: #e8e0cc;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3a8f4e, #2d7a40);
  border-radius: 999px;
  transition: width 0.4s ease;
}

.progress-percent {
  min-width: 42px;
  text-align: right;
  font-weight: 700;
  color: #3b2a1a;
  font-size: 0.95rem;
}

/* Medaljer */
.badges-container {
  border: 1px solid #e0d5c0;
  border-radius: 12px;
  padding: 0.75rem;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-auto-rows: 5.5rem;
  gap: 0.75rem;
  list-style: none;
  margin: 0;
}

.badges-container li {
  display: flex;
}


.locked-container {
  background-color: #f5f2ec;
  border-color: #ccc5b5;
}

/* States */
.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.error-state {
  color: #c0392b;
  font-weight: 600;
}

/* Scrollbar */
.achievements-card:focus {
  outline: none;
}

.achievements-card:focus-visible {
  outline: 2px solid var(--color-nd-glow);
  outline-offset: 3px;
}

.achievements-card::-webkit-scrollbar-track {
  background: transparent;
  margin-block: 12px;
}
.achievements-card::-webkit-scrollbar {
  width: 8px;
}
.achievements-card::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.15);
  border-radius: 999px;
}

@media (max-width: 500px) {
  .achievements-card {
    padding: 1.25rem 1rem;
  }

  .profile-info {
    justify-content: space-around;
  }

  .total-progress {
    flex-direction: column;
    align-items: stretch;
  }

  .progress-bar-container {
    min-width: 0;
  }

  .badges-container {
    grid-template-columns: 1fr;
  }
}
</style>
