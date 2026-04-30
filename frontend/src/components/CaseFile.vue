<script setup lang="ts">
defineProps<{ tabLabel: string }>()
</script>

<template>
  <div class="case-file">

    <div class="case-tab" aria-hidden="true">
      <span class="case-tab-text">{{ tabLabel }}</span>
    </div>
    <div class="case-body">
      <div class="case-page case-page--left" :class="{ 'case-page--full': !$slots.right }">
        <slot name="left" />
      </div>

      <template v-if="$slots.right">
        <div class="case-fold" aria-hidden="true" />
        <div class="case-page case-page--right">
          <slot name="right" />
        </div>
      </template>
    </div>
    <svg
      class="decor-clip"
      aria-hidden="true"
      viewBox="0 0 20 60"
      fill="none"
    >
      <path
        stroke="#78716c"
        stroke-width="1.5"
        stroke-linecap="round"
      />
    </svg>
    <svg
      class="decor-stamp"
      aria-hidden="true"
      viewBox="0 0 160 40"
      fill="none"
    >
      <rect x="3" y="3" width="154" height="34" rx="4" stroke="#991b1b" stroke-width="2.5" />
      <text
        x="80" y="27"
        text-anchor="middle"
        font-family="'Courier New', monospace"
        font-size="16"
        font-weight="900"
        fill="#991b1b"
        letter-spacing="3"
      >HEMMELIG</text>
    </svg>
  </div>
</template>

<style scoped>

.case-file {
  position: relative;
  padding-top: 2.5rem;
}

.case-tab {
  position: absolute;
  top: 0;
  left: 0;
  height: calc(2.5rem + 2px);
  background: #efe3c8;
  border: 2px solid #44403c;
  border-bottom: none;
  border-radius: 0.5rem 0.5rem 0 0;
  padding: 0.5rem 2rem 0;
  z-index: 2;
}

.case-tab-text {
  font-size: 0.95rem;
  font-weight: 800;
  color: #44403c;
  letter-spacing: 0.15em;
}

.case-body {
  position: relative;
  background: #f5ecd7;
  border: 2px solid #44403c;
  border-radius: 0 0.75rem 0.75rem 0.75rem;
  display: flex;
  flex-direction: column;
  min-height: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

@media (min-width: 1024px) {
  .case-body {
    flex-direction: row;
  }
}

.case-page {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.case-page--full {
  width: 100%;
}

@media (min-width: 1024px) {
  .case-page--left:not(.case-page--full) {
    flex: 2;
  }
  .case-page--right {
    flex: 3;
  }
}

.case-fold {
  flex-shrink: 0;
  border-top: 1.5px dashed rgba(68, 64, 60, 0.25);
  margin: 0 1.5rem;
}

@media (min-width: 1024px) {
  .case-fold {
    border-top: none;
    border-left: 1.5px dashed rgba(68, 64, 60, 0.25);
    margin: 1.5rem 0;
    align-self: stretch;
  }
}

.decor-clip {
  position: absolute;
  top: -0.3rem;
  left: -0.1rem;
  width: 2rem;
  height: 5rem;
  transform: rotate(8deg);
  z-index: 3;
  pointer-events: none;
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.15));
}

.decor-stamp {
  display: none;
  position: absolute;
  top: 3.75rem;
  right: 1rem;
  width: 8rem;
  height: 2rem;
  opacity: 0.4;
  transform: rotate(12deg);
  pointer-events: none;
  z-index: 3;
}

@media (min-width: 1024px) {
  .decor-stamp {
    display: block;
  }
}
</style>
