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
const myAchievements = computed(() => myAchievementsData.value?.data ?? [])

const earnedById = computed(() => {
  const map = new Map<string, string>()
  for (const m of myAchievements.value) {
    if (m.achievementId) map.set(m.achievementId, m.earnedAt ?? '')
  }
  return map
})

const earned = computed(() => earnedById.value.size)
const total = computed(() => allAchievements.value.length)
const progressPercent = computed(() =>
  total.value ? Math.round((earned.value / total.value) * 100) : 0,
)

const earnedAchievements = computed(() =>
  allAchievements.value.filter((a) => a.id && earnedById.value.has(a.id)),
)

const notEarnedAchievements = computed(() =>
  allAchievements.value
    .filter((a) => a.id && !earnedById.value.has(a.id))
    .map((a) =>
      a.triggerType === 'secret'
        ? { ...a, title: 'Skjult medalje', descAch: 'Utforsk for å avsløre denne medaljen.' }
        : a,
    ),
)
</script>

<template>
  <SceneComponent title="Medaljer" previous="/student-home" background="/red carpet.jpeg" class="scene--fit">
    <div class="achievements">
      <div v-if="isLoading" class="state-msg" role="status">Laster medaljer...</div>
      <div v-else-if="isError" class="state-msg state-msg--error" role="alert">
        Kunne ikke laste medaljer. Prøv igjen senere.
      </div>

      <template v-else>
        <header class="hero">
          <p class="hero-eyebrow">Detektivkofferten</p>
          <h1 class="hero-title">Mine medaljer</h1>
          <p class="hero-subtitle">
            <span class="hero-name">{{ profile?.displayName ?? 'Detektiv' }}</span>
            <span class="hero-divider" aria-hidden="true">/</span>
            <span class="hero-level">Nivå {{ profile?.xpLevel ?? 1 }}</span>
            <span class="hero-divider" aria-hidden="true">/</span>
            <span class="hero-xp">{{ profile?.xp ?? 0 }} XP</span>
          </p>

          <div class="progress-block">
            <div class="progress-stats">
              <span class="progress-count">
                <strong>{{ earned }}</strong> / {{ total }} medaljer
              </span>
              <span class="progress-percent">{{ progressPercent }}%</span>
            </div>
            <div
              class="progress-track"
              role="progressbar"
              :aria-valuenow="progressPercent"
              aria-valuemin="0"
              aria-valuemax="100"
              :aria-label="`Du har oppnådd ${earned} av ${total} medaljer`"
            >
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
          </div>
        </header>

        <div class="scroll-pane" tabindex="0" aria-label="Liste over medaljer">
          <section v-if="earnedAchievements.length > 0" class="medal-section" aria-labelledby="earned-heading">
            <h2 id="earned-heading" class="section-heading">
              <span class="section-marker section-marker--earned" aria-hidden="true" />
              Oppnådd
              <span class="section-count">{{ earnedAchievements.length }}</span>
            </h2>
            <ul class="medal-list">
              <BadgeCard
                v-for="achievement in earnedAchievements"
                :key="achievement.id"
                :title="achievement.title ?? ''"
                :description="achievement.descAch ?? ''"
                :icon="achievement.icon"
                :earned-at="earnedById.get(achievement.id ?? '')"
                :unlocked="true"
              />
            </ul>
          </section>

          <section v-if="notEarnedAchievements.length > 0" class="medal-section" aria-labelledby="locked-heading">
            <h2 id="locked-heading" class="section-heading">
              <span class="section-marker section-marker--locked" aria-hidden="true" />
              Låst
              <span class="section-count">{{ notEarnedAchievements.length }}</span>
            </h2>
            <ul class="medal-list">
              <BadgeCard
                v-for="achievement in notEarnedAchievements"
                :key="achievement.id"
                :title="achievement.title ?? ''"
                :description="achievement.descAch ?? ''"
                :icon="achievement.icon"
                :unlocked="false"
              />
            </ul>
          </section>

          <p v-if="allAchievements.length === 0" class="state-msg">
            Ingen medaljer tilgjengelig.
          </p>
        </div>
      </template>
    </div>
  </SceneComponent>
