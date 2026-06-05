<template>
  <div class="admin-layout" :class="{ 'admin-layout--collapsed': collapsed }">
    <aside class="admin-sidebar">
      <!-- Organic blob decoration -->
      <div class="sidebar-blob sidebar-blob--1" />
      <div class="sidebar-blob sidebar-blob--2" />

      <div class="admin-sidebar__brand">
        <div class="admin-sidebar__logo">
          <svg viewBox="0 0 42 42" width="38" height="38" fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="org-logo-grad" x1="6" y1="6" x2="36" y2="36">
                <stop offset="0%" stop-color="#6eb89a" />
                <stop offset="100%" stop-color="#8cc9aa" />
              </linearGradient>
              <filter id="logo-blur">
                <feGaussianBlur in="SourceGraphic" stdDeviation="1.5" />
              </filter>
            </defs>
            <!-- Glow circle -->
            <circle cx="21" cy="21" r="18" fill="url(#org-logo-grad)" opacity=".15" filter="url(#logo-blur)" />
            <!-- Main shape -->
            <rect x="6" y="6" width="30" height="30" rx="14" fill="url(#org-logo-grad)" />
            <!-- Icon: abstract leaf/store -->
            <path d="M21 14c-3 3-5 7-5 7h4v5h2v-5h4s-2-4-5-7z" fill="#fff" opacity=".95" />
            <circle cx="21" cy="18" r="2" fill="#fff" opacity=".6" />
          </svg>
        </div>
        <transition name="brand-fade">
          <div v-show="!collapsed" class="admin-sidebar__brand-text">
            <span class="admin-sidebar__brand-name">Marketplace</span>
            <span class="admin-sidebar__brand-label">管理后台</span>
          </div>
        </transition>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        background-color="transparent"
        text-color="rgba(255,255,255,.45)"
        active-text-color="#fff"
        class="admin-menu"
        @select="handleMenuSelect"
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

      <div class="admin-sidebar__footer">
        <button class="collapse-btn" @click="collapsed = !collapsed" :title="collapsed ? '展开菜单' : '收起菜单'">
          <el-icon :size="18">
            <DArrowLeft v-if="!collapsed" />
            <DArrowRight v-else />
          </el-icon>
        </button>
      </div>
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
        <router-view />
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
   Sidebar — organic dark with biomorphic blobs
   ═══════════════════════════════════════ */
.admin-sidebar {
  width: 250px;
  flex-shrink: 0;
  background: #1c2822;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  transition: width var(--org-duration) var(--org-ease);
}

// Organic floating blobs
.sidebar-blob {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(60px);
  opacity: .06;

  &--1 {
    width: 260px;
    height: 260px;
    background: #6eb89a;
    top: -60px;
    right: -80px;
  }

  &--2 {
    width: 200px;
    height: 200px;
    background: #b8b0e0;
    bottom: 40px;
    left: -60px;
  }
}

.admin-sidebar__brand {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, .06);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

.admin-sidebar__logo {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.admin-sidebar__brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  white-space: nowrap;
}

.admin-sidebar__brand-name {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
  letter-spacing: -.3px;
}

.admin-sidebar__brand-label {
  font-size: 11px;
  color: rgba(255, 255, 255, .3);
  font-weight: 600;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}

/* ── Menu ── */
.admin-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 0;
  position: relative;
  z-index: 1;

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 46px;
    line-height: 46px;
    margin: 3px 12px;
    border-radius: var(--org-radius-md);
    font-size: 13.5px;
    font-weight: 500;
    transition: all var(--org-duration) var(--org-ease);
    position: relative;

    &:hover {
      background: rgba(255, 255, 255, .05);
      color: rgba(255, 255, 255, .85);
      transform: translateX(2px);
    }
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(135deg, rgba(110, 184, 154, .22), rgba(110, 184, 154, .08));
    color: #fff;
    font-weight: 700;
    box-shadow: 0 2px 12px rgba(110, 184, 154, .1);

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 10px;
      bottom: 10px;
      width: 4px;
      background: var(--org-accent);
      border-radius: 0 4px 4px 0;
    }
  }

  :deep(.el-sub-menu .el-menu) {
    background: rgba(0, 0, 0, .18);
    padding: 6px 0;
    margin: 2px 12px;
    border-radius: var(--org-radius-md);

    .el-menu-item {
      height: 40px;
      line-height: 40px;
      padding-left: 56px;
      font-size: 13px;
      margin: 2px 6px;
      border-radius: var(--org-radius-sm);
    }
  }
}

/* ── Sidebar footer ── */
.admin-sidebar__footer {
  padding: 14px 16px;
  border-top: 1px solid rgba(255, 255, 255, .05);
  position: relative;
  z-index: 1;
}

.collapse-btn {
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, .03);
  border: 1px solid rgba(255, 255, 255, .06);
  border-radius: var(--org-radius-md);
  color: rgba(255, 255, 255, .35);
  cursor: pointer;
  transition: all var(--org-duration) var(--org-ease);
  font-family: inherit;

  &:hover {
    background: rgba(255, 255, 255, .06);
    color: rgba(255, 255, 255, .65);
    border-color: rgba(255, 255, 255, .1);
  }

  &:active {
    transform: scale(.94);
  }
}

/* ── Collapsed ── */
.admin-layout--collapsed .admin-sidebar {
  width: 72px;
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
  background: var(--org-bg);
}

/* ── Header — frosted glass ── */
.admin-header {
  height: 64px;
  background: rgba(255, 255, 255, .7);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom: 1px solid var(--org-border);
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
    gap: 6px;
  }
}

.header-icon-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--org-radius-full);
  color: var(--org-text-secondary);
  cursor: pointer;
  transition: all var(--org-duration) var(--org-ease);
  font-family: inherit;

  &:hover {
    background: var(--org-surface-hover);
    color: var(--org-text);
  }

  &:active {
    transform: scale(.9);
  }
}

.header-user-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 16px 5px 5px;
  border: 1px solid var(--org-border);
  background: var(--org-surface);
  border-radius: var(--org-radius-full);
  cursor: pointer;
  transition: all var(--org-duration) var(--org-ease);
  font-family: inherit;
  color: inherit;

  &:hover {
    border-color: var(--org-accent-light);
    box-shadow: var(--org-shadow-sm);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0) scale(.97);
  }
}

.header-avatar {
  background: linear-gradient(135deg, #6eb89a, #8cc9aa) !important;
  color: #fff;
  font-weight: 700;
}

.header-user-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--org-text);
}

.header-user-arrow {
  color: var(--org-text-muted);
  transition: transform var(--org-duration) var(--org-ease);
}

/* ── Content ── */
.admin-content {
  flex: 1;
  padding: 32px 32px 48px;
  overflow-y: auto;
}
</style>

<style lang="scss">
// User dropdown (teleported, must be unscoped)
.user-dropdown {
  border-radius: var(--org-radius-lg) !important;
  border: 1px solid var(--org-border) !important;
  box-shadow: var(--org-shadow-lg) !important;
  margin-top: 8px !important;
  padding: 8px !important;

  .el-dropdown-menu__item {
    border-radius: var(--org-radius-sm);
    font-weight: 600;
    font-size: 13.5px;
    gap: 10px;
    padding: 10px 14px;
    margin: 2px 0;
  }
}
</style>
