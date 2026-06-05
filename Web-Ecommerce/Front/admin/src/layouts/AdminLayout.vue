<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-sidebar__brand">
        <svg viewBox="0 0 32 32" width="28" height="28" xmlns="http://www.w3.org/2000/svg">
          <rect width="32" height="32" rx="8" fill="rgba(255,255,255,.15)" />
          <text x="16" y="22" text-anchor="middle" font-size="16" fill="#fff">&#x1F6E1;</text>
        </svg>
        <span>管理后台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,.65)"
        active-text-color="#fff"
        class="admin-menu"
      >
        <el-menu-item index="/dashboard">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>数据看板</span>
          </template>
        </el-menu-item>

        <el-sub-menu index="product-group">
          <template #title>
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </template>
          <el-menu-item index="/products">商品列表</el-menu-item>
          <el-menu-item index="/products/create">新增商品</el-menu-item>
          <el-menu-item index="/categories">分类管理</el-menu-item>
          <el-menu-item index="/reviews">评价管理</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>

        <el-menu-item index="/refunds">
          <el-icon><Money /></el-icon>
          <span>退款管理</span>
        </el-menu-item>

        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item index="/coupons">
          <el-icon><Ticket /></el-icon>
          <span>优惠券管理</span>
        </el-menu-item>

        <el-menu-item index="/seckill">
          <el-icon><AlarmClock /></el-icon>
          <span>秒杀活动</span>
        </el-menu-item>

        <el-menu-item index="/hot-keywords">
          <el-icon><TrendCharts /></el-icon>
          <span>热门搜索词</span>
        </el-menu-item>

        <el-sub-menu index="cs-group">
          <template #title>
            <el-icon><ChatDotSquare /></el-icon>
            <span>客服管理</span>
          </template>
          <el-menu-item index="/customer-service">在线客服</el-menu-item>
          <el-menu-item index="/customer-service/quick-replies">快捷回复</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system-group">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/banners">轮播管理</el-menu-item>
          <el-menu-item index="/system/announcements">公告管理</el-menu-item>
          <el-menu-item index="/system/feedbacks">用户反馈</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <el-breadcrumb>
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="admin-header__right">
          <el-badge :value="0" :hidden="true">
            <el-button circle :icon="Bell" />
          </el-badge>
          <el-dropdown>
            <span class="admin-user">
              <el-avatar :size="32">
                <span style="font-size:14px">{{ adminStore.username?.[0]?.toUpperCase() || 'A' }}</span>
              </el-avatar>
              <span class="admin-user__name">{{ adminStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataAnalysis, Goods, Document, User, Ticket, ChatDotSquare,
  Setting, Bell, ArrowDown, TrendCharts, AlarmClock, Money,
} from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const activeMenu = computed(() => route.path)

function handleLogout() {
  adminStore.logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

/* ── Sidebar ── */
.admin-sidebar {
  width: 230px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;

  &__brand {
    height: 60px;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 18px;
    color: #fff;
    font-size: 16px;
    font-weight: 600;
    border-bottom: 1px solid rgba(255,255,255,.08);
    letter-spacing: .5px;
  }
}

.admin-menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 48px;
    line-height: 48px;
    margin: 2px 8px;
    border-radius: 8px;
    font-size: 14px;

    &:hover {
      background: rgba(255,255,255,.06);
    }
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(135deg, rgba(64,158,255,.3), rgba(64,158,255,.15));
    color: #fff;
    font-weight: 500;
  }

  :deep(.el-sub-menu .el-menu) {
    background: rgba(0,0,0,.15);
    padding: 4px 0;

    .el-menu-item {
      height: 40px;
      line-height: 40px;
      padding-left: 56px;
      font-size: 13px;
    }
  }
}

/* ── Main ── */
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  z-index: 10;

  &__right {
    display: flex;
    align-items: center;
    gap: 14px;
  }
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  &__name { font-size: 14px; color: #555; }
}

.admin-content {
  flex: 1;
  padding: 24px;
  background: #f0f2f5;
  overflow-y: auto;
}
</style>
