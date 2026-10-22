<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { uploadImage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const props = withDefaults(
  defineProps<{
    modelValue: string[]
    limit?: number
  }>(),
  { limit: 9 },
)

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const auth = useAuthStore()
const uploading = ref(false)
const images = computed(() => props.modelValue)

function update(next: string[]) {
  emit('update:modelValue', next)
}

async function customUpload(options: UploadRequestOptions) {
  if (!auth.isLoggedIn) {
    ElMessage.warning('请先登录再上传图片')
    return
  }
  uploading.value = true
  try {
    const url = await uploadImage(options.file)
    update([...props.modelValue, url])
    ElMessage.success('图片已上传')
  } finally {
    uploading.value = false
  }
}

function removeAt(index: number) {
  const next = [...props.modelValue]
  next.splice(index, 1)
  update(next)
}

watch(
  () => props.modelValue,
  (value) => {
    if (value.length > props.limit) {
      update(value.slice(0, props.limit))
    }
  },
)
</script>

<template>
  <div class="uploader">
    <div class="grid">
      <div v-for="(url, index) in images" :key="url" class="cell">
        <img :src="url" alt="商品图片" />
        <button type="button" class="remove" aria-label="移除图片" @click="removeAt(index)">×</button>
      </div>

      <el-upload
        v-if="images.length < limit"
        class="cell add"
        :show-file-list="false"
        :http-request="customUpload"
        accept="image/png,image/jpeg,image/webp"
      >
        <div class="add-inner" :class="{ busy: uploading }">
          <span class="plus">＋</span>
          <span class="tiny muted">{{ uploading ? '上传中' : '添加图片' }}</span>
        </div>
      </el-upload>
    </div>
    <p class="form-hint">最多 {{ limit }} 张，支持 jpg / png / webp，单张不超过 5MB；第一张作为封面。</p>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: 12px;
}

.cell {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
  background: var(--paper-deep);
  border: 1px solid var(--line);
}

.cell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cell.add {
  border-style: dashed;
  background: transparent;
}

.add-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 100%;
  height: 100%;
  color: var(--muted);
}

.add-inner.busy {
  opacity: 0.6;
}

.plus {
  font-size: 22px;
  line-height: 1;
}

.remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: rgba(20, 32, 28, 0.72);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}

.remove:hover {
  background: var(--pop);
}
</style>
