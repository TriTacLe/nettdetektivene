<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CutsceneDialog from '@/components/CutsceneDialog.vue'
import { mysteryDialogs, mysteryStartId } from '@/data/MysteryDialogs'
import type { DialogChoice } from '@/types/cutscene'

const route = useRoute()
const router = useRouter()

const classroomId = computed(() => String(route.params.classroomId))
const scene = computed(() => mysteryDialogs[mysteryStartId]!)
const startAtLastLine = computed(() => route.query.skip === '1')

function handleChoose(choice: DialogChoice) {
  if (choice.flag === 'play') {
    router.push({
      name: 'student-weekly-mystery-play',
      params: { classroomId: classroomId.value },
    })
  } else if (choice.flag === 'submit') {
    router.push({
      name: 'student-weekly-mystery-submit',
      params: { classroomId: classroomId.value },
    })
  }
}

function handleDone() {
  router.push('/student-home')
}

function handleBack() {
  router.push('/student-home')
}
</script>

<template>
  <CutsceneDialog
    :scene="scene"
    :start-at-last-line="startAtLastLine"
    @choose="handleChoose"
    @done="handleDone"
    @back="handleBack"
  />
</template>
