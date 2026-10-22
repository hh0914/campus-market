<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/social'
import { useAuthStore } from '@/stores/auth'
import type { PublicProfile } from '@/types'
import { ROLE_LABELS } from '@/utils/format'
import CreditBadge from '@/components/CreditBadge.vue'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const profile = ref<PublicProfile | null>(null)
const loading = ref(true)
const saving = ref(false)

const form = reactive({
  nickname: '',
  campus: '',
  avatarUrl: '',
})
const campuses = ['主校区', '东校区', '西校区', '南校区', '北校区']

const passwordVisible = ref(false)
const passwordBusy = ref(false)
const passwordRef = ref<FormInstance>()
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d)[\x21-\x7E]{8,32}$/,
      message: '密码需 8-32 位，且同时包含字母与数字',
      trigger: 'blur',
    },
  ],
  confirm: [
    {
      validator: (_rule, value: string, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const verifyVisible = ref(false)
const verifyBusy = ref(false)
const verifyCode = ref('')
const verifySent = ref(false)
const verifyHint = ref('')
const demoCode = ref('')

const reputation = computed(() => profile.value?.reputation ?? null)
const verified = computed(() => Boolean(auth.user?.verified))

async function load() {
  loading.value = true
  try {
    const me = await auth.refreshMe()
    if (!me) {
      router.replace({ name: 'login' })
      return
    }
    profile.value = await userApi.profile(me.id)
    form.nickname = me.nickname
    form.campus = me.campus ?? ''
    form.avatarUrl = me.avatarUrl ?? ''
  } catch {
    ElMessage.error('个人资料加载失败')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!form.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    const updated = await userApi.update({
      nickname: form.nickname.trim(),
      campus: form.campus,
      avatarUrl: form.avatarUrl.trim(),
    })
    auth.patchUser(updated)
    ElMessage.success('资料已更新')
    profile.value = await userApi.profile(updated.id)
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  const valid = await passwordRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  passwordBusy.value = true
  try {
    await userApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码已修改，其他设备需要重新登录')
    passwordVisible.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirm = ''
  } finally {
    passwordBusy.value = false
  }
}

async function sendVerification() {
  verifyBusy.value = true
  try {
    const result = await authApi.sendVerification()
    verifySent.value = true
    demoCode.value = result.code ?? ''
    verifyHint.value = `${result.hint || '验证码已发送到校内邮箱'}（${Math.round(result.expiresInSeconds / 60)} 分钟内有效）`
    ElMessage.success('验证码已发送')
  } finally {
    verifyBusy.value = false
  }
}

async function confirmVerification() {
  if (!verifyCode.value.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  verifyBusy.value = true
  try {
    const user = await authApi.confirmVerification(verifyCode.value.trim())
    auth.patchUser(user)
    verifyVisible.value = false
    verifyCode.value = ''
    verifySent.value = false
    ElMessage.success('校园认证完成')
    await load()
  } finally {
    verifyBusy.value = false
  }
}

async function logout() {
  await auth.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'home' })
}

watch(
  () => route.query.verify,
  (value) => {
    if (value === '1' && !verified.value) {
      verifyVisible.value = true
    }
  },
  { immediate: true },
)

onMounted(load)
</script>

<template>
  <div class="page-shell page">
    <div v-if="loading" v-loading="true" style="min-height: 420px" />

    <template v-else>
      <div class="row between wrap" style="gap: 14px; margin-bottom: 20px">
        <div>
          <h1 class="display" style="font-size: clamp(26px, 3.2vw, 34px)">个人中心</h1>
          <p class="muted small" style="margin-top: 6px">资料越完整，买家越信任你</p>
        </div>
        <div class="row" style="gap: 8px">
          <el-button v-if="auth.user" round plain @click="router.push({ name: 'user-profile', params: { id: auth.user.id } })">
            查看我的主页
          </el-button>
          <el-button round plain @click="logout">退出登录</el-button>
        </div>
      </div>

      <div class="profile-grid">
        <aside class="surface pad side-card">
          <div class="center">
            <UserAvatar :name="auth.user?.nickname" :src="auth.user?.avatarUrl" :size="82" />
            <h2 class="display" style="font-size: 22px; margin-top: 12px">{{ auth.displayName }}</h2>
            <p class="tiny muted" style="margin-top: 4px">{{ auth.user?.studentNo }}</p>
            <div class="row" style="justify-content: center; margin-top: 10px; gap: 8px">
              <CreditBadge :score="reputation?.creditScore" :badge="reputation?.badge" />
              <span v-if="verified" class="pill">✓ 已认证</span>
              <span v-else class="pill plain">未认证</span>
            </div>
          </div>

          <div class="divider" />

          <dl class="mini-list">
            <div><dt>身份</dt><dd>{{ ROLE_LABELS[auth.user?.role ?? 'STUDENT'] }}</dd></div>
            <div><dt>校区</dt><dd>{{ auth.user?.campus ?? '未填写' }}</dd></div>
            <div><dt>完成交易</dt><dd>{{ reputation?.completedDeals ?? 0 }}</dd></div>
            <div><dt>平均评分</dt><dd>{{ reputation?.avgRating?.toFixed(1) ?? '—' }}</dd></div>
          </dl>

          <el-button
            v-if="!verified"
            type="primary"
            round
            style="width: 100%; margin-top: 16px"
            @click="verifyVisible = true"
          >
            完成校园认证
          </el-button>
          <p v-else class="form-hint" style="margin-top: 16px">
            已完成校园邮箱认证，可正常发布商品与下单交易。
          </p>
        </aside>

        <section class="stack">
          <div class="surface pad">
            <h2 class="display" style="font-size: 20px; margin-bottom: 16px">基本资料</h2>
            <el-form label-position="top" size="large">
              <div class="form-grid">
                <el-form-item label="昵称">
                  <el-input v-model="form.nickname" maxlength="32" show-word-limit placeholder="同学们怎么称呼你" />
                </el-form-item>
                <el-form-item label="校区">
                  <el-select v-model="form.campus" allow-create filterable clearable style="width: 100%" placeholder="选择校区">
                    <el-option v-for="campus in campuses" :key="campus" :label="campus" :value="campus" />
                  </el-select>
                </el-form-item>
              </div>
              <el-form-item label="头像地址">
                <el-input v-model="form.avatarUrl" placeholder="粘贴图片链接，留空则使用昵称首字头像" />
              </el-form-item>
              <el-form-item label="学号（不可修改）">
                <el-input :model-value="auth.user?.studentNo" disabled />
              </el-form-item>
              <el-form-item label="校内邮箱（不可修改）">
                <el-input :model-value="auth.user?.email" disabled />
              </el-form-item>
              <div class="row wrap" style="gap: 10px">
                <el-button type="primary" round :loading="saving" @click="saveProfile">保存资料</el-button>
                <el-button round plain @click="passwordVisible = true">修改密码</el-button>
              </div>
            </el-form>
          </div>

          <div class="surface pad">
            <div class="row between" style="margin-bottom: 12px">
              <h2 class="display" style="font-size: 20px">信用概览</h2>
              <el-button text size="small" @click="router.push({ name: 'orders' })">查看订单 →</el-button>
            </div>
            <div class="credit-grid">
              <div class="credit-cell">
                <span class="tiny muted">信用分</span>
                <strong>{{ reputation?.creditScore ?? '—' }}</strong>
              </div>
              <div class="credit-cell">
                <span class="tiny muted">好评 / 差评</span>
                <strong>{{ reputation?.goodReviews ?? 0 }} / {{ reputation?.badReviews ?? 0 }}</strong>
              </div>
              <div class="credit-cell">
                <span class="tiny muted">取消订单</span>
                <strong>{{ reputation?.cancelledDeals ?? 0 }}</strong>
              </div>
              <div class="credit-cell">
                <span class="tiny muted">退款订单</span>
                <strong>{{ reputation?.refundedDeals ?? 0 }}</strong>
              </div>
            </div>
            <p class="form-hint" style="margin-top: 12px">
              完成交易 +2 分，收到 5 星好评 +2 分，取消或退款会扣分；信用分低于 60 将暂停发布。
            </p>
          </div>
        </section>
      </div>
    </template>

    <el-dialog v-model="passwordVisible" title="修改密码" width="min(440px, 92vw)">
      <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="8-32 位字母 + 数字" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirm">
          <el-input v-model="passwordForm.confirm" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordBusy" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="verifyVisible" title="校园身份认证" width="min(460px, 92vw)">
      <p class="small muted">
        我们会向你的校内邮箱 {{ auth.user?.email }} 发送 6 位验证码。认证后可发布商品、下单交易。
      </p>

      <el-alert
        v-if="demoCode"
        type="success"
        :closable="false"
        style="margin-top: 12px"
        :title="`演示环境验证码：${demoCode}`"
      />

      <div class="row" style="gap: 10px; margin-top: 16px">
        <el-input v-model="verifyCode" maxlength="6" placeholder="输入 6 位验证码" />
        <el-button round :loading="verifyBusy" @click="sendVerification">{{ verifySent ? '重新发送' : '发送验证码' }}</el-button>
      </div>
      <p v-if="verifyHint" class="form-hint">{{ verifyHint }}</p>

      <template #footer>
        <el-button @click="verifyVisible = false">稍后认证</el-button>
        <el-button type="primary" :loading="verifyBusy" :disabled="!verifySent" @click="confirmVerification">提交认证</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.side-card .center {
  text-align: center;
}

.side-card .center :deep(.avatar) {
  margin: 0 auto;
}

.mini-list {
  display: grid;
  gap: 8px;
  margin: 0;
}

.mini-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.mini-list dt {
  color: var(--muted);
}

.mini-list dd {
  margin: 0;
  font-weight: 600;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.credit-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.credit-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px;
  background: var(--paper);
  border-radius: 12px;
}

.credit-cell strong {
  font-family: var(--font-num);
  font-variant-numeric: lining-nums tabular-nums;
  font-size: 22px;
  line-height: 1.1;
}

@media (max-width: 900px) {
  .profile-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .credit-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