</template>

<style scoped>
.achievements {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 100%;
  max-width: 820px;
  margin: 0 auto;
  padding: 0.4rem 1.25rem 2.5rem;
  min-height: 0;
}

.scroll-pane {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0.25rem 0.6rem 0.75rem 0.4rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  scrollbar-gutter: stable;
}

.scroll-pane:focus-visible {
  outline: 2px solid var(--color-nd-glow, #f0c040);
  outline-offset: 2px;
  border-radius: 8px;
}

.scroll-pane::-webkit-scrollbar {
  width: 10px;
}

.scroll-pane::-webkit-scrollbar-track {
  background: transparent;
}

.scroll-pane::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.25);
  border-radius: 999px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.scroll-pane::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.4);
  background-clip: padding-box;
}

.state-msg {
  margin: 4rem auto;
  font-size: 1.1rem;
  font-weight: 700;
  color: #ffffff;
  text-align: center;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.85);
}

.state-msg--error {
  color: #fca5a5;
}

.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.2rem;
  padding: 0.4rem 0 0.4rem;
  flex-shrink: 0;
}

.hero-eyebrow {
  margin: 0;
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: var(--color-nd-glow, #f0c040);
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.9);
}

.hero-title {
  margin: 0;
  font-size: clamp(1.4rem, 3vw, 2rem);
  font-weight: 900;
  letter-spacing: 0.02em;
  color: #ffffff;
  text-transform: uppercase;
}

.hero-subtitle {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin: 0.1rem 0 0;
  font-size: 0.85rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.85);
}

.hero-divider {
  color: rgba(255, 255, 255, 0.4);
  font-weight: 400;
}

.hero-level,
.hero-xp {
  letter-spacing: 0.04em;
}

.progress-block {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  width: 100%;
  max-width: 480px;
  margin-top: 0.45rem;
}

.progress-stats {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  font-size: 0.82rem;
  font-weight: 700;
  color: #ffffff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.85);
  letter-spacing: 0.04em;
}

.progress-count strong {
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--color-nd-glow, #f0c040);
  letter-spacing: 0.02em;
  margin-right: 0.15rem;
}

.progress-percent {
  font-size: 0.9rem;
  font-weight: 800;
  color: var(--color-nd-glow, #f0c040);
}

.progress-track {
  position: relative;
  height: 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.7);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(
    90deg,
    var(--color-nd-glow, #f0c040) 0%,
    #ffd76a 50%,
    var(--color-nd-glow, #f0c040) 100%
  );
  box-shadow: 0 0 14px color-mix(in srgb, var(--color-nd-glow, #f0c040) 75%, transparent);
  transition: width 0.5s ease;
  border-radius: 999px;
}

.medal-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0;
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #ffffff;
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.9);
  -webkit-text-stroke: 0;
}

.section-marker {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.section-marker--earned {
  background: var(--color-nd-glow, #f0c040);
  box-shadow: 0 0 10px color-mix(in srgb, var(--color-nd-glow, #f0c040) 80%, transparent);
}

.section-marker--locked {
  background: #5a5a68;
}

.section-count {
  margin-left: 0.35rem;
  padding: 0.15rem 0.55rem;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.92);
}

.medal-list {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin: 0;
  padding: 0;
}

@media (max-width: 640px) {
  .achievements {
    padding: 1rem 1rem 1rem;
    gap: 1rem;
  }

  .hero {
    padding: 0.75rem 0 0.5rem;
  }
}
</style>

<!-- Lock the scene to the viewport on this page so only the inner medal box scrolls. -->
<style>
.scene.scene--fit {
  height: 100vh;
  min-height: 100vh;
  max-height: 100vh;
  overflow: hidden;
}

.scene.scene--fit > .scene-content {
  min-height: 0;
  height: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.scene.scene--fit > .scene-content > .scene-body {
  flex: 1;
  min-height: 0;
}
</style>
