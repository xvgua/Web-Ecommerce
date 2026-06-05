<template>
  <div class="admin-layout" :class="{ 'admin-layout--collapsed': collapsed }">
    <aside class="admin-sidebar">
      <div class="admin-sidebar__brand">
        <div class="admin-sidebar__logo" @click="collapsed = false" :title="collapsed ? '展开菜单' : ''">
          <svg viewBox="0 0 36 36" width="36" height="36" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect x="2" y="2" width="32" height="32" rx="10" fill="#3964FE" />
            <path d="M18 11c-3 3-5 7-5 7h4v5h2v-5h4s-2-4-5-7z" fill="#fff" opacity=".95" />
            <circle cx="18" cy="15" r="2" fill="#fff" opacity=".5" />
          </svg>
        </div>
        <transition name="brand-fade">
          <div v-show="!collapsed" class="admin-sidebar__brand-text">
            <span class="admin-sidebar__brand-name">Marketplace</span>
            <span class="admin-sidebar__brand-label">乐购管理后台</span>
          </div>
        </transition>
        <button class="collapse-btn collapse-btn--top" @click="collapsed = !collapsed" :title="collapsed ? '展开菜单' : '收起菜单'">
          <el-icon :size="18">
            <DArrowLeft v-if="!collapsed" />
            <DArrowRight v-else />
          </el-icon>
        </button>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        router
        background-color="transparent"
        text-color="var(--text-secondary)"
        active-text-color="var(--sidebar-text-active)"
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
        <div class="admin-header__left">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="admin-header__right">
          <el-tooltip content="暂无新通知" placement="bottom">
            <button class="header-icon-btn">
              <el-icon :size="18"><Bell /></el-icon>
            </button>
          </el-tooltip>
          <el-dropdown trigger="click" popper-class="user-dropdown">
            <button class="header-user-btn">
              <el-avatar :size="36" class="header-avatar">
                <span>{{ adminStore.username?.[0]?.toUpperCase() || 'A' }}</span>
              </el-avatar>
              <span class="header-user-name">{{ adminStore.username }}</span>
              <el-icon class="header-user-arrow" :size="14"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <div class="content-wrapper">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataAnalysis, Goods, Document, User, Ticket, ChatDotSquare,
  Setting, Bell, ArrowDown, TrendCharts, AlarmClock, Money,
  DArrowLeft, DArrowRight, SwitchButton,
} from '@element-plus/icons-vue'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const collapsed = ref(false)

const activeMenu = computed(() => route.path)

function handleMenuSelect(index: string) {
  router.push(index)
}

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

/* ═══════════════════════════════════════
   Sidebar — light, flat, DeepSeek style
   ═══════════════════════════════════════ */
.admin-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  transition: width var(--duration) var(--ease-in-out);
}

.admin-sidebar__brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  overflow: hidden;
}

.admin-sidebar__logo {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  cursor: pointer;
}

.admin-sidebar__brand-text {
  display: flex;
  flex-direction: column;
  gap: 0;
  white-space: nowrap;
}

.admin-sidebar__brand-name {
  font-size: 15px;
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  line-height: 1.2;
  letter-spacing: 0;
}

.admin-sidebar__brand-label {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: var(--font-weight-strong);
  letter-spacing: 1px;
}

/* ── Menu ── */
.admin-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 8px;
  border-right: none;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 42px;
    line-height: 42px;
    margin: 2px 0;
    border-radius: var(--radius-lg);
    font-size: var(--font-size-m);
    font-weight: var(--font-weight-strong);
    color: var(--text-primary);
    transition: background-color var(--duration) var(--ease-in-out),
                color var(--duration-slow) var(--ease-in-out);
    position: relative;

    &:hover {
      background: var(--sidebar-hover);
      color: var(--text-primary);
    }
  }

  :deep(.el-menu-item.is-active) {
    background: var(--sidebar-active);
    color: var(--sidebar-text-active);
    font-weight: var(--font-weight-strong);

    &::before {
      display: none;
    }
  }

  :deep(.el-sub-menu .el-menu) {
    background: transparent;
    padding: 4px 0;
    margin: 0;

    .el-menu-item {
      height: 38px;
      line-height: 38px;
      padding-left: 56px;
      font-size: var(--font-size-s);
      margin: 1px 0;
      border-radius: var(--radius-lg);
    }
  }
}

.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--border-l1);
  border-radius: var(--radius-md);
  color: var(--text-muted);
  cursor: pointer;
  transition: background-color var(--duration) var(--ease-in-out),
              color var(--duration-slow) var(--ease-in-out);
  font-family: inherit;

  &:hover {
    background: var(--bg-surface-hover);
    color: var(--text-secondary);
  }

  &--top {
    width: 30px;
    height: 30px;
    flex-shrink: 0;
    margin-left: auto;
  }
}

/* ── Collapsed ── */
.admin-layout--collapsed .admin-sidebar {
  width: 64px;
}

.brand-fade-enter-active,
.brand-fade-leave-active {
  transition: opacity 180ms ease;
}
.brand-fade-enter-from,
.brand-fade-leave-to {
  opacity: 0;
}

/* ═══════════════════════════════════════
   Main Area
   ═══════════════════════════════════════ */
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-base);
}

/* ── Header — flat, light, border-bottom ── */
.admin-header {
  height: 56px;
  background: var(--sidebar-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid var(--border-l2);
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 20;

  &__left {
    display: flex;
    align-items: center;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.header-icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration) var(--ease-in-out);
  font-family: inherit;

  &:hover {
    background: var(--bg-surface-hover);
    color: var(--text-primary);
  }
}

.header-user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 14px 4px 4px;
  border: 1px solid var(--border-l2);
  background: var(--bg-surface);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration) var(--ease-in-out);
  font-family: inherit;
  color: inherit;

  &:hover {
    background: var(--bg-surface-hover);
    border-color: var(--border-l3);
  }
}

.header-avatar {
  background: var(--brand-primary) !important;
  color: #fff;
  font-weight: var(--font-weight-bold);
}

.header-user-name {
  font-size: var(--font-size-m);
  font-weight: var(--font-weight-strong);
  color: var(--text-primary);
}

.header-user-arrow {
  color: var(--text-muted);
  transition: transform var(--duration) var(--ease-in-out);
}

/* ── Content ── */
.admin-content {
  flex: 1;
  padding: 32px 40px 48px;
  overflow-y: auto;
  background: var(--bg-surface);
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
}
</style>

<style lang="scss">
// User dropdown (teleported, must be unscoped)
.user-dropdown {
  border-radius: var(--radius-md) !important;
  border: 1px solid var(--border-l1) !important;
  box-shadow: var(--shadow-md) !important;
  margin-top: 6px !important;
  padding: 6px !important;

  .el-dropdown-menu__item {
    border-radius: var(--radius-sm);
    font-weight: var(--font-weight-strong);
    font-size: var(--font-size-m);
    gap: 8px;
    padding: 8px 12px;
    margin: 1px 0;
  }
}
</style>
