<template>
  <div class="order-list-page">
    <h1 class="page-title">我的订单</h1>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="order-tabs">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="待收货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="已取消" name="4" />
      <el-tab-pane label="评价" name="review" />
      <el-tab-pane label="退款/售后" name="refund" />
    </el-tabs>

    <!-- 评价子标签 -->
    <div class="review-subtabs" v-if="activeTab === 'review'">
      <el-radio-group v-model="reviewSubTab" size="small" @change="handleSubTabChange">
        <el-radio-button value="pending">待评价</el-radio-button>
        <el-radio-button value="reviewed">已评价</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 已评价：直接展示评价列表 -->
    <div v-if="activeTab === 'review' && reviewSubTab === 'reviewed'" v-loading="reviewLoading" class="review-list">
      <div v-for="review in reviews" :key="review.id" class="review-card">
        <div class="review-card__product" @click="$router.push(`/products/${review.productId}`)">
          <ProductImage :src="review.productImage" :seed="(review.productName || '') + review.productId" fit="cover" class="review-card__product-img" />
          <div class="review-card__product-info">
            <div class="review-card__product-name">{{ review.productName || '' }}</div>
            <div class="review-card__product-price">{{ formatPrice(review.productPrice ?? 0) }}</div>
          </div>
        </div>
        <div class="review-card__body">
          <div class="review-card__rating">
            <el-rate :model-value="review.rating" disabled :max="5" size="small" />
            <span class="review-card__rating-text">{{ review.rating }} 分</span>
            <span class="review-card__date">{{ formatDate(review.createTime) }}</span>
          </div>
          <div class="review-card__content">{{ review.content }}</div>
          <div v-if="review.images && review.images.length" class="review-card__images">
            <el-image
              v-for="(img, i) in review.images"
              :key="i"
              :src="img"
              fit="cover"
              :preview-src-list="review.images"
              :initial-index="i"
              class="review-card__image-item"
            />
          </div>
        </div>
        <!-- 追评列表 -->
        <div v-if="review.followUpReviews?.length" class="review-card__followups">
          <div v-for="fu in review.followUpReviews" :key="fu.id" class="followup-item">
            <div class="followup-item__header">
              <span class="followup-item__label">{{ daysAfter(review.createTime, fu.createTime) }}天后追评</span>
              <span class="followup-item__date">{{ formatDate(fu.createTime) }}</span>
            </div>
            <div class="followup-item__content">{{ fu.content }}</div>
            <div v-if="fu.images && fu.images.length" class="review-card__images followup-item__images">
              <el-image
                v-for="(img, i) in fu.images"
                :key="i"
                :src="img"
                fit="cover"
                :preview-src-list="fu.images"
                :initial-index="i"
                class="review-card__image-item"
              />
            </div>
          </div>
        </div>

        <div class="review-card__actions">
          <span class="review-card__action-btn" :class="{ 'is-active': review.isLiked }" @click.stop="handleToggleLike(review)">
            <el-icon><CaretTop /></el-icon>
            <span>{{ review.likeCount || 0 }}</span>
          </span>
          <span class="review-card__action-btn" @click.stop="handleToggleComments(review)">
            <el-icon><ChatDotSquare /></el-icon>
            <span>{{ review.commentCount || 0 }}</span>
          </span>
          <el-button
            size="small"
            type="warning"
            @click.stop="handleFollowUp(review)"
          >
            追评
          </el-button>
        </div>
        <div v-if="commentsVisible[review.id]" class="review-card__comments">
          <div class="review-card__comments-list" v-if="commentMap[review.id]?.length">
            <div v-for="c in commentMap[review.id]" :key="c.id" class="comment-item">
              <span class="comment-item__user">{{ c.username }}</span>
              <span class="comment-item__content">{{ c.content }}</span>
              <span class="comment-item__time">{{ formatDate(c.createTime) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无评论" :image-size="40" />
          <div class="comment-input-row">
            <el-input
              v-model="commentInput[review.id]"
              placeholder="写评论..."
              size="small"
              maxlength="200"
              @keyup.enter="handleAddComment(review)"
            />
            <el-button size="small" type="primary" :loading="commentSubmitting[review.id]" @click.stop="handleAddComment(review)">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="activeTab === 'review' && reviewSubTab === 'reviewed' && !reviewLoading && !reviews.length" description="暂无已评价的商品" />

    <!-- 评价-已评价分页 -->
    <div class="pagination-wrap" v-if="activeTab === 'review' && reviewSubTab === 'reviewed' && reviewTotal > 0">
      <el-pagination
        v-model:current-page="reviewPage"
        v-model:page-size="reviewPageSize"
        :total="reviewTotal"
        :page-sizes="[10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadReviews"
        @size-change="loadReviews"
      />
    </div>

    <div v-if="activeTab !== 'review' || reviewSubTab !== 'reviewed'" v-loading="loading" class="order-list" :class="{ 'order-list--review': activeTab === 'review' }">
      <div v-for="order in orders" :key="order.id" class="order-card" @click="activeTab === 'refund' && order.refundStatus != null ? $router.push(`/orders/${order.id}/refund`) : $router.push(`/orders/${order.id}`)">
        <div class="order-card__header">
          <span class="order-card__no">订单号：{{ order.orderNo }}</span>
          <span class="order-card__time">{{ formatDate(order.createTime) }}</span>
          <el-tag :type="ORDER_STATUS_COLOR[order.status]" effect="dark" size="small">
            {{ order.statusText }}
          </el-tag>
          <el-tag
            v-if="activeTab === 'refund' && order.refundStatus != null"
            :type="REFUND_STATUS_COLOR[order.refundStatus]"
            effect="plain"
            size="small"
            style="margin-left: 6px"
          >
            {{ order.refundStatusText }}
          </el-tag>
        </div>
        <div class="order-card__body">
          <div class="order-card__items">
            <div v-for="item in order.items" :key="item.id" class="order-card__item">
              <div class="order-card__item-img">
                <ProductImage :src="item.productImage" :seed="item.productName + item.productId" fit="cover" />
              </div>
              <div class="order-card__item-info">
                <div class="order-card__item-name">{{ item.productName }}</div>
                <div class="order-card__item-spec" v-if="item.specDesc">{{ item.specDesc }}</div>
              </div>
              <div class="order-card__item-price">{{ formatPrice(item.price) }}</div>
              <div class="order-card__item-qty">x{{ item.quantity }}</div>
            </div>
          </div>
        </div>
        <div class="order-card__footer" @click.stop>
          <div class="order-card__footer-left">
            <span v-if="activeTab === 'review' && order._reviewInfo" class="order-card__review-info">
              已评价 {{ order._reviewInfo.reviewed }} / {{ order._reviewInfo.total }} 件
            </span>
            <span v-else>
              共 {{ order.items.length }} 件商品，合计：<strong>{{ formatPrice(order.totalAmount) }}</strong>
            </span>
          </div>
          <div class="order-card__actions">
            <!-- 待付款 -->
            <el-button v-if="order.status === 0" type="primary" size="small" @click="handlePay(order.id)">去支付</el-button>
            <el-button v-if="order.status === 0" size="small" @click="handleCancel(order.id)">取消</el-button>
            <!-- 待发货 -->
            <el-button v-if="order.status === 1" size="small" @click="handleEditAddress(order)">修改地址</el-button>
            <el-button
              v-if="canRefund(order)"
              size="small"
              @click="handleRefund(order)"
            >{{ refundButtonText(order) }}</el-button>
            <!-- 已完成 -->
            <el-button v-if="order.status === 3" size="small" @click="handleReorder(order.id)">再来一单</el-button>
            <!-- 已取消 -->
            <el-button v-if="order.status === 4" size="small" @click="handleReorder(order.id)">加入购物车</el-button>
            <!-- 待收货 -->
            <el-button v-if="order.status === 2 || order.status === 3" size="small" @click="handleViewLogistics(order.id)">查看物流</el-button>
            <el-button v-if="order.status === 2" type="success" size="small" @click="handleConfirm(order.id)">确认收货</el-button>
            <!-- 评价 tab — 待评价 -->
            <el-button v-if="activeTab === 'review' && reviewSubTab === 'pending'" type="warning" size="small" @click="$router.push(`/orders/${order.id}`)">评价</el-button>
            <!-- 评价 tab — 已评价 -->
            <el-button v-if="activeTab === 'review' && reviewSubTab === 'reviewed'" type="warning" size="small" @click="$router.push(`/orders/${order.id}`)">追加评价</el-button>
            <el-button size="small" @click="$router.push(`/orders/${order.id}`)">详情</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !orders.length" description="暂无订单" />
    </div>

    <!-- 非评价tab & 待评价：底部分页 -->
    <div class="pagination-wrap" v-if="activeTab !== 'review' || reviewSubTab !== 'reviewed'">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 40]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadOrders"
        @size-change="loadOrders"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { CaretTop, ChatDotSquare } from '@element-plus/icons-vue'
import { getOrderList, cancelOrder, confirmReceive, reorderOrder } from '@/api/order'
import { getMyReviews, likeReview, unlikeReview, getReviewComments, addReviewComment } from '@/api/product'
import { formatPrice, formatDate } from '@/utils/format'
import { ORDER_STATUS_COLOR, REFUND_STATUS_MAP, REFUND_STATUS_COLOR } from '@shared/constants'
import type { Order } from '@shared/types/order'
import type { Review, ReviewComment } from '@shared/types/product'
import ProductImage from '@/components/common/ProductImage.vue'

const router = useRouter()
const route = useRoute()
const orders = ref<Order[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const activeTab = ref('')
const reviewSubTab = ref('pending')

// 评价列表状态
const reviews = ref<Review[]>([])
const reviewLoading = ref(false)
const reviewPage = ref(1)
const reviewPageSize = ref(10)
const reviewTotal = ref(0)
const commentsVisible = ref<Record<number, boolean>>({})
const commentMap = ref<Record<number, ReviewComment[]>>({})
const commentInput = ref<Record<number, string>>({})
const commentSubmitting = ref<Record<number, boolean>>({})

async function loadOrders() {
  // "已评价"子标签走评价列表接口
  if (activeTab.value === 'review' && reviewSubTab.value === 'reviewed') {
    await loadReviews()
    return
  }
  loading.value = true
  try {
    const status = activeTab.value === 'review' ? 3
      : activeTab.value ? Number(activeTab.value) : undefined
    const reviewFilter = activeTab.value === 'review' ? reviewSubTab.value : undefined

    const hasRefund = activeTab.value === 'refund'
    const res = await getOrderList({
      page: page.value,
      pageSize: pageSize.value,
      status: hasRefund ? undefined : status,
      hasRefund: hasRefund || undefined,
      reviewFilter,
    })
    orders.value = res.data.records.map(enrichReviewInfo)
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadReviews() {
  reviewLoading.value = true
  try {
    const res = await getMyReviews(reviewPage.value, reviewPageSize.value)
    reviews.value = res.data.records
    reviewTotal.value = res.data.total
  } finally {
    reviewLoading.value = false
  }
}

function enrichReviewInfo(order: Order): Order & { _reviewInfo?: { reviewed: number; total: number } } {
  if (activeTab.value !== 'review') return order as Order & { _reviewInfo?: any }
  const total = order.items?.length || 0
  const reviewed = order.reviewCount ?? 0
  return { ...order, _reviewInfo: { reviewed, total } }
}

function handleTabChange() {
  page.value = 1
  reviewPage.value = 1
  if (activeTab.value === 'review') {
    reviewSubTab.value = 'pending'
  }
  loadOrders()
}

function handleSubTabChange() {
  page.value = 1
  reviewPage.value = 1
  loadOrders()
}

async function handleToggleLike(review: Review) {
  try {
    if (review.isLiked) {
      await unlikeReview(review.id)
      review.isLiked = false
      review.likeCount = Math.max((review.likeCount || 1) - 1, 0)
    } else {
      await likeReview(review.id)
      review.isLiked = true
      review.likeCount = (review.likeCount || 0) + 1
    }
  } catch { /* handled by interceptor */ }
}

async function handleToggleComments(review: Review) {
  if (!review.commentCount) return
  const id = review.id
  if (commentsVisible.value[id]) {
    commentsVisible.value[id] = false
    return
  }
  commentsVisible.value[id] = true
  if (!commentMap.value[id]) {
    try {
      const res = await getReviewComments(id)
      commentMap.value[id] = res.data
    } catch {
      commentMap.value[id] = []
    }
  }
  if (commentInput.value[id] === undefined) {
    commentInput.value[id] = ''
  }
}

async function handleAddComment(review: Review) {
  const content = (commentInput.value[review.id] || '').trim()
  if (!content) return
  commentSubmitting.value[review.id] = true
  try {
    await addReviewComment(review.id, content)
    commentInput.value[review.id] = ''
    review.commentCount = (review.commentCount || 0) + 1
    ElMessage.success('评论成功')
    const res = await getReviewComments(review.id)
    commentMap.value[review.id] = res.data
  } finally {
    commentSubmitting.value[review.id] = false
  }
}

function daysAfter(from: string, to: string): number {
  const diff = new Date(to).getTime() - new Date(from).getTime()
  return Math.max(1, Math.round(diff / 86400000))
}

function handleFollowUp(review: Review) {
  router.push(`/orders/${review.orderId}/review/${review.productId}?followUp=1`)
}

async function handleCancel(id: number) {
  await ElMessageBox.confirm('确定要取消该订单吗？', '提示', { type: 'warning' })
  await cancelOrder(id)
  ElMessage.success('订单已取消')
  loadOrders()
}

function canRefund(order: Order): boolean {
  const status = order.status
  if (status !== 1 && status !== 2 && status !== 3) return false
  if (order.refundStatus != null && order.refundStatus !== 1 && order.refundStatus !== 3) return false
  return true
}

function refundButtonText(order: Order): string {
  if (order.refundStatus === 1 || order.refundStatus === 3) return '再次申请'
  return '申请退款'
}

function handleRefund(order: Order) {
  if (order.refundStatus != null && order.refundStatus !== 1 && order.refundStatus !== 3) {
    router.push(`/orders/${order.id}/refund`)
  } else {
    router.push(`/orders/${order.id}/refund/apply`)
  }
}

async function handleReorder(id: number) {
  const res = await reorderOrder(id)
  if (res.data) {
    ElMessage.success('下单成功，即将跳转支付页面')
    router.push(`/orders/${res.data.id}/pay`)
  } else {
    ElMessage.warning('部分商品库存不足，已加入购物车')
    router.push('/cart')
  }
}

async function handleConfirm(id: number) {
  await ElMessageBox.confirm('确认已收到商品吗？', '提示', { type: 'warning' })
  await confirmReceive(id)
  ElMessage.success('已确认收货')
  loadOrders()
}

function handlePay(id: number) {
  router.push(`/orders/${id}/pay`)
}

function handleEditAddress(order: Order) {
  if (order.addressModified) {
    ElMessage.warning('您已经修改过地址啦')
    return
  }
  router.push(`/orders/${order.id}/edit-address`)
}

function handleViewLogistics(id: number) {
  router.push(`/orders/${id}`)
}

onMounted(() => {
  if (route.query.tab) {
    activeTab.value = route.query.tab as string
  }
  if (route.query.subTab) {
    reviewSubTab.value = route.query.subTab as string
  }
  loadOrders()
})
</script>

<style lang="scss" scoped>
.order-list-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 20px;
}

.order-tabs {
  background: var(--bg1);
  padding: 4px 20px 0;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  margin-bottom: 0;
  border: 1px solid var(--line-light);
  border-bottom: none;
}

.review-subtabs {
  background: var(--bg2);
  padding: 12px 20px;
  border-bottom: 1px solid var(--line-light);
}

.order-list {
  background: var(--bg1);
  border-radius: 0 0 var(--radius-sm) var(--radius-sm);
  border: 1px solid var(--line-light);
  border-top: none;
  padding: 0 20px 20px;

  &--review {
    border-radius: 0 0 12px 12px;
  }
}

.order-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  margin-top: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow .2s;

  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,.06); }

  &__header {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 16px;
    background: #fafafa;
    font-size: 13px;
    color: #666;
  }

  &__time { flex: 1; color: #999; }

  &__body { padding: 0 16px; }

  &__item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #f8f8f8;

    &:last-child { border-bottom: none; }

    &-img {
      width: 60px;
      height: 60px;
      border-radius: 6px;
      overflow: hidden;
      flex-shrink: 0;
    }

    &-info { flex: 1; min-width: 0; }

    &-name {
      font-size: 14px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &-spec {
      font-size: 12px;
      color: #999;
      margin-top: 2px;
    }

    &-price { font-size: 14px; font-weight: 600; }
    &-qty   { font-size: 13px; color: #999; }
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-top: 1px solid #f5f5f5;

    strong { color: #e6423a; font-size: 16px; }

    &-left {
      font-size: 13px;
      color: #666;
    }
  }

  &__review-info {
    color: #409eff;
    font-weight: 500;
    font-size: 13px;
  }

  &__actions {
    display: flex;
    gap: 8px;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

// 追评区域
.review-card__followups {
  margin: 0 16px 12px;
  border-left: 3px solid #e6a23c;
  padding-left: 14px;
}

.followup-item {
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;

  &:last-child { border-bottom: none; }

  &__header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;
  }

  &__label {
    font-size: 13px;
    color: #e6a23c;
    font-weight: 500;
  }

  &__date {
    font-size: 12px;
    color: #bbb;
  }

  &__content {
    font-size: 14px;
    line-height: 1.7;
    color: #333;
    white-space: pre-wrap;
    word-break: break-word;
  }

  &__images {
    margin-top: 8px;
  }
}

// 已评价列表
.review-list {
  background: var(--bg1);
  border-radius: 0 0 var(--radius-sm) var(--radius-sm);
  border: 1px solid var(--line-light);
  border-top: none;
  padding: 0 20px;
}

.review-card {
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child { border-bottom: none; }

  &__product {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    cursor: pointer;
  }

  &__product-img {
    width: 64px;
    height: 64px;
    border-radius: 6px;
    overflow: hidden;
    flex-shrink: 0;
  }

  &__product-info { flex: 1; min-width: 0; }

  &__product-name {
    font-size: 15px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__product-price {
    font-size: 14px;
    color: #e6423a;
    font-weight: 600;
    margin-top: 4px;
  }

  &__body { margin-bottom: 12px; }

  &__rating {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__rating-text { font-size: 13px; color: #e6423a; font-weight: 600; }

  &__date { font-size: 12px; color: #999; margin-left: auto; }

  &__content {
    font-size: 14px;
    line-height: 1.7;
    color: #333;
    white-space: pre-wrap;
    word-break: break-word;
  }

  &__images {
    display: flex;
    gap: 8px;
    margin-top: 10px;
    flex-wrap: wrap;
  }

  &__image-item {
    width: 80px;
    height: 80px;
    border-radius: 4px;
    overflow: hidden;
    cursor: pointer;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__action-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #999;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 4px;
    transition: all .2s;

    &:hover { color: #409eff; background: #ecf5ff; }

    &.is-active { color: #409eff; }
  }

  &__comments {
    margin-top: 14px;
    padding: 14px;
    background: #fafafa;
    border-radius: 8px;
  }

  &__comments-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 12px;
    max-height: 240px;
    overflow-y: auto;
  }
}

.comment-item {
  font-size: 13px;
  line-height: 1.6;

  &__user { color: #409eff; font-weight: 500; margin-right: 8px; }
  &__content { color: #333; }
  &__time { color: #ccc; margin-left: 8px; font-size: 11px; }
}

.comment-input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
