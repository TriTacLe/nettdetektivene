<script setup lang="ts">
import Toast from '@/components/Toast.vue'
import { useToast } from '@/composables/UseToast'

const { toast } = useToast()
</script>

<template>
  <RouterView />
  <Toast v-bind="toast" />
</template>

<style scoped></style>
