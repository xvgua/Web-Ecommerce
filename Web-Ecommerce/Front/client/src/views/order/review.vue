<template>
  <div class="review-page">
    <h1 class="page-title">{{ isFollowUp ? '追加评价' : '发表评价' }}</h1>

    <div v-loading="loading" class="review-card">
      <div class="review-card__product" v-if="orderItem">
        <div class="review-card__product-img">
          <ProductImage :src="orderItem.productImage" :seed="orderItem.productName + orderItem.productId" fit="cover" />
        </div>
        <div class="review-card__product-info">
          <div class="review-card__product-name">{{ orderItem.productName }}</div>
          <div class="review-card__product-spec" v-if="orderItem.specDesc">{{ orderItem.specDesc }}</div>
          <div class="review-card__product-price">{{ formatPrice(orderItem.price) }}</div>
        </div>
      </div>

      <el-divider />

      <div class="review-card__form">
        <div class="review-card__rating">
          <span class="review-card__label">评分</span>
          <el-rate v-model="rating" :max="5" show-score :texts="rateTexts" />
        </div>

        <div class="review-card__content">
          <span class="review-card__label">评价内容</span>
          <el-input
            v-model="content"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="请分享您对这件商品的使用感受..."
          />
        </div>

        <div class="review-card__images">
          <span class="review-card__label">上传图片 <span class="review-card__label-hint">（可选，最多5张）</span></span>
          <div class="review-card__upload-list">
            <div v-for="(img, i) in imageUrls" :key="i" class="review-card__upload-item">
              <el-image :src="img" fit="cover" class="review-card__upload-preview" />
              <span class="review-card__upload-remove" @click="removeImage(i)">
                <el-icon><Close /></el-icon>
              </span>
            </div>
            <el-upload
              v-if="imageUrls.length < 5"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :before-upload="beforeUpload"
              :on-success="onUploadSuccess"
              :on-error="onUploadError"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="review-card__upload-btn"
            >
              <el-icon :size="24"><Plus /></el-icon>
            </el-upload>
          </div>
        </div>
      </div>

      <el-divider />

      <div class="review-card__actions">
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
          {{ isFollowUp ? '提交追加评价' : '提交评价' }}
        </el-button>
      </div>
    </div>

    <el-empty v-if="!loading && !orderItem" description="商品不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import { getOrderById } from '@/api/order'
import { createReview, createFollowUpReview } from '@/api/product'
import { formatPrice } from '@/utils/format'
import { TOKEN_KEY } from '@shared/constants'
import type { OrderItem } from '@shared/types/order'
import ProductImage from '@/components/common/ProductImage.vue'

const route = useRoute()
const router = useRouter()

const orderId = Number(route.params.orderId)
const productId = Number(route.params.productId)
const isFollowUp = computed(() => route.query.followUp === '1')

const loading = ref(false)
const submitting = ref(false)
const orderItem = ref<OrderItem | null>(null)

const rating = ref(0)
const content = ref('')
const imageUrls = ref<string[]>([])

const canSubmit = computed(() => isFollowUp.value ? content.value.trim().length > 0 : rating.value > 0)

const rateTexts = ['非常差', '差', '一般', '好', '非常好']

const uploadUrl = '/api/upload'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY) || ''}`,
}))

function beforeUpload(file: File) {
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

function onUploadSuccess(res: { code: number; data: { url: string }; message: string }) {
  if (res.code === 200 && res.data?.url) {
    imageUrls.value.push(res.data.url)
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

function onUploadError() {
  ElMessage.error('上传失败，请重试')
}

function removeImage(index: number) {
  imageUrls.value.splice(index, 1)
}

async function loadOrderItem() {
  loading.value = true
  try {
    const res = await getOrderById(orderId)
    const order = res.data
    if (order.dealTime) {
      const deadline = new Date(order.dealTime)
      deadline.setMonth(deadline.getMonth() + 1)
      if (deadline <= new Date()) {
        ElMessage.warning('该订单已完成超过1个月，已关闭评价入口')
        router.replace(`/orders/${orderId}`)
        return
      }
    }
    const item = order.items?.find(i => i.productId === productId)
    if (item) {
      orderItem.value = item
    }
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!isFollowUp.value && !rating.value) {
    ElMessage.warning('请给商品评分')
    return
  }
  if (!content.value.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }

  submitting.value = true
  try {
    const data = {
      productId,
      orderId,
      rating: rating.value,
      content: content.value.trim(),
      images: imageUrls.value,
    }
    if (isFollowUp.value) {
      await createFollowUpReview(data)
      ElMessage.success('追加评价成功')
    } else {
      await createReview(data)
      ElMessage.success('评价成功')
    }
    router.push(`/orders/${orderId}`)
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (orderId && productId) {
    loadOrderItem()
  }
})
</script>

<style lang="scss" scoped>
.review-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
}

.review-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;

  &__product {
    display: flex;
    gap: 14px;
    align-items: center;

    &-img {
      width: 72px;
      height: 72px;
      border-radius: 6px;
      overflow: hidden;
      flex-shrink: 0;
    }

    &-info { flex: 1; min-width: 0; }

    &-name { font-size: 14px; font-weight: 600; }

    &-spec { font-size: 12px; color: #999; margin-top: 4px; }

    &-price { font-size: 14px; font-weight: 600; color: #e6423a; margin-top: 6px; }
  }

  &__form { display: flex; flex-direction: column; gap: 20px; }

  &__label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;

    &-hint { font-weight: 400; font-size: 12px; color: #999; }
  }

  &__rating {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__upload-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }

  &__upload-item {
    position: relative;
    width: 80px;
    height: 80px;
    border-radius: 6px;
    overflow: hidden;
    border: 1px solid #e5e5e5;
  }

  &__upload-preview {
    width: 100%;
    height: 100%;
  }

  &__upload-remove {
    position: absolute;
    top: -4px;
    right: -4px;
    width: 18px;
    height: 18px;
    background: #e6423a;
    color: #fff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 10px;
  }

  &__upload-btn {
    :deep(.el-upload) {
      width: 80px;
      height: 80px;
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: border-color .2s;

      &:hover { border-color: #409eff; }
    }
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>
