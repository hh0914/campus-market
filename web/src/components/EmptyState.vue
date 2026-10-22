<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    hint?: string
    icon?: string
  }>(),
  { title: '这里还空着', hint: '', icon: '🌱' },
)
</script>

<template>
  <div class="empty-box">
    <div class="glyph">{{ icon }}</div>
    <p class="title">{{ title }}</p>
    <p v-if="hint" class="hint">{{ hint }}</p>
    <slot />
  </div>
</template>

<style scoped>
.glyph {
  font-size: 34px;
  line-height: 1;
  margin-bottom: 12px;
}

.title {
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--ink);
}

.hint {
  margin-top: 6px;
  font-size: 13px;
  color: var(--muted);
}
</style>
