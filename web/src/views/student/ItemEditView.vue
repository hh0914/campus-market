<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { categoryApi, itemApi } from '@/api/items'
import { useAuthStore } from '@/stores/auth'
import type { Category, ConditionLevel, TradeMethod } from '@/types'
import { CONDITION_LABELS, TRADE_METHOD_LABELS } from '@/utils/format'
import UploadImages from '@/components/UploadImages.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const editingId = computed(() => (route.params.id ? String(route.params.id) : ''))
const isEditing = computed(() => Boolean(editingId.value))

const formRef = ref<FormInstance>()
const categories = ref<Category[]>([])
const loading = ref(true)
const saving = ref('')

const form = reactive({
  title: '',
  description: '',
  price: null as number | null,
  originalPrice: null as number | null,
  conditionLevel: 'GOOD' as ConditionLevel,
  categoryId: null as number | null,
  campus: '主校区',
  tradeMethod: 'MEETUP' as TradeMethod,
  tags: '',
  imageUrls: [] as string[],
})

const conditions = Object.entries(CONDITION_LABELS).map(([value, label]) => ({ value, label }))
const tradeMethods = Object.entries(TRADE_METHOD_LABELS).map(([value, label]) => ({ value, label }))
const campuses = ['主校区', '东校区', '西校区', '南校区', '北校区']

const rules: FormRules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { max: 80, message: '标题最多 80 个字符', trigger: 'blur' },
  ],
  description: [
    { required: true, message: '请填写商品描述', trigger: 'blur' },
    { min: 10, max: 2000, message: '描述需 10-2000 个字符', trigger: 'blur' },
  ],
  price: [{ required: true, message: '请填写价格', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  conditionLevel: [{ required: true, message: '请选择成色', trigger: 'change' }],
  tradeMethod: [{ required: true, message: '请选择交易方式', trigger: 'change' }],
}

const pageTitle = computed(() => (isEditing.value ? '编辑商品' : '发布闲置'))

async function load() {
  loading.value = true
  try {
    categories.value = (await categoryApi.list()).filter((category) => category.enabled)
    if (isEditing.value) {
      const detail = await itemApi.detail(editingId.value)
      form.title = detail.title
      form.description = detail.description
      form.price = Number(detail.price)
      form.originalPrice = detail.originalPrice === null || detail.originalPrice === undefined ? null : Number(detail.originalPrice)
      form.conditionLevel = detail.conditionLevel
      form.categoryId = detail.categoryId ?? null
      form.campus = detail.campus ?? auth.user?.campus ?? '主校区'
      form.tradeMethod = detail.tradeMethod
      form.tags = detail.tags ?? ''
      form.imageUrls = [...detail.images]
    } else {
      form.campus = auth.user?.campus ?? '主校区'
      if (categories.value.length && form.categoryId === null) {
        form.categoryId = categories.value[0].id
      }
    }
  } catch {
    ElMessage.error('商品信息加载失败')
    router.replace({ name: 'my-items' })
  } finally {
    loading.value = false
  }
}

async function save(submitNow: boolean) {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (submitNow && form.price !== null && form.originalPrice !== null && form.originalPrice < form.price) {
    ElMessage.warning('原价应不低于现价，请确认后再发布')
    return
  }
  if (submitNow && form.imageUrls.length === 0) {
    const go = await ElMessageBox.confirm('还没有上传实拍图，买家会更信任有图的商品。仍然继续提交吗？', '缺少实拍图', {
      confirmButtonText: '继续提交',
      cancelButtonText: '去上传',
      type: 'warning',
    }).catch(() => false)
    if (!go) {
      return
    }
  }

  saving.value = submitNow ? 'submit' : 'draft'
  try {
    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      price: form.price,
      originalPrice: form.originalPrice,
      conditionLevel: form.conditionLevel,
      categoryId: form.categoryId,
      campus: form.campus,
      tradeMethod: form.tradeMethod,
      tags: form.tags.trim(),
      imageUrls: form.imageUrls,
      submitNow,
    }
    const saved = isEditing.value
      ? await itemApi.update(editingId.value, payload)
      : await itemApi.create(payload)
    ElMessage.success(submitNow ? '已提交审核，审核通过后自动上架' : '草稿已保存')
    router.replace({ name: 'item-detail', params: { id: saved.id } })
  } finally {
    saving.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="page-shell narrow page">
    <div class="row between" style="align-items: flex-end; margin-bottom: 18px">
      <div>
        <h1 class="display" style="font-size: clamp(26px, 3.4vw, 34px)">{{ pageTitle }}</h1>
        <p class="muted small" style="margin-top: 6px">实拍图 + 真实成色，能让商品更快卖出去。</p>
      </div>
      <el-button text @click="router.back()">← 返回</el-button>
    </div>

    <el-alert
      v-if="!auth.isVerified"
      type="warning"
      :closable="false"
      title="发布前需要完成校园认证"
      style="margin-bottom: 16px"
    >
      <template #default>
        <span class="small">认证后才能发布商品与下单。</span>
        <el-button size="small" type="warning" round style="margin-left: 8px" @click="router.push({ name: 'profile', query: { verify: '1' } })">
          去认证
        </el-button>
      </template>
    </el-alert>

    <div v-loading="loading" class="surface pad">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="save(false)">
        <el-form-item label="商品标题" prop="title">
          <el-input v-model="form.title" maxlength="80" show-word-limit placeholder="例如：iPad Air 4 64G 深空灰，附原装充电器" />
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            maxlength="2000"
            show-word-limit
            placeholder="写清入手时间、使用情况、配件、瑕疵位置，以及希望的交易时间地点。"
          />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="售价（元）" prop="price">
            <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="原价（元，可选）">
            <el-input-number
              v-model="form.originalPrice"
              :min="0"
              :precision="2"
              :step="10"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="成色" prop="conditionLevel">
            <el-select v-model="form.conditionLevel" style="width: 100%">
              <el-option v-for="item in conditions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
              <el-option v-for="category in categories" :key="category.id" :label="`${category.icon ?? ''} ${category.name}`" :value="category.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="校区">
            <el-select v-model="form.campus" allow-create filterable style="width: 100%">
              <el-option v-for="campus in campuses" :key="campus" :label="campus" :value="campus" />
            </el-select>
          </el-form-item>
          <el-form-item label="交易方式" prop="tradeMethod">
            <el-radio-group v-model="form.tradeMethod">
              <el-radio-button v-for="item in tradeMethods" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>

        <el-form-item label="标签">
          <el-input v-model="form.tags" maxlength="200" placeholder="用逗号分隔，例如：考研, 英语, 真题" />
        </el-form-item>

        <el-form-item label="商品图片">
          <UploadImages v-model="form.imageUrls" :limit="9" style="width: 100%" />
        </el-form-item>

        <div class="row wrap" style="gap: 12px; margin-top: 10px">
          <el-button type="primary" size="large" round :loading="saving === 'submit'" @click="save(true)">
            {{ isEditing ? '保存并重新提交审核' : '提交审核并上架' }}
          </el-button>
          <el-button size="large" round :loading="saving === 'draft'" @click="save(false)">保存为草稿</el-button>
        </div>
        <p class="form-hint">提交后进入审核队列，通常几分钟内完成；驳回时会给出具体原因，修改后可重新提交。</p>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
