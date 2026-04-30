<script setup lang="ts">
import type { Component } from 'vue'

export interface SidePanelMenuItem {
  key: string
  title: string
  subtitle?: string
  icon: Component
  badge?: number
}

withDefaults(
  defineProps<{
    items: SidePanelMenuItem[]
    code?: { label: string; value: string } | null
    ariaLabel?: string
  }>(),
  {
    code: null,
    ariaLabel: 'Sidemeny',
  },
)

const emit = defineEmits<{ select: [item: SidePanelMenuItem] }>()

function handleSelect(item: SidePanelMenuItem) {
  emit('select', item)
}
</script>

<template>
  <nav class="side-panel" :aria-label="ariaLabel">
    <div v-if="code" class="code-block">
      <span class="code-label">{{ code.label }}</span>
      <span class="code-value">{{ code.value }}</span>
    </div>
    <h2 class="panel-title">Meny</h2>

    <div
      v-for="(item, i) in items"
      :key="item.key"
      class="side-btn-wrap"
      :style="{ animationDelay: `${0.05 + i * 0.05}s` }"
    >
      <span
        v-if="item.badge && item.badge > 0"
        class="badge"
        :aria-label="`${item.badge} uleste`"
      >{{ item.badge }}</span>
      <button
        class="side-btn"
        :aria-label="item.subtitle ? `${item.title} - ${item.subtitle}` : item.title"
        @click="handleSelect(item)"
      >
        <component :is="item.icon" :size="22" aria-hidden="true" class="side-btn-icon" />
        <span class="side-btn-title">{{ item.title }}</span>
      </button>
    </div>
  </nav>
</template>

<style scoped>
.side-panel {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.6rem;
}

.panel-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.85);
  text-align: center;
  margin-bottom: 0.25rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-shadow: none;
  -webkit-text-stroke: 0;
  paint-order: normal;
  width: 100%;
}

.code-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  margin-bottom: 0.8rem;
  padding: 1rem 1.5rem;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 0.75rem;
  width: 100%;
  animation: slideRight 0.4s ease-out both;
}

.code-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.7);
  text-transform: uppercase;
  letter-spacing: 0.12em;
  -webkit-text-stroke: 0;
  text-shadow: none;
}

.code-value {
  font-size: 2.75rem;
  font-weight: 800;
  color: #ffffff;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  letter-spacing: 0.18em;
  line-height: 1;
  -webkit-text-stroke: 0;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.5);
}

.side-btn-wrap {
  position: relative;
  display: inline-flex;
  animation: slideRight 0.4s ease-out both;
}

.side-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.85rem;
  min-height: 3.25rem;
  width: 100%;
  padding: 1rem 3rem;
  font-size: 1.125rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.side-btn-icon {
  flex-shrink: 0;
}

.side-btn-title {
  display: inline-block;
}

.badge {
  position: absolute;
  top: -0.5rem;
  left: -0.5rem;
  z-index: 2;
  font-size: 0.8125rem;
  font-weight: 700;
  border-radius: 9999px;
  min-width: 1.65rem;
  height: 1.65rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.45rem;
  background: #dc2626;
  color: white;
  box-shadow: 0 1px 4px rgb(0 0 0 / 0.45);
  pointer-events: none;
}

@keyframes slideRight {
  from { opacity: 0; transform: translateX(12px); }
  to { opacity: 1; transform: translateX(0); }
}
</style>
