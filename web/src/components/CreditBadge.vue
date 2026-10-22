<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    score?: number | null
    badge?: string | null
    size?: 'sm' | 'md'
  }>(),
  { score: null, badge: null, size: 'sm' },
)

const BADGE_LABELS: Record<string, string> = {
  ELITE: '精英卖家',
  RELIABLE: '可靠用户',
  NORMAL: '普通用户',
  WATCH: '观察期',
}

const resolvedBadge = computed(() => {
  if (props.badge) {
    return props.badge
  }
  const score = props.score ?? 0
  if (score >= 95) return 'ELITE'
  if (score >= 85) return 'RELIABLE'
  if (score >= 70) return 'NORMAL'
  return 'WATCH'
})

const label = computed(() => BADGE_LABELS[resolvedBadge.value] ?? '普通用户')
</script>

<template>
  <span class="credit-badge" :class="[resolvedBadge.toLowerCase(), size]">
    <span class="dot" />
    {{ label }}
    <strong v-if="score !== null">{{ score }}</strong>
  </span>
</template>

<style scoped>
.credit-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
  white-space: nowrap;
}

.credit-badge.md {
  padding: 4px 12px;
  font-size: 13px;
}

.credit-badge strong {
  font-family: var(--font-num);
  font-variant-numeric: lining-nums tabular-nums;
  font-size: 13px;
}

.credit-badge .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.elite {
  background: #fbf1d6;
  border-color: #ecd9a5;
  color: #8a6314;
}

.reliable {
  background: var(--brand-soft);
  border-color: #c6dfd6;
  color: var(--brand-deep);
}

.normal {
  background: #f0eee6;
  border-color: #e0d9ca;
  color: #5f6b64;
}

.watch {
  background: var(--pop-soft);
  border-color: #f4cdb8;
  color: #a8431a;
}
</style>
