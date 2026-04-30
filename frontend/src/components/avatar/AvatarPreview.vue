<script setup lang="ts">
import { computed } from 'vue'
import { createAvatar } from '@dicebear/core'
import { avataaars } from '@dicebear/collection'
import type { AvatarConfig } from '@/types/avatar'

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
}>(), {
  size: 160,
})

const avatarSvg = computed(() => {
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
  return avatar.toString()
})

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
    <div v-html="avatarSvg" class="w-full h-full" />
  </div>
</template>
