<template>
  <div class="feedback-section">
    <div class="feedback-header">
      <span class="feedback-title">我的反馈</span>
      <el-button type="primary" @click="openSubmitDialog">提交反馈</el-button>
    </div>

    <div class="feedback-list" v-loading="loading">
      <div
        v-for="item in list"
        :key="item.id"
        class="feedback-card"
        :class="{ 'feedback-card--expanded': expandedId === item.id }"
      >
        <div class="feedback-card__summary" @click="toggleExpand(item)">
          <div class="feedback-card__top">
            <el-tag :type="item.type === 1 ? 'danger' : 'primary'" size="small">
              {{ FEEDBACK_TYPE_MAP[item.type] }}
            </el-tag>
            <span class="feedback-card__title">{{ item.title }}</span>
            <el-tag :type="FEEDBACK_STATUS_COLOR[item.status] as any" size="small" effect="plain">
              {{ FEEDBACK_STATUS_MAP[item.status] }}
            </el-tag>
          </div>
          <div class="feedback-card__meta">
            <span>{{ item.createTime }}</span>
            <el-icon class="feedback-card__arrow" :class="{ rotated: expandedId === item.id }">
              <ArrowDown />
            </el-icon>
          </div>
        </div>

        <div class="feedback-card__detail" v-if="expandedId === item.id">
          <el-divider />
          <div class="feedback-card__content">{{ item.content }}</div>

          <div class="feedback-card__images" v-if="imageList(item.images).length">
            <el-image
              v-for="(img, i) in imageList(item.images)"
              :key="i"
              :src="img"
              fit="cover"
              :preview-src-list="imageList(item.images)"
              :initial-index="i"
              class="feedback-card__img"
            />
          </div>

          <div class="feedback-card__timeline">
            <el-steps :active="statusStep(item.status)" finish-status="success" align-center>
              <el-step title="已提交" :description="item.createTime" />
              <el-step title="处理中" :description="item.handleTime && item.status >= 1 ? item.handleTime : ''" />
              <el-step title="已解决" :description="item.status === 2 ? item.handleTime : ''" />
            </el-steps>
          </div>

          <div class="feedback-card__reply" v-if="item.adminReply">
            <div class="reply-label">管理员回复：</div>
            <div class="reply-content">{{ item.adminReply }}</div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && !list.length" description="暂无反馈，去提交你的第一条反馈吧" />

      <div class="feedback-pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadList"
        />
      </div>
    </div>

    <!-- Submit Dialog -->
    <el-dialog v-model="dialogVisible" title="提交反馈" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="反馈类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择反馈类型">
            <el-option label="问题反馈" :value="1" />
            <el-option label="功能建议" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="反馈标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="反馈内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            placeholder="请详细描述您遇到的问题或建议..."
            maxlength="2000"
            show-word-limit
            :rows="5"
          />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.contact" placeholder="选填，默认使用您的邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="截图上传">
          <el-upload
            action="/api/upload"
            :headers="uploadHeaders"
            list-type="picture-card"
            :limit="3"
            :before-upload="beforeImageUpload"
            :on-success="onImageSuccess"
            :on-remove="onImageRemove"
            :file-list="fileList"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadProps } from 'element-plus'
import { ArrowDown, Plus } from '@element-plus/icons-vue'
import { submitFeedback, getMyFeedbackList } from '@/api/feedback'
import { TOKEN_KEY, FEEDBACK_TYPE_MAP, FEEDBACK_STATUS_MAP, FEEDBACK_STATUS_COLOR, DEFAULT_PAGE_SIZE } from '@shared/constants'
import type { Feedback } from '@shared/types/feedback'

const loading = ref(false)
const list = ref<Feedback[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = DEFAULT_PAGE_SIZE
const expandedId = ref<number | null>(null)

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY) || ''}`,
}))

function statusStep(status: number) {
  if (status === 0) return 0
  if (status === 1) return 1
  if (status >= 2) return 2
  return 0
}

function toggleExpand(item: Feedback) {
  expandedId.value = expandedId.value === item.id ? null : item.id
}

async function loadList() {
  loading.value = true
  try {
    const res = await getMyFeedbackList(currentPage.value, pageSize)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// ---- Submit Dialog ----
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const uploadedImages = ref<string[]>([])
const fileList = ref<UploadProps['fileList']>([])

const form = reactive({
  type: 1 as number,
  title: '',
  content: '',
  contact: '',
})

const rules: FormRules = {
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入反馈标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入反馈内容', trigger: 'blur' }],
}

function openSubmitDialog() {
  form.type = 1
  form.title = ''
  form.content = ''
  form.contact = ''
  uploadedImages.value = []
  fileList.value = []
  dialogVisible.value = true
}

function beforeImageUpload(file: File) {
  const isValid = /^image\/(jpeg|png|gif|webp)$/.test(file.type)
  if (!isValid) {
    ElMessage.error('仅支持 JPG/PNG/GIF/WebP 格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return false
  }
  return true
}

function onImageSuccess(response: { code: number; data: { url: string } }) {
  if (response.code === 200) {
    uploadedImages.value.push(response.data.url)
  }
}

function onImageRemove(_file: UploadFile) {
  const url = _file.response?.data?.url || _file.url
  if (url) {
    const idx = uploadedImages.value.indexOf(url)
    if (idx > -1) uploadedImages.value.splice(idx, 1)
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    await submitFeedback({
      type: form.type,
      title: form.title,
      content: form.content,
      contact: form.contact || undefined,
      images: uploadedImages.value.length ? uploadedImages.value : undefined,
    })
    ElMessage.success('反馈已提交')
    dialogVisible.value = false
    currentPage.value = 1
    await loadList()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadList()
})

function imageList(images: string): string[] {
  if (!images) return []
  return images.split(',').filter(Boolean)
}

defineExpose({ loadList })
</script>

<style lang="scss" scoped>
.feedback-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.feedback-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.feedback-title {
  font-weight: 600;
  font-size: 16px;
}

.feedback-list {
  min-height: 200px;
}

.feedback-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  margin-bottom: 12px;
  overflow: hidden;
  transition: box-shadow .2s;

  &:last-child { margin-bottom: 0; }

  &:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, .06); }

  &__summary {
    padding: 16px 20px;
    cursor: pointer;
    user-select: none;
  }

  &__top {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }

  &__title {
    font-size: 15px;
    font-weight: 500;
    color: #333;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    font-size: 13px;
    color: #999;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__arrow {
    font-size: 14px;
    transition: transform .2s;
    &.rotated { transform: rotate(180deg); }
  }

  &__detail {
    padding: 0 20px 20px;
  }

  &__content {
    font-size: 14px;
    color: #555;
    line-height: 1.8;
    white-space: pre-wrap;
    word-break: break-word;
  }

  &__images {
    display: flex;
    gap: 8px;
    margin-top: 12px;
  }

  &__img {
    width: 80px;
    height: 80px;
    border-radius: 6px;
    border: 1px solid #eee;
  }

  &__timeline {
    margin-top: 20px;
  }

  &__reply {
    margin-top: 16px;
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 8px;
  }

  .reply-label {
    font-size: 13px;
    color: #999;
    margin-bottom: 6px;
  }

  .reply-content {
    font-size: 14px;
    color: #333;
    line-height: 1.6;
    white-space: pre-wrap;
  }
}

.feedback-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .feedback-card__top {
    flex-wrap: wrap;
  }
}
</style>
