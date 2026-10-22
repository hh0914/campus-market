<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import type { Category } from '@/types'

const categories = ref<Category[]>([])
const loading = ref(false)
const busyId = ref<number | null>(null)

const dialogVisible = ref(false)
const dialogBusy = ref(false)
const editing = ref<Category | null>(null)
const form = reactive({
  name: '',
  slug: '',
  icon: '',
  sortOrder: 10,
  enabled: true,
})

async function load() {
  loading.value = true
  try {
    categories.value = await adminApi.categories()
  } catch {
    categories.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.slug = ''
  form.icon = ''
  form.sortOrder = (categories.value.at(-1)?.sortOrder ?? 0) + 10
  form.enabled = true
  dialogVisible.value = true
}

function openEdit(category: Category) {
  editing.value = category
  form.name = category.name
  form.slug = category.slug
  form.icon = category.icon ?? ''
  form.sortOrder = category.sortOrder
  form.enabled = category.enabled
  dialogVisible.value = true
}

async function submit() {
  if (!form.name.trim() || !form.slug.trim()) {
    ElMessage.warning('分类名称与标识都不能为空')
    return
  }
  dialogBusy.value = true
  try {
    const payload = {
      name: form.name.trim(),
      slug: form.slug.trim(),
      icon: form.icon.trim(),
      sortOrder: form.sortOrder,
      enabled: form.enabled,
    }
    if (editing.value) {
      await adminApi.updateCategory(editing.value.id, payload)
      ElMessage.success('分类已更新')
    } else {
      await adminApi.createCategory(payload)
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    await load()
  } finally {
    dialogBusy.value = false
  }
}

async function toggle(category: Category) {
  busyId.value = category.id
  try {
    await adminApi.updateCategory(category.id, {
      name: category.name,
      slug: category.slug,
      icon: category.icon ?? '',
      sortOrder: category.sortOrder,
      enabled: !category.enabled,
    })
    ElMessage.success(category.enabled ? '分类已停用' : '分类已启用')
    await load()
  } finally {
    busyId.value = null
  }
}

async function remove(category: Category) {
  const confirmed = await ElMessageBox.confirm(`删除分类「${category.name}」？该分类下已有商品时会被拒绝。`, '删除分类', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).catch(() => false)
  if (!confirmed) {
    return
  }
  busyId.value = category.id
  try {
    await adminApi.deleteCategory(category.id)
    ElMessage.success('分类已删除')
    await load()
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="row between wrap" style="gap: 12px; margin-bottom: 16px">
      <p class="small muted">共 {{ categories.length }} 个分类 · 停用后不会出现在前台筛选与发布页</p>
      <div class="row" style="gap: 8px">
        <el-button round plain :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" round @click="openCreate">＋ 新建分类</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="categories" style="width: 100%" empty-text="还没有分类">
        <el-table-column label="分类" min-width="200">
          <template #default="{ row }">
            <div class="row" style="gap: 10px">
              <span class="cat-icon">{{ row.icon ?? '📦' }}</span>
              <div>
                <strong>{{ row.name }}</strong>
                <div class="tiny muted">{{ row.slug }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" align="center" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled ? 'success' : 'info'" effect="light">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="right">
          <template #default="{ row }">
            <el-button size="small" round @click="openEdit(row)">编辑</el-button>
            <el-button size="small" round plain :loading="busyId === row.id" @click="toggle(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" round plain :loading="busyId === row.id" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑分类' : '新建分类'" width="min(460px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="分类名称">
          <el-input v-model="form.name" maxlength="20" placeholder="例如：数码电子" />
        </el-form-item>
        <el-form-item label="英文标识">
          <el-input v-model="form.slug" maxlength="32" placeholder="例如：digital" />
        </el-form-item>
        <el-form-item label="图标（emoji）">
          <el-input v-model="form.icon" maxlength="4" placeholder="例如：💻" />
        </el-form-item>
        <div class="row" style="gap: 14px">
          <el-form-item label="排序权重" style="flex: 1">
            <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 100%" />
          </el-form-item>
          <el-form-item label="是否启用" style="flex: 1">
            <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogBusy" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.cat-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--brand-soft);
  font-size: 17px;
}
</style>
