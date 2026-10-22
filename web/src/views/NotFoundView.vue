<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <p class="code display">404</p>
    <h1 class="display">这件闲置已经不在货架上了</h1>
    <p class="muted">页面可能被下架、删除，或者链接打错了一个字。</p>
    <div class="row" style="gap: 10px; margin-top: 24px">
      <el-button type="primary" round @click="router.push({ name: 'home' })">回到首页</el-button>
      <el-button round plain @click="router.push({ name: 'search' })">去逛市场</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 40px 24px;
  text-align: center;
  background:
    radial-gradient(700px 400px at 50% 0%, rgba(20, 107, 82, 0.08), transparent 65%),
    var(--paper);
}

.code {
  font-size: clamp(84px, 18vw, 150px);
  line-height: 1;
  color: var(--brand);
  opacity: 0.22;
}

h1 {
  margin-top: 8px;
  font-size: clamp(22px, 3.4vw, 30px);
}
</style>
