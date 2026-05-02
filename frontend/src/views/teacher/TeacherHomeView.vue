<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import { Bell, Trophy } from 'lucide-vue-next'
import SceneComponent from '@/components/SceneComponent.vue'
import SidePanelMenu, { type SidePanelMenuItem } from '@/components/SidePanelMenu.vue'
import ClassroomListPanel from '@/components/ClassroomListPanel.vue'
import { getMyNotificationsOptions } from '@/generated-client/@tanstack/vue-query.gen'

const router = useRouter()

const { data: notificationsData } = useQuery({ ...getMyNotificationsOptions(), refetchInterval: 5000 })

const unreadCount = computed(() =>
  (notificationsData.value?.data ?? []).filter((n) => !n.isRead).length
)

const menuItems = computed<SidePanelMenuItem[]>(() => [
  {
    key: 'teacher-notifications',
    title: 'Varsler',
    subtitle: 'Se alle varsler',
    icon: Bell,
    badge: unreadCount.value,
  },
  {
    key: 'teacher-leaderboard',
    title: 'Ledertavler',
    subtitle: 'Se klassenes resultater',
    icon: Trophy,
  },
])

function handleSelect(item: SidePanelMenuItem) {
  router.push({ name: item.key })
}
</script>

<template>
  <SceneComponent
    title="Lærerportal"
    background="/StartBackground.jpg"
    dimOverlay
    lockViewport
  >
    <div class="scene-split-layout">
      <div class="scene-split-grid">
        <section class="scene-split-main" aria-label="Klasserom">
          <ClassroomListPanel />
        </section>

        <aside class="scene-split-side">
          <SidePanelMenu
            :items="menuItems"
            aria-label="Lærerportal-navigasjon"
            @select="handleSelect"
          />
        </aside>
      </div>
    </div>
  </SceneComponent>
</template>
