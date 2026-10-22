<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api/admin'
import type { AuditLogEntry } from '@/types'
import { formatDateTime } from '@/utils/format'

const logs = ref<AuditLogEntry[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const ACTION_LABELS: Record<string, string> = {
  ITEM_APPROVE: '商品审核通过',
  ITEM_REJECT: '商品审核驳回',
  REPORT_RESOLVE: '举报处理',
  REPORT_REJECT: '举报驳回',
  USER_WARN: '警告用户',
  USER_BAN: '封禁用户',
  USER_UNBAN: '解除封禁',
  USER_ROLE: '调整角色',
  CATEGORY_CREATE: '新建分类',
  CATEGORY_UPDATE: '更新分类',
  CATEGORY_DELETE: '删除分类',
}

const TARGET_LABELS: Record<string, string> = {
  ITEM: '商品',
  USER: '用户',
  REPORT: '举报工单',
  CATEGORY: '分类',
  ORDER: '订单',
}

async function load() {
  loading.value = true
  try {
    const result = await adminApi.auditLogs({ page: page.value, size: size.value })
    logs.value = result.list
    total.value = result.total
  } catch {
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(next: number) {
  page.value = next
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="row between wrap" style="gap: 12px; margin-bottom: 16px">
      <p class="small muted">共 {{ total }} 条操作记录 · 记录审核与治理动作，便于追责与复盘</p>
      <el-button round plain :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="logs" style="width: 100%" empty-text="暂无操作记录">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">
            <span class="tiny">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">
            {{ row.actorNickname || (row.actorId ? `#${row.actorId}` : '系统') }}
          </template>
        </el-table-column>
        <el-table-column label="动作" width="150">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ ACTION_LABELS[row.action] ?? row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="对象" width="150">
          <template #default="{ row }">
            <span class="tiny">
              {{ TARGET_LABELS[row.targetType] ?? row.targetType }}
              <template v-if="row.targetId"> #{{ row.targetId }}</template>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="详情" min-width="260">
          <template #default="{ row }">
            <span class="tiny muted">{{ row.detail ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="IP" width="140">
          <template #default="{ row }">
            <span class="tiny muted">{{ row.ip ?? '—' }}</span>
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
  </div>
</template>
