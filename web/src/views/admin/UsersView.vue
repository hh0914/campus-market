<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import type { AdminUser } from '@/types'
import { ROLE_LABELS, USER_STATUS_LABELS, formatDateTime } from '@/utils/format'
import UserAvatar from '@/components/UserAvatar.vue'

const auth = useAuthStore()

const users = ref<AdminUser[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const busyId = ref<number | null>(null)

const filters = reactive({ keyword: '', role: '', status: '' })

const roles = [
  { value: '', label: '全部角色' },
  { value: 'STUDENT', label: ROLE_LABELS.STUDENT },
  { value: 'MODERATOR', label: ROLE_LABELS.MODERATOR },
  { value: 'ADMIN', label: ROLE_LABELS.ADMIN },
]
const statuses = [
  { value: '', label: '全部状态' },
  { value: 'ACTIVE', label: USER_STATUS_LABELS.ACTIVE },
  { value: 'WARNED', label: USER_STATUS_LABELS.WARNED },
  { value: 'BANNED', label: USER_STATUS_LABELS.BANNED },
]

const warnVisible = ref(false)
const banVisible = ref(false)
const roleVisible = ref(false)
const dialogBusy = ref(false)
const current = ref<AdminUser | null>(null)
const warnReason = ref('')
const banReason = ref('')
const banDays = ref(7)
const nextRole = ref('STUDENT')

async function load() {
  loading.value = true
  try {
    const result = await adminApi.users({
      keyword: filters.keyword.trim() || undefined,
      role: filters.role || undefined,
      status: filters.status || undefined,
      page: page.value,
      size: size.value,
    })
    users.value = result.list
    total.value = result.total
  } catch {
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  search()
}

function changePage(next: number) {
  page.value = next
  load()
}

function openWarn(user: AdminUser) {
  current.value = user
  warnReason.value = ''
  warnVisible.value = true
}

async function submitWarn() {
  if (!current.value) return
  if (warnReason.value.trim().length < 2) {
    ElMessage.warning('警告原因至少 2 个字')
    return
  }
  dialogBusy.value = true
  try {
    await adminApi.warnUser(current.value.id, warnReason.value.trim())
    ElMessage.success('已发送警告')
    warnVisible.value = false
    await load()
  } finally {
    dialogBusy.value = false
  }
}

function openBan(user: AdminUser) {
  current.value = user
  banReason.value = ''
  banDays.value = 7
  banVisible.value = true
}

async function submitBan() {
  if (!current.value) return
  if (banReason.value.trim().length < 2) {
    ElMessage.warning('封禁原因至少 2 个字')
    return
  }
  dialogBusy.value = true
  try {
    await adminApi.banUser(current.value.id, banReason.value.trim(), banDays.value)
    ElMessage.success('已封禁该账号')
    banVisible.value = false
    await load()
  } finally {
    dialogBusy.value = false
  }
}

async function unban(user: AdminUser) {
  const confirmed = await ElMessageBox.confirm(`解除 ${user.nickname} 的封禁？`, '解除封禁', {
    confirmButtonText: '解除',
    cancelButtonText: '取消',
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  busyId.value = user.id
  try {
    await adminApi.unbanUser(user.id, '人工复核解除')
    ElMessage.success('已解除封禁')
    await load()
  } finally {
    busyId.value = null
  }
}

function openRole(user: AdminUser) {
  current.value = user
  nextRole.value = user.role
  roleVisible.value = true
}

async function submitRole() {
  if (!current.value) return
  dialogBusy.value = true
  try {
    await adminApi.changeRole(current.value.id, nextRole.value)
    ElMessage.success('角色已更新')
    roleVisible.value = false
    await load()
  } finally {
    dialogBusy.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar" style="margin-bottom: 16px">
      <el-input v-model="filters.keyword" placeholder="搜索学号、昵称或邮箱" clearable style="max-width: 280px" @keyup.enter="search" />
      <el-select v-model="filters.role" style="width: 140px">
        <el-option v-for="item in roles" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.status" style="width: 140px">
        <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-button type="primary" round @click="search">查询</el-button>
      <el-button round plain @click="reset">重置</el-button>
      <span class="tiny muted" style="margin-left: auto">共 {{ total }} 位用户</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="users" style="width: 100%" empty-text="没有匹配的用户">
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="row" style="gap: 10px">
              <UserAvatar :name="row.nickname" :src="row.avatarUrl" :size="34" />
              <div style="min-width: 0">
                <div class="row" style="gap: 6px">
                  <strong class="ellipsis">{{ row.nickname }}</strong>
                  <span v-if="row.verified" class="pill tiny">已认证</span>
                </div>
                <div class="tiny muted">{{ row.studentNo }} · {{ row.email }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : row.role === 'MODERATOR' ? 'warning' : 'info'" effect="light">
              {{ ROLE_LABELS[row.role] ?? row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.status === 'ACTIVE' ? 'success' : row.status === 'WARNED' ? 'warning' : 'danger'"
              effect="light"
            >
              {{ USER_STATUS_LABELS[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="信用分" width="90" align="center">
          <template #default="{ row }">
            <strong>{{ row.creditScore ?? '—' }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="onSaleItems" label="在售" width="80" align="center" />
        <el-table-column label="最近登录" width="170">
          <template #default="{ row }">
            <span class="tiny muted">{{ formatDateTime(row.lastLoginAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="封禁至" width="150">
          <template #default="{ row }">
            <span class="tiny muted">{{ row.status === 'BANNED' ? formatDateTime(row.bannedUntil) : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="right">
          <template #default="{ row }">
            <template v-if="row.id !== auth.user?.id">
              <el-button size="small" round :disabled="row.status === 'BANNED'" @click="openWarn(row)">警告</el-button>
              <el-button
                v-if="row.status !== 'BANNED'"
                size="small"
                type="danger"
                round
                plain
                :loading="busyId === row.id"
                @click="openBan(row)"
              >
                封禁
              </el-button>
              <el-button v-else size="small" type="success" round plain :loading="busyId === row.id" @click="unban(row)">
                解封
              </el-button>
              <el-button v-if="auth.isAdmin" size="small" round plain @click="openRole(row)">角色</el-button>
            </template>
            <span v-else class="tiny muted">当前账号</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="total > size" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="changePage"
      />
    </div>

    <el-dialog v-model="warnVisible" title="警告用户" width="min(440px, 94vw)">
      <p v-if="current" class="small muted" style="margin-bottom: 12px">
        将向 {{ current.nickname }} 发送平台警告通知，并标记为「已警告」。
      </p>
      <el-input v-model="warnReason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="填写违规说明与整改要求" />
      <template #footer>
        <el-button @click="warnVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogBusy" @click="submitWarn">发送警告</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="banVisible" title="封禁账号" width="min(440px, 94vw)">
      <p v-if="current" class="small muted" style="margin-bottom: 12px">
        封禁后 {{ current.nickname }} 将无法登录与交易，信用分同时扣减。
      </p>
      <el-form label-position="top">
        <el-form-item label="封禁天数">
          <el-input-number v-model="banDays" :min="1" :max="365" style="width: 100%" />
        </el-form-item>
        <el-form-item label="封禁原因">
          <el-input v-model="banReason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="例如：多次发布违禁物品" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banVisible = false">取消</el-button>
        <el-button type="danger" :loading="dialogBusy" @click="submitBan">确认封禁</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="调整角色" width="min(420px, 94vw)">
      <p v-if="current" class="small muted" style="margin-bottom: 12px">{{ current.nickname }} 当前角色：{{ ROLE_LABELS[current.role] }}</p>
      <el-select v-model="nextRole" style="width: 100%">
        <el-option label="学生" value="STUDENT" />
        <el-option label="审核员" value="MODERATOR" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>
      <p class="form-hint" style="margin-top: 8px">系统至少保留一名管理员，不能修改自己的角色。</p>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogBusy" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
