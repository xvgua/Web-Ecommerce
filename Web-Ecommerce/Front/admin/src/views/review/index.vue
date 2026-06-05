<template>
  <div class="review-manage">
    <h1 class="page-title">评价管理</h1>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称"
        clearable
        class="toolbar-search"
        @keyup.enter="handleSearch"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-input
        v-model="username"
        placeholder="搜索用户名称"
        clearable
        class="toolbar-search"
        @keyup.enter="handleSearch"
      >
        <template #prefix><el-icon><User /></el-icon></template>
      </el-input>
      <el-select
        v-model="rating"
        placeholder="评分筛选"
        clearable
        class="toolbar-select"
        @change="handleSearch"
      >
        <el-option label="1星" :value="1" />
        <el-option label="2星" :value="2" />
        <el-option label="3星" :value="3" />
        <el-option label="4星" :value="4" />
        <el-option label="5星" :value="5" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        class="toolbar-date"
        @change="handleSearch"
      />
      <el-select
        v-model="hasImage"
        placeholder="图片筛选"
        clearable
        class="toolbar-select"
        @change="handleSearch"
      >
        <el-option label="有图" :value="true" />
        <el-option label="无图" :value="false" />
      </el-select>
      <el-select
        v-model="hasFollowUp"
        placeholder="追评筛选"
        clearable
        class="toolbar-select"
        @change="handleSearch"
      >
        <el-option label="有追评" :value="true" />
        <el-option label="无追评" :value="false" />
      </el-select>
      <el-select
        v-model="sort"
        class="toolbar-select"
        @change="handleSearch"
      >
        <el-option label="最新发布" value="newest" />
        <el-option label="最多点赞" value="most_liked" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="action-bar" v-if="selectedIds.length > 0">
      <el-button type="danger" @click="handleBatchDelete">
        批量删除 ({{ selectedIds.length }})
      </el-button>
    </div>

    <div class="table-card">
      <el-table
        :data="reviews"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" align="center" />
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="orderId" label="订单号" width="100" align="center" />
        <el-table-column label="商品信息" width="220">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image
                :src="row.productImage"
                class="product-thumb"
                fit="cover"
              >
                <template #error>
                  <div class="product-thumb-placeholder">
                    <el-icon><Goods /></el-icon>
                  </div>
                </template>
              </el-image>
              <span class="product-name" :title="row.productName">{{ row.productName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="24" :src="row.avatar">
                <span style="font-size:12px">{{ row.username?.[0] || 'U' }}</span>
              </el-avatar>
              <span class="user-name">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="150" align="center">
          <template #default="{ row }">
            <div class="rating-cell">
              <el-rate :model-value="row.rating" disabled show-score size="small" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="200">
          <template #default="{ row }">
            <div class="content-cell">
              <span class="content-text">{{ truncateText(row.content, 80) }}</span>
              <el-tag v-if="row.hasFollowUp" type="warning" size="small" class="followup-tag">有追评</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="100" align="center">
          <template #default="{ row }">
            <div class="images-cell" v-if="row.images && row.images.length > 0">
              <el-image
                v-for="(img, idx) in row.images.slice(0, 3)"
                :key="idx"
                :src="img"
                class="review-thumb"
                fit="cover"
                :preview-src-list="row.images"
                :initial-index="idx"
              >
                <template #error>
                  <div class="review-thumb-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
            <span v-else class="no-data">-</span>
          </template>
        </el-table-column>
        <el-table-column label="点赞" width="70" align="center">
          <template #default="{ row }">
            <span>{{ row.likeCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评论" width="70" align="center">
          <template #default="{ row }">
            <span>{{ row.commentCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发表时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDetail(row.id)">详情</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadList"
        @size-change="loadList"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="评价详情" width="680px">
      <div v-if="detail" class="review-detail" v-loading="detailLoading">
        <div class="detail-header">
          <div class="detail-product">
            <el-image
              :src="detail.productImage"
              class="detail-product-img"
              fit="cover"
            >
              <template #error>
                <div class="detail-product-img-placeholder">
                  <el-icon><Goods /></el-icon>
                </div>
              </template>
            </el-image>
            <div>
              <div class="detail-product-name">{{ detail.productName || '-' }}</div>
              <div class="detail-product-price" v-if="detail.productPrice">
                {{ formatPrice(detail.productPrice) }}
              </div>
            </div>
          </div>
          <div class="detail-user">
            <el-avatar :size="32" :src="detail.avatar">
              <span>{{ detail.username?.[0] || 'U' }}</span>
            </el-avatar>
            <div>
              <div class="detail-username">{{ detail.username }}</div>
              <div class="detail-order">订单号：{{ detail.orderId }}</div>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="detail-ratings">
          <div class="detail-overall">
            <span class="detail-label">综合评分</span>
            <el-rate :model-value="detail.rating" disabled show-score />
          </div>
          <div class="detail-sub-ratings">
            <span><span class="detail-label">描述</span> {{ detail.ratingDesc || '-' }}</span>
            <span><span class="detail-label">物流</span> {{ detail.ratingLogistics || '-' }}</span>
            <span><span class="detail-label">服务</span> {{ detail.ratingService || '-' }}</span>
          </div>
        </div>

        <div class="detail-content">
          <div class="detail-label">评价内容</div>
          <p>{{ detail.content || '（无文字评价）' }}</p>
          <div v-if="detail.images && detail.images.length > 0" class="detail-images">
            <el-image
              v-for="(img, idx) in detail.images"
              :key="idx"
              :src="img"
              class="detail-image-item"
              fit="cover"
              :preview-src-list="detail.images"
              :initial-index="idx"
            />
          </div>
        </div>

        <div class="detail-meta">
          点赞 {{ detail.likeCount || 0 }} · 评论 {{ detail.commentCount || 0 }} · 发表于 {{ formatDate(detail.createTime) }}
        </div>

        <!-- 追评 -->
        <template v-if="detail.followUpReviews && detail.followUpReviews.length > 0">
          <el-divider />
          <div class="followup-section">
            <h4>追评</h4>
            <div v-for="fu in detail.followUpReviews" :key="fu.id" class="followup-item">
              <p>{{ fu.content || '（无文字）' }}</p>
              <div v-if="fu.images && fu.images.length > 0" class="detail-images">
                <el-image
                  v-for="(img, idx) in fu.images"
                  :key="idx"
                  :src="img"
                  class="detail-image-item"
                  fit="cover"
                  :preview-src-list="fu.images"
                  :initial-index="idx"
                />
              </div>
              <span class="followup-time">{{ formatDate(fu.createTime) }}</span>
            </div>
          </div>
        </template>

        <!-- 评论列表 -->
        <template v-if="detail.comments && detail.comments.length > 0">
          <el-divider />
          <div class="comments-section">
            <h4>评论 ({{ detail.comments.length }})</h4>
            <div v-for="c in detail.comments" :key="c.id" class="comment-item">
              <div class="comment-header">
                <el-avatar :size="20" :src="c.avatar">
                  <span style="font-size:10px">{{ c.username?.[0] || 'U' }}</span>
                </el-avatar>
                <span class="comment-username">{{ c.username }}</span>
                <span class="comment-time">{{ formatDate(c.createTime) }}</span>
              </div>
              <p class="comment-content">{{ c.content }}</p>
            </div>
          </div>
        </template>
      </div>
    </el-dialog>

    <!-- 批量删除确认 -->
    <el-dialog v-model="batchDeleteVisible" title="批量删除评价" width="480px">
      <p>确定要删除选中的 <strong>{{ selectedIds.length }}</strong> 条评价吗？</p>
      <p class="delete-warn">删除后不可恢复，关联的追评、评论和点赞数据也会一并删除。</p>
      <template #footer>
        <el-button @click="batchDeleteVisible = false">取消</el-button>
        <el-button type="danger" :loading="batchDeleteLoading" @click="confirmBatchDelete">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, User, Goods, Picture } from '@element-plus/icons-vue'
import { formatPrice, formatDate } from '@/utils/format'
import {
  getReviewList,
  getReviewDetail,
  deleteReview,
  batchDeleteReviews,
} from '@/api/admin'
import type { Review } from '@shared/types/product'

const reviews = ref<Review[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const keyword = ref('')
const username = ref('')
const rating = ref<number | undefined>(undefined)
const dateRange = ref<[string, string] | null>(null)
const hasImage = ref<boolean | undefined>(undefined)
const hasFollowUp = ref<boolean | undefined>(undefined)
const sort = ref<string>('newest')

const selectedIds = ref<number[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<Review | null>(null)

const batchDeleteVisible = ref(false)
const batchDeleteLoading = ref(false)

function truncateText(text: string, max: number): string {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '…' : text
}

async function loadList() {
  loading.value = true
  try {
    const res = await getReviewList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      username: username.value || undefined,
      rating: rating.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      hasImage: hasImage.value,
      hasFollowUp: hasFollowUp.value,
      sort: sort.value,
    })
    reviews.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadList()
}

function handleSelectionChange(selection: Review[]) {
  selectedIds.value = selection.map((r) => r.id)
}

async function showDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getReviewDetail(id).then((res) => res.data)
  } finally {
    detailLoading.value = false
  }
}

function handleDelete(row: Review) {
  const summary = `确定要删除用户「${row.username}」对「${row.productName || '商品'}」的评价吗？`
  ElMessageBox.confirm(summary, '删除评价', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
  }).then(async () => {
    await deleteReview(row.id)
    ElMessage.success('评价已删除')
    loadList()
  }).catch(() => {})
}

function handleBatchDelete() {
  batchDeleteVisible.value = true
}

async function confirmBatchDelete() {
  batchDeleteLoading.value = true
  try {
    await batchDeleteReviews(selectedIds.value)
    ElMessage.success(`已删除 ${selectedIds.value.length} 条评价`)
    batchDeleteVisible.value = false
    selectedIds.value = []
    loadList()
  } finally {
    batchDeleteLoading.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style lang="scss" scoped>
.review-manage { max-width: 1400px; }

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;

  &-search { width: 200px; }
  &-select { width: 130px; }
  &-date { width: 260px; }
}

.action-bar {
  margin-bottom: 12px;
}

.table-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, .04);
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-thumb {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  flex-shrink: 0;
}

.product-thumb-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.product-name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rating-cell {
  display: flex;
  justify-content: center;
}

.content-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.content-text {
  font-size: 13px;
  color: #555;
  line-height: 1.4;
}

.followup-tag {
  align-self: flex-start;
}

.images-cell {
  display: flex;
  gap: 4px;
  justify-content: center;
}

.review-thumb {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  cursor: pointer;
}

.review-thumb-placeholder {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 12px;
}

.no-data {
  color: #ccc;
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ── Detail Dialog ── */
.review-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
  }

  .detail-product {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .detail-product-img {
    width: 64px;
    height: 64px;
    border-radius: 8px;
    flex-shrink: 0;
  }

  .detail-product-img-placeholder {
    width: 64px;
    height: 64px;
    border-radius: 8px;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ccc;
  }

  .detail-product-name {
    font-size: 15px;
    font-weight: 600;
  }

  .detail-product-price {
    font-size: 14px;
    color: #e6423a;
    font-weight: 500;
    margin-top: 4px;
  }

  .detail-user {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-shrink: 0;
  }

  .detail-username {
    font-size: 14px;
    font-weight: 500;
  }

  .detail-order {
    font-size: 12px;
    color: #999;
    margin-top: 2px;
  }

  .detail-ratings {
    display: flex;
    gap: 40px;
    align-items: center;
    margin: 12px 0;
  }

  .detail-overall {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .detail-sub-ratings {
    display: flex;
    gap: 20px;
    font-size: 13px;
    color: #666;
  }

  .detail-label {
    font-size: 13px;
    color: #999;
    margin-right: 4px;
  }

  .detail-content {
    margin: 16px 0;

    .detail-label {
      display: block;
      margin-bottom: 8px;
      font-weight: 500;
      color: #333;
    }

    p {
      font-size: 14px;
      line-height: 1.6;
      color: #444;
      white-space: pre-wrap;
      word-break: break-word;
    }
  }

  .detail-images {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin-top: 10px;
  }

  .detail-image-item {
    width: 90px;
    height: 90px;
    border-radius: 6px;
    cursor: pointer;
  }

  .detail-meta {
    font-size: 12px;
    color: #aaa;
  }

  .followup-section, .comments-section {
    h4 {
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 12px;
    }
  }

  .followup-item {
    padding: 12px;
    background: #f9f9f9;
    border-radius: 8px;
    margin-bottom: 10px;

    p {
      font-size: 14px;
      line-height: 1.5;
      color: #444;
      white-space: pre-wrap;
      word-break: break-word;
      margin: 0;
    }

    .detail-images {
      margin: 8px 0;
    }
  }

  .followup-time {
    font-size: 12px;
    color: #aaa;
  }

  .comment-item {
    padding: 10px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child { border-bottom: none; }
  }

  .comment-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  .comment-username {
    font-size: 13px;
    font-weight: 500;
  }

  .comment-time {
    font-size: 12px;
    color: #aaa;
    margin-left: auto;
  }

  .comment-content {
    font-size: 13px;
    color: #555;
    line-height: 1.5;
    margin: 0 0 0 28px;
    white-space: pre-wrap;
    word-break: break-word;
  }
}

.delete-warn {
  font-size: 13px;
  color: #999;
  margin-top: 8px;
}
</style>
