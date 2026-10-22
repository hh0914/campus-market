<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  studentNo: '',
  nickname: '',
  email: '',
  password: '',
  confirm: '',
})

const rules: FormRules = {
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^[0-9A-Za-z]{6,20}$/, message: '学号需 6-20 位字母或数字', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 32, message: '昵称最多 32 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入校内邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,32}$/,
      message: '密码需 8-32 位，且同时包含字母与数字',
      trigger: 'blur',
    },
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await auth.register({
      studentNo: form.studentNo.trim(),
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      password: form.password,
    })
    ElMessage.success('注册成功，记得去个人中心完成校园认证')
    router.replace({ name: 'profile' })
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
        <p class="quote">一次认证，四年可用。你的信用从第一笔交易开始累积。</p>
        <div class="points">
          <div><span>📮</span> 校内邮箱验证，杜绝校外账号</div>
          <div><span>📦</span> 发布 / 议价 / 托管 / 评价一站完成</div>
          <div><span>🤝</span> 面交或快递，交易方式自己定</div>
        </div>
      </div>

      <p class="tiny aside-foot">注册即表示同意校园集交易公约与隐私说明</p>
    </aside>

    <main class="auth-main">
      <div class="auth-card">
        <p class="pill">新同学</p>
        <h2 class="display">创建账号</h2>
        <p class="muted small" style="margin-top: 8px">填写真实学号与校内邮箱，方便后续校园认证。</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          style="margin-top: 24px"
          @submit.prevent="submit"
        >
          <div class="two-col">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="form.studentNo" placeholder="例如 2021001" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="同学们怎么称呼你" maxlength="32" />
            </el-form-item>
          </div>
          <el-form-item label="校内邮箱" prop="email">
            <el-input v-model="form.email" placeholder="name@campus.edu" />
          </el-form-item>
          <div class="two-col">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="8-32 位字母 + 数字" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirm">
              <el-input v-model="form.confirm" type="password" show-password placeholder="再输入一次" @keyup.enter="submit" />
            </el-form-item>
          </div>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">注册</el-button>
        </el-form>

        <div class="row between" style="margin-top: 16px">
          <span class="small muted">已有账号？</span>
          <router-link :to="{ name: 'login' }" class="brand-text small" style="font-weight: 600">直接登录 →</router-link>
        </div>
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

.two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

@media (max-width: 560px) {
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
