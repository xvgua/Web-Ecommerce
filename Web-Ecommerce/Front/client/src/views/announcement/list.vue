<template>
  <div class="announcement-page">
    <h2 class="page-title">平台公告</h2>
    <div v-loading="loading">
      <div v-if="!loading && list.length === 0" class="empty">
        <el-empty description="暂无公告" />
      </div>
      <div v-else class="list">
        <div
          v-for="item in list"
          :key="item.id"
          class="item"
          :class="`item--${item.level}`"
          @click="handleClick(item)"
        >
          <div class="item__header">
            <span class="item__level-tag">{{ levelLabel(item.level) }}</span>
            <span class="item__title">{{ item.title }}</span>
            <span class="item__date">{{ item.createTime?.slice(0, 10) }}</span>
          </div>
          <p class="item__content">{{ item.content?.slice(0, 120) }}{{ item.content?.length > 120 ? '...' : '' }}</p>
        </div>
      </div>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchList"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="selected?.title" width="560px" center>
      <div class="detail">
        <div class="detail__meta">
          <span class="detail__level-tag">{{ levelLabel(selected?.level) }}</span>
          {{ selected?.createTime }}
        </div>
        <div class="detail__content">{{ selected?.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAnnouncementPage } from '@/api/announcement'
import type { Announcement } from '@shared/types'

const list = ref<Announcement[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const dialogVisible = ref(false)
const selected = ref<Announcement | null>(null)

function levelLabel(l?: string) {
  return { info: '普通', warning: '提醒', important: '重要' }[l || 'info'] || l
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getAnnouncementPage({ page: page.value, pageSize })
    list.value = res.data.records
    total.value = res.data.total
  } catch { /* handled by interceptor */ }
  finally { loading.value = false }
}

function handleClick(item: Announcement) {
  selected.value = item
  dialogVisible.value = true
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.announcement-page {
  max-width: 860px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 24px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item {
  background: #fff;
  border-radius: 10px;
  padding: 18px 24px;
  cursor: pointer;
  transition: box-shadow .2s, transform .2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, .04);
  border-left: 4px solid transparent;

  &:hover {
    box-shadow: 0 4px 14px rgba(0, 0, 0, .06);
    transform: translateY(-1px);
  }

  &--info    { border-left-color: #409eff; }
  &--warning { border-left-color: #e6a23c; }
  &--important { border-left-color: #f56c6c; }

  &__header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }

  &__level-tag {
    font-size: 11px;
    padding: 1px 8px;
    border-radius: 4px;
    flex-shrink: 0;
    font-weight: 500;
  }

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    flex: 1;
  }

  &__date {
    font-size: 12px;
    color: #c0c4cc;
    flex-shrink: 0;
  }

  &__content {
    font-size: 13px;
    color: #909399;
    line-height: 1.6;
    margin: 0;
    padding-left: 0;
  }
}

.item--info .item__level-tag    { background: #ecf5ff; color: #409eff; }
.item--warning .item__level-tag { background: #fdf6ec; color: #e6a23c; }
.item--important .item__level-tag { background: #fef0f0; color: #f56c6c; }

.empty {
  padding: 60px 0;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.detail {
  &__meta {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #909399;
    margin-bottom: 16px;
  }

  &__level-tag {
    font-size: 11px;
    padding: 1px 8px;
    border-radius: 4px;
    background: #ecf5ff;
    color: #409eff;
    font-weight: 500;
  }

  &__content {
    font-size: 14px;
    color: #303133;
    line-height: 1.8;
    white-space: pre-wrap;
  }
}

@media (max-width: 768px) {
  .announcement-page {
    padding: 0 8px;
  }

  .item {
    padding: 14px 16px;

    &__header {
      flex-wrap: wrap;
      gap: 6px;
    }

    &__title {
      font-size: 14px;
      flex: 1 1 100%;
      order: 1;
    }

    &__date {
      order: 2;
      margin-left: auto;
    }
  }
}
</style>
