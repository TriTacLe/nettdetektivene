<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { AvatarConfig } from '@/types/avatar'
import { useAvatarLoader, loadDicebear } from '@/composables/UseAvatarGenerator'

const CLOTHES_COLORS: Record<string, string> = {
  hoodie: '1a2744',
  shirtCrewNeck: 'c4841d',
  blazerAndShirt: '1b3a4b',
  collarAndSweater: '6b2fa0',
}

const HAT_COLORS: Record<string, string> = {
  hat: '2d3e50',
  hijab: 'd46a3a',
  winterHat03: '3b7a57',
}

const props = withDefaults(defineProps<{
  config: AvatarConfig
  size?: number
  lazyLoad?: boolean
}>(), {
  size: 160,
  lazyLoad: true
})

const { isLoading, isLoaded, load } = useAvatarLoader()
const avatarSvg = ref<string>('')
const shouldLoadDicebear = ref(!props.lazyLoad)

onMounted(async () => {
  if (shouldLoadDicebear.value) {
    await load()
    generateAvatar()
  }
})

async function generateAvatar() {
  const { createAvatar, avataaars } = await loadDicebear()
  
  const topValue = props.config.hat !== 'none' ? props.config.hat : props.config.hair
  const options: Record<string, unknown> = {
    seed: 'Christian',
    top: [topValue],
    clothing: [props.config.clothing],
    skinColor: [props.config.skinColor],
    hairColor: [props.config.hairColor],
    clothesColor: [CLOTHES_COLORS[props.config.clothing] ?? '65c9ff'],
    topProbability: 100,
    facialHairProbability: 0,
  }

  if (props.config.prop !== 'none') {
    options.accessories = [props.config.prop]
    options.accessoriesProbability = 100
  } else {
    options.accessoriesProbability = 0
  }

  if (props.config.hat !== 'none' && HAT_COLORS[props.config.hat]) {
    options.hatColor = [HAT_COLORS[props.config.hat]]
  }

  const avatar = createAvatar(avataaars, options)
  avatarSvg.value = avatar.toString()
}

const ariaLabel = computed(() => {
  const propText = props.config.prop !== 'none' ? ` med ${props.config.prop}` : ''
  return `Avatar${propText}`
})
</script>

<template>
  <div
    class="avatar-preview relative inline-block"
    :style="{ width: size + 'px', height: size + 'px' }"
    role="img"
    :aria-label="ariaLabel"
  >
    <div v-if="isLoading" class="w-full h-full flex items-center justify-center bg-gray-100 rounded-full animate-pulse" />
    <div v-else-if="isLoaded && avatarSvg" v-html="avatarSvg" class="w-full h-full" />
    <div v-else @click="shouldLoadDicebear = true; load(); generateAvatar()" class="w-full h-full flex items-center justify-center bg-gray-100 rounded-full cursor-pointer hover:bg-gray-200 transition-colors">
      <span class="text-xs text-gray-500">Klikk for å laste avatar</span>
    </div>
  </div>
</template>
