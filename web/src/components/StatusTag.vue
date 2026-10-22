<script setup lang="ts">
import { computed } from 'vue'
import {
  ITEM_STATUS_LABELS,
  ITEM_STATUS_TONE,
  ORDER_STATUS_LABELS,
  ORDER_STATUS_TONE,
  REPORT_STATUS_LABELS,
} from '@/utils/format'

const props = withDefaults(
  defineProps<{
    status: string
    kind?: 'item' | 'order' | 'report'
  }>(),
  { kind: 'item' },
)

const label = computed(() => {
  if (props.kind === 'order') {
    return ORDER_STATUS_LABELS[props.status as keyof typeof ORDER_STATUS_LABELS] ?? props.status
  }
  if (props.kind === 'report') {
    return REPORT_STATUS_LABELS[props.status as keyof typeof REPORT_STATUS_LABELS] ?? props.status
  }
  return ITEM_STATUS_LABELS[props.status as keyof typeof ITEM_STATUS_LABELS] ?? props.status
})

const tone = computed(() => {
  if (props.kind === 'order') {
    return ORDER_STATUS_TONE[props.status as keyof typeof ORDER_STATUS_TONE] ?? 'info'
  }
  if (props.kind === 'report') {
    return props.status === 'RESOLVED' ? 'success' : props.status === 'REJECTED' ? 'info' : props.status === 'PROCESSING' ? 'primary' : 'warning'
  }
  return ITEM_STATUS_TONE[props.status as keyof typeof ITEM_STATUS_TONE] ?? 'info'
})
</script>

<template>
  <el-tag :type="tone" size="small" effect="light" round>{{ label }}</el-tag>
</template>
