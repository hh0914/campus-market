<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { ReportItem, ReportStatus } from '@/types'
import { REPORT_STATUS_LABELS, formatDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

const reports = ref<ReportItem[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const status = ref<ReportStatus | ''>('PENDING')
const busyId = ref<number | null>(null)

const resolveVisible = ref(false)
const resolveBusy = ref(false)
const current = ref<ReportItem | null>(null)
const resolveForm = reactive({ action: 'NONE', remark: '' })

const rejectVisible = ref(false)
const rejectBusy = ref(false)
const rejectRemark = ref('')

const statuses: (ReportStatus | '')[] = ['', 'PENDING', 'PROCESSING', 'RESOLVED', 'REJECTED']
const actions = [
  { value: 'NONE', label: '仅记录处理意见' },
  { value: 'DOWN_ITEM', label: '下架商品并扣信用分' },
  { value: 'WARN', label: '警告违规用户' },
  { value: 'BAN', label: '封禁账号 7 天' },
]

async function load() {
  loading.value = true
  try {
    const result = await adminApi.reports({ status: status.value || undefined, page: page.value, size: size.value })
    reports.value = result.list
    total.value = result.total
  } catch {
    reports.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function pick(next: ReportStatus | '') {
  status.value = next
  page.value = 1
  load()
}

async function claim(report: ReportItem) {
  busyId.value = report.id
  try {
    await adminApi.claimReport(report.id)
    ElMessage.success('工单已领取，请尽快核实处理')
    await load()
  } finally {
    busyId.value = null
  }
}

function openResolve(report: ReportItem) {
  current.value = report
  resolveForm.action = report.targetType === 'ITEM' ? 'DOWN_ITEM' : 'WARN'
  resolveForm.remark = ''
  resolveVisible.value = true
}

async function submitResolve() {
  if (!current.value) {
    return
  }
  if (resolveForm.remark.trim().length < 3) {
    ElMessage.warning('处理意见至少 3 个字')
    return
  }
  resolveBusy.value = true
  try {
    await adminApi.resolveReport(current.value.id, {
      action: resolveForm.action,
      remark: resolveForm.remark.trim(),
    })
    ElMessage.success('工单已处理')
    resolveVisible.value = false
    await load()
  } finally {
    resolveBusy.value = false
  }
}

function openReject(report: ReportItem) {
  current.value = report
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!current.value) {
    return
  }
  if (rejectRemark.value.trim().length < 3) {
    ElMessage.warning('驳回意见至少 3 个字')
    return
  }
  rejectBusy.value = true
  try {
    await adminApi.rejectReport(current.value.id, rejectRemark.value.trim())
    ElMessage.success('已驳回，举报人会收到通知')
    rejectVisible.value = false
    await load()
  } finally {
    rejectBusy.value = false
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
      <div class="chip-row">
        <button
          v-for="item in statuses"
          :key="item || 'all'"
          type="button"
          class="chip"
          :class="{ active: status === item }"
          @click="pick(item)"
        >
          {{ item ? REPORT_STATUS_LABELS[item] : '全部工单' }}
        </button>
      </div>
      <div class="row" style="gap: 10px">
        <span class="tiny muted">共 {{ total }} 条</span>
        <el-button round plain :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="reports" style="width: 100%" empty-text="当前没有工单">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <div>
                <span class="tiny muted">举报描述</span>
                <p class="small" style="margin-top: 4px">{{ row.description }}</p>
              </div>
              <div v-if="row.evidenceUrl">
                <span class="tiny muted">证据链接</span>
                <p class="small" style="margin-top: 4px; word-break: break-all">
                  <a :href="row.evidenceUrl" target="_blank" rel="noreferrer" class="brand-text">{{ row.evidenceUrl }}</a>
                </p>
              </div>
              <div v-if="row.handleRemark">
                <span class="tiny muted">处理意见</span>
                <p class="small" style="margin-top: 4px">{{ row.handleRemark }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="工单" width="90">
          <template #default="{ row }">
            <strong>#{{ row.id }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="举报对象" min-width="220">
          <template #default="{ row }">
            <div>
              <span class="pill plain tiny">{{ row.targetType === 'ITEM' ? '商品' : '用户' }}</span>
              <div class="ellipsis" style="margin-top: 4px; font-weight: 500">{{ row.targetTitle ?? '—' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ row.reasonLabel }}</template>
        </el-table-column>
        <el-table-column label="举报人" width="120">
          <template #default="{ row }">{{ row.reporterNickname ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.status" kind="report" />
          </template>
        </el-table-column>
        <el-table-column label="处理人" width="110">
          <template #default="{ row }">
            <span class="tiny">{{ row.handlerNickname ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">
            <span class="tiny muted">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              round
              :loading="busyId === row.id"
              @click="claim(row)"
            >
              领取
            </el-button>
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'PROCESSING'"
              size="small"
              type="primary"
              round
              @click="openResolve(row)"
            >
              处理
            </el-button>
            <el-button
              v-if="row.status === 'PENDING' || row.status === 'PROCESSING'"
              size="small"
              round
              plain
              @click="openReject(row)"
            >
              驳回
            </el-button>
            <span v-if="row.status === 'RESOLVED' || row.status === 'REJECTED'" class="tiny muted">
              {{ formatDateTime(row.handledAt) }}
            </span>
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

    <el-dialog v-model="resolveVisible" title="处理举报工单" width="min(520px, 94vw)">
      <template v-if="current">
        <div class="report-summary">
          <div class="row between">
            <span class="tiny muted">工单 #{{ current.id }} · {{ current.reasonLabel }}</span>
            <StatusTag :status="current.status" kind="report" />
          </div>
          <p class="small" style="margin-top: 6px">{{ current.targetTitle }}</p>
          <p class="small muted" style="margin-top: 4px">{{ current.description }}</p>
        </div>

        <el-form label-position="top" style="margin-top: 16px">
          <el-form-item label="处置动作">
            <el-select v-model="resolveForm.action" style="width: 100%">
              <el-option v-for="action in actions" :key="action.value" :label="action.label" :value="action.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="处理意见">
            <el-input
              v-model="resolveForm.remark"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="说明核实结果与处置依据，会通知举报人与被举报人"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" :loading="resolveBusy" @click="submitResolve">确认处理</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回举报" width="min(460px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="驳回意见">
          <el-input
            v-model="rejectRemark"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="例如：图片与描述一致，未发现违规行为"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="rejectBusy" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.expand-box {
  display: grid;
  gap: 12px;
  padding: 6px 18px 16px 48px;
  background: #fbf8f1;
}

.report-summary {
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--paper);
}
</style>
