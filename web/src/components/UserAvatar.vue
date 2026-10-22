<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    name?: string | null
    src?: string | null
    size?: number
  }>(),
  { name: '同', src: null, size: 36 },
)

const initial = computed(() => (props.name ?? '同').trim().slice(0, 1) || '同')
const broken = computed(() => !props.src)
</script>

<template>
  <span
    class="avatar"
    :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${Math.max(size * 0.42, 11)}px` }"
  >
    <img v-if="!broken" :src="src ?? ''" :alt="name ?? '用户头像'" @error="($event.target as HTMLImageElement).style.display = 'none'" />
    <span v-else>{{ initial }}</span>
  </span>
</template>

<style scoped>
.avatar {
  position: relative;
  display: inline-grid;
  place-items: center;
  flex: 0 0 auto;
  overflow: hidden;
  border-radius: 50%;
  background: linear-gradient(135deg, #146b52, #2f8f6b);
  color: #f6f2ea;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
