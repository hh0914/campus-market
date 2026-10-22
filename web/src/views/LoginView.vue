<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  account: '',
  password: '',
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入学号或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }],
}

const demoAccounts = [
  { label: '学生 · 林晓', account: '2021001' },
  { label: '审核员 · 陈默', account: 'mod001' },
  { label: '管理员 · 张明远', account: 'admin' },
]

function fill(account: string) {
  form.account = account
  form.password = 'Campus@2026'
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    const user = await auth.login({ account: form.account.trim(), password: form.password })
    ElMessage.success(`欢迎回来，${user?.nickname ?? '同学'}`)
    const redirect = route.query.redirect
    if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('/login')) {
      router.replace(redirect)
    } else if (user?.role === 'ADMIN' || user?.role === 'MODERATOR') {
      router.replace({ name: 'admin-dashboard' })
    } else {
      router.replace({ name: 'home' })
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrap">
    <aside class="auth-aside">
      <router-link :to="{ name: 'home' }" class="row aside-brand">
        <span class="aside-mark">集</span>
        <span>
          <strong>校园集</strong>
          <small>CAMPUS MARKET</small>
        </span>
      </router-link>

      <div>
        <p class="quote">把宿舍里的闲置，交给下一个需要它的同学。</p>
        <div class="points">
          <div><span>🔐</span> 学生身份认证，交易对象来自同校</div>
          <div><span>🛡️</span> 资金托管，确认收货后才结算</div>
          <div><span>⭐</span> 双向评价，信用分决定你的曝光</div>
        </div>
      </div>

      <p class="tiny aside-foot">© 2026 校园集 · 校园二手交易与信用评价平台</p>
    </aside>

    <main class="auth-main">
      <div class="auth-card">
        <p class="pill">学号登录</p>
        <h2 class="display">登录校园集</h2>
        <p class="muted small" style="margin-top: 8px">用学号或校内邮箱登录，继续你的校园交易。</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" style="margin-top: 28px" @submit.prevent="submit">
          <el-form-item label="学号 / 邮箱" prop="account">
            <el-input v-model="form.account" placeholder="例如 2021001" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">登录</el-button>
        </el-form>

        <div class="row between" style="margin-top: 16px">
          <span class="small muted">还没有账号？</span>
          <router-link :to="{ name: 'register' }" class="brand-text small" style="font-weight: 600">注册新同学 →</router-link>
        </div>

        <div class="auth-demo">
          <strong>演示账号（密码统一 Campus@2026）</strong>
          <div class="row wrap" style="gap: 8px; margin-top: 8px">
            <el-button v-for="item in demoAccounts" :key="item.account" size="small" plain round @click="fill(item.account)">
              {{ item.label }}
            </el-button>
          </div>
        </div>

        <router-link :to="{ name: 'home' }" class="small muted back-link">← 先随便逛逛</router-link>
      </div>
    </main>
  </div>
</template>

<style scoped>
.aside-brand {
  position: relative;
  z-index: 1;
  color: #f6f2ea;
}

.aside-brand strong {
  display: block;
  font-family: var(--font-display);
  font-size: 20px;
}

.aside-brand small {
  color: rgba(246, 242, 234, 0.6);
  font-size: 9px;
  letter-spacing: 0.16em;
}

.aside-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
}

.aside-foot {
  position: relative;
  z-index: 1;
  color: rgba(246, 242, 234, 0.55);
}

.back-link {
  display: inline-block;
  margin-top: 22px;
}

.back-link:hover {
  color: var(--brand);
}
</style>
