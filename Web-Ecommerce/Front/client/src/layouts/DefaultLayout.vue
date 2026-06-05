<template>
  <div class="default-layout">
    <header class="header">
      <!-- Row 1: Main bar -->
      <div class="header__bar">
        <div class="header__bar-inner">
          <div class="header__bar-left">
            <router-link to="/" class="header__logo">
              <svg class="header__logo-icon" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
                <rect x="4" y="10" width="24" height="19" rx="3" fill="currentColor"/>
                <path d="M10 10V8a6 6 0 0 1 12 0v2" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
                <circle cx="12" cy="22" r="1.5" fill="#fff"/>
                <circle cx="20" cy="22" r="1.5" fill="#fff"/>
              </svg>
              <span>乐购电商平台</span>
            </router-link>
            <nav class="header__nav">
              <router-link to="/" class="header__nav-link" exact-active-class="header__nav-link--active">首页</router-link>
              <router-link to="/products" class="header__nav-link" active-class="header__nav-link--active">全部商品</router-link>
              <router-link to="/seckill" class="header__nav-link" active-class="header__nav-link--active">限时秒杀</router-link>
              <router-link to="/coupon/center" class="header__nav-link" active-class="header__nav-link--active">领券中心</router-link>
            </nav>
          </div>

          <div class="header__bar-search">
            <div class="search-box">
              <el-autocomplete
                v-model="keyword"
                value-key="keyword"
                :fetch-suggestions="fetchSuggestions"
                :trigger-on-focus="true"
                placeholder="搜索你想要的商品..."
                size="large"
                clearable
                maxlength="100"
                @select="handleSelect"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
                popper-class="search-history-popper"
                class="search-box__input"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
                <template #default="{ item }">
                  <div v-if="item.type === 'hot-section'" class="hot-section">
                    <div
                      v-for="kw in item.keywords"
                      :key="kw.keyword"
                      class="history-item hot-item"
                      @click="handleHotClick(kw)"
                    >
                      <span class="hot-item__fire">🔥</span>
                      <span class="history-item__text">{{ kw.keyword }}</span>
                      <span class="hot-item__count">{{ kw.count }}</span>
                    </div>
                  </div>
                  <div v-else-if="item.type === 'history-separator'" class="history-separator">
                    <span class="history-separator__text">搜索记录</span>
                  </div>
                  <div v-else-if="item.type === 'clear'" class="history-clear" @click.stop="handleClearHistory">
                    清除全部历史
                  </div>
                  <div v-else class="history-item">
                    <el-icon class="history-item__clock"><Clock /></el-icon>
                    <span class="history-item__text">{{ item.keyword }}</span>
                    <el-icon class="history-item__del" @click.stop="handleRemoveHistory(item.keyword)"><Close /></el-icon>
                  </div>
                </template>
              </el-autocomplete>
              <button class="search-box__btn" @click="handleSearch" :disabled="!keyword.trim()">
                <el-icon :size="20"><Search /></el-icon>
              </button>
            </div>
          </div>

          <div class="header__bar-right">
            <el-badge :value="cartStore.totalCount" :hidden="!cartStore.totalCount" :max="99">
              <router-link to="/cart" class="header__quick-link">
                <el-icon :size="20"><ShoppingCart /></el-icon>
                <span>购物车</span>
              </router-link>
            </el-badge>
            <template v-if="userStore.isLoggedIn">
              <el-dropdown trigger="hover">
                <span class="header__user">
                  <el-avatar :size="30" :src="userStore.user?.avatar">
                    <span>{{ userStore.user?.username?.[0]?.toUpperCase() }}</span>
                  </el-avatar>
                  <span class="header__user-name">{{ userStore.user?.username }}</span>
                  <el-icon class="header__user-arrow"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="$router.push('/user/profile')">
                      <el-icon><User /></el-icon> 个人中心
                    </el-dropdown-item>
                    <el-dropdown-item @click="$router.push('/orders')">
                      <el-icon><Document /></el-icon> 我的订单
                    </el-dropdown-item>
                    <el-dropdown-item @click="$router.push('/user/favorites')">
                      <el-icon><Star /></el-icon> 我的收藏
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleLogout">
                      <el-icon><SwitchButton /></el-icon> 退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <router-link to="/login" class="header__login-btn">登录</router-link>
              <router-link to="/register" class="header__reg-btn">免费注册</router-link>
            </template>
          </div>
        </div>
      </div>

    </header>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">
      <div class="footer__inner">
        <div class="footer__col">
          <h4>购物指南</h4>
          <a href="#">购物流程</a>
          <a href="#">支付方式</a>
          <a href="#">配送说明</a>
        </div>
        <div class="footer__col">
          <h4>售后服务</h4>
          <a href="#">退换货政策</a>
          <a href="#">退款说明</a>
          <a href="#">帮助中心</a>
        </div>
        <div class="footer__col">
          <h4>关于我们</h4>
          <a href="#">公司介绍</a>
          <a href="#">联系我们</a>
          <a href="#">隐私政策</a>
          <router-link to="/announcements">平台公告</router-link>
        </div>
        <div class="footer__col footer__col--brand">
          <p>&#x1F6D2; 电商平台</p>
          <span>&copy; 2026 Ecommerce Marketplace</span>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Search, ShoppingCart, Clock, Close,
  User, Document, SwitchButton, Star, ArrowDown,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useSearchHistory } from '@/composables/useSearchHistory'
import { getCategories, getHotKeywords } from '@/api/product'
import type { SearchHistoryItem } from '@/composables/useSearchHistory'
import type { Category, HotKeyword } from '@shared/types/product'
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()
const keyword = ref('')
const hotKeywords = ref<HotKeyword[]>([])
const { getAll: getHistory, add: addHistory, remove: removeHistory, clear: clearHistory } = useSearchHistory()

function findCategoryName(tree: Category[], id: number): string | null {
  for (const node of tree) {
    if (node.id === id) return node.name
    if (node.children) {
      for (const child of node.children) {
        if (child.id === id) return child.name
      }
    }
  }
  return null
}

function fetchSuggestions(queryString: string, callback: (data: any[]) => void) {
  if (queryString.trim()) {
    callback([])
    return
  }
  const items: any[] = []
  // Hot keywords — fixed at top, 5 items
  if (hotKeywords.value.length > 0) {
    items.push({ type: 'hot-section', keywords: hotKeywords.value.slice(0, 3) })
  }
  // History — scrollable below
  const history = getHistory()
  if (history.length > 0) {
    items.push({ type: 'history-separator' })
    items.push(...history)
    items.push({ type: 'clear' })
  }
  callback(items)
}

function handleSelect(item: any) {
  if (item.type === 'clear' || item.type === 'hot-section' || item.type === 'history-separator') return
  keyword.value = item.keyword
  router.push({ path: '/products', query: { keyword: item.keyword } })
}

function handleHotClick(kw: { keyword: string }) {
  keyword.value = kw.keyword
  router.push({ path: '/products', query: { keyword: kw.keyword } })
}

function handleRemoveHistory(keyword: string) {
  removeHistory([keyword])
}

function handleClearHistory() {
  clearHistory()
  keyword.value = ''
}

function handleSearch() {
  const kw = keyword.value.trim()
  if (kw) {
    addHistory(kw)
    router.push({ path: '/products', query: { keyword: kw } })
  }
}

watch(() => route.query.keyword, (kw) => {
  keyword.value = (kw as string) || ''
})

watch(() => route.query.categoryId, async (catId) => {
  const cid = catId ? Number(catId) : 0
  if (!cid) return
  try {
    const res = await getCategories()
    const name = findCategoryName(res.data, cid)
    if (name) keyword.value = name
  } catch { /* ignore */ }
}, { immediate: false })

// Clear search box when returning to home page
watch(() => route.path, (path) => {
  if (path === '/') keyword.value = ''
})

onMounted(async () => {
  const tasks: Promise<unknown>[] = []
  if (userStore.isLoggedIn) {
    tasks.push(userStore.fetchUser())
  }
  tasks.push(getHotKeywords(10).then(res => hotKeywords.value = res.data))
  await Promise.allSettled(tasks)
})

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}

</script>

<style lang="scss">
/* ═══════════════════════════════════════════════════════════
   CSS Design Tokens — global via :root (unscoped so variables
   cascade to all components)
   ═══════════════════════════════════════════════════════════ */
:root {
  /* ═══════════════════════════════════════════════════════════
     Mint Sage — Design Tokens
     Primary: #4EAB8E | A fresh, clean palette for ecommerce
     ═══════════════════════════════════════════════════════════ */

  /* Brand — Mint Sage */
  --brand-primary: #4EAB8E;
  --brand-primary-rgb: 78, 171, 142;
  --brand-primary-hover: #5FC0A2;
  --brand-primary-active: #3D8F76;
  --brand-primary-light: #E8F6F0;
  --brand-primary-ghost: rgba(var(--brand-primary-rgb), 0.08);

  /* Text — neutral charcoal-to-gray with cool undertone */
  --text1: #1A1C1B;
  --text2: #3D4A47;
  --text3: #80948F;
  --text4: #B0BDB9;

  /* Background — cool-tinted whites with slight sage undertone */
  --bg1: #fff;
  --bg2: #F4F9F7;
  --bg3: #EBF2EF;

  /* Border — cool gray matching the green family */
  --line-light: #E0E8E5;
  --line-regular: #C8D4D0;

  /* Radius */
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-full: 24px;

  /* Transition */
  --transition-fast: 0.15s ease;
  --transition-normal: 0.25s ease;
  --transition-slow: 0.35s cubic-bezier(0.4, 0, 0.2, 1);

  /* Shadow — tinted mint sage (matches brand hue) */
  --shadow-sm: 0 1px 3px rgba(40, 95, 75, 0.04);
  --shadow-md: 0 1px 6px rgba(40, 95, 75, 0.07);
  --shadow-lg: 0 8px 24px rgba(40, 95, 75, 0.10);
  --shadow-xl: 0 16px 40px rgba(40, 95, 75, 0.14);

  /* Accent — semantic colors that harmonize with mint */
  --color-success: #3DA06E;
  --color-warning: #E6A23C;
  --color-danger: #D94A4A;
  --color-info: #5C9E8E;
}
</style>

<style lang="scss" scoped>
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg2);
}

/* ── Header ── */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
  box-shadow: 0 1px 0 var(--line-light), 0 2px 8px rgba(40, 95, 75, 0.05);
}

/* ── Row 1: Main bar (64px) ── */
.header__bar {
  &-inner {
    max-width: 1280px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    height: 64px;
    padding: 0 24px;
  }

  &-left {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: nowrap;
    min-width: 0;
  }

  &-search {
    flex: 1;
    max-width: 600px;
    margin: 0 16px;
  }

  &-right {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 0;
    flex-shrink: 0;
  }
}

.header__logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  white-space: nowrap;
  padding: 4px 0;

  span {
    font-size: 21px;
    font-weight: 700;
    letter-spacing: -0.5px;
    color: var(--text1);
    transition: color var(--transition-fast);
  }

  &:hover span {
    color: var(--brand-primary);
  }

  &-icon {
    width: 34px;
    height: 34px;
    color: var(--brand-primary);
    flex-shrink: 0;
    transition: transform var(--transition-normal);

    .header__logo:hover & {
      transform: scale(1.05);
    }
  }
}

.header__nav {
  display: flex;
  flex-wrap: nowrap;
  gap: 2px;

  &-link {
    padding: 6px 14px;
    font-size: 14px;
    color: var(--text2);
    border-radius: var(--radius-md);
    white-space: nowrap;
    font-weight: 500;
    transition: all var(--transition-fast);

    &:hover {
      color: var(--brand-primary);
      background: var(--brand-primary-ghost);
      text-decoration: none;
    }

    &--active {
      color: var(--brand-primary) !important;
      font-weight: 600;
      background: var(--brand-primary-ghost);
    }
  }
}

.header__quick-link {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  font-size: 13px;
  color: var(--text2);
  border-radius: var(--radius-md);
  font-weight: 500;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--brand-primary);
    background: var(--brand-primary-ghost);
  }

  .el-icon {
    font-size: 17px;
  }
}

.header__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px 4px 4px;
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);

  &:hover {
    background: var(--brand-primary-ghost);
  }

  &-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text1);
  }
}

/* ── Login / Register ── */
.header__login-btn {
  padding: 7px 18px;
  font-size: 13px;
  font-weight: 600;
  color: var(--brand-primary);
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);

  &:hover {
    background: var(--brand-primary-ghost);
    text-decoration: none;
  }
}

.header__reg-btn {
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text3);
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);

  &:hover {
    color: var(--text1);
    text-decoration: none;
  }
}

/* ── Cart badge ── */
.header__bar-right :deep(.el-badge__content.is-fixed) {
  top: 6px;
  right: 8px;
}

/* ── User dropdown arrow ── */
.header__user-arrow {
  font-size: 12px;
  color: var(--text3);
  transition: transform var(--transition-fast);
}

.header__user:hover .header__user-arrow {
  transform: rotate(180deg);
}

/* ── Search box ── */
.search-box {
  position: relative;
  display: flex;
  align-items: center;

  &__input {
    flex: 1;

    :deep(.el-input__wrapper) {
      border: 2px solid var(--line-light);
      border-radius: 24px 0 0 24px;
      background: var(--bg2);
      box-shadow: none;
      padding-left: 18px;
      height: 44px;
      border-right: none;
      transition: all var(--transition-normal);

      &:hover {
        border-color: var(--line-regular);
        background: #fff;
      }

      &.is-focus {
        border-color: var(--brand-primary);
        background: #fff;
        box-shadow: 0 0 0 4px rgba(var(--brand-primary-rgb), 0.08);
      }
    }

    :deep(.el-input__inner) {
      color: var(--text1);
      font-size: 14px;

      &::placeholder {
        color: var(--text4);
        font-weight: 400;
      }
    }

    :deep(.el-input__prefix) {
      color: var(--brand-primary);
      font-size: 18px;
    }
  }

  &__btn {
    width: 48px;
    height: 44px;
    border-radius: 0 24px 24px 0;
    background: var(--brand-primary);
    color: #fff;
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    transition: background var(--transition-fast);

    &:hover {
      background: var(--brand-primary-hover);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

/* ── Old search input class (kept for search-history-popper) ── */
.header-search-input {
  :deep(.el-input__wrapper) {
    border: 1.5px solid var(--line-light);
    border-radius: var(--radius-full);
    background: var(--bg2);
    box-shadow: none;
    padding-left: 16px;
    height: 42px;
    transition: all var(--transition-normal);

    &:hover {
      border-color: var(--line-regular);
      background: #fff;
    }

    &.is-focus {
      border-color: var(--brand-primary);
      background: #fff;
      box-shadow: 0 0 0 3px rgba(var(--brand-primary-rgb), 0.1);
    }
  }

  :deep(.el-input__inner) {
    color: var(--text1);
    font-size: 14px;

    &::placeholder {
      color: var(--text4);
      font-weight: 400;
    }
  }

  :deep(.el-input__prefix) {
    color: var(--brand-primary);
    font-size: 18px;
  }
}

/* ── Main ── */
.main {
  flex: 1;
  padding: 24px;
}

/* ── Footer ── */
.footer {
  background: #1A2E28;
  color: #7AACA0;
  padding: 48px 24px 28px;

  &__inner {
    max-width: 1000px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 40px;
  }

  &__col {
    h4 {
      color: #A0C8BC;
      font-size: 14px;
      font-weight: 600;
      margin-bottom: 16px;
      letter-spacing: 0.5px;
    }

    a {
      display: block;
      color: #5A8C80;
      font-size: 13px;
      margin-bottom: 10px;
      transition: color var(--transition-fast);

      &:hover { color: var(--brand-primary-hover); }
    }

    &--brand {
      text-align: right;
      p {
        font-size: 18px;
        font-weight: 600;
        color: #A0C8BC;
        margin-bottom: 6px;
      }
      span { font-size: 12px; color: #4A7C70; }
    }
  }
}

/* ── Responsive ── */
@media (max-width: 1024px) {
  .header__bar {
    &-left { gap: 16px; }
    &-search { max-width: 340px; margin: 0 12px; }
  }

  .header__nav-link { padding: 4px 10px; font-size: 13px; }
  .header__quick-link { padding: 5px 8px; }
  .header__quick-link span { display: none; }
  .header__user-name { display: none; }
}

@media (max-width: 768px) {
  .header__bar {
    &-inner {
      gap: 8px;
      padding: 0 12px;
      height: 56px;
    }

    &-left { gap: 8px; }
    &-search {
      flex: 1 1 auto;
      max-width: 200px;
      margin: 0 6px;
    }
  }

  .header__logo {
    &-icon { width: 26px; height: 26px; }

    span {
      font-size: 15px;
      letter-spacing: 0;
    }
  }

  .header__nav { display: none; }
  .header__quick-link { padding: 4px 6px; font-size: 12px; }

  .main {
    padding: 12px;
  }

  .footer__inner {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }
}
</style>

<style lang="scss">
.search-history-popper {
  margin-top: 4px !important;

  .el-autocomplete-suggestion__wrap {
    padding: 4px 0;
    max-height: 390px;
  }

  li {
    padding: 0;
    line-height: normal;

    &:hover {
      background: var(--bg2);
    }

    &.highlighted {
      background: var(--brand-primary-light);
    }

    &:last-child {
      border-top: 1px solid var(--line-light);
    }
  }

  .hot-section {
    position: sticky;
    top: 0;
    z-index: 2;
    background: var(--bg1);
    padding-bottom: 4px;
    border-bottom: 1px solid var(--line-light);
  }

  .history-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 16px;

    &__clock {
      color: #a8a4a0;
      font-size: 14px;
      flex-shrink: 0;
    }

    &__text {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 14px;
      color: var(--text1);
    }

    &__del {
      color: var(--text4);
      font-size: 12px;
      flex-shrink: 0;
      cursor: pointer;
      padding: 2px;
      border-radius: 4px;
      transition: all var(--transition-fast);

      &:hover {
        color: var(--brand-primary);
        background: var(--brand-primary-ghost);
      }
    }
  }

  .history-clear {
    padding: 10px 16px;
    text-align: center;
    font-size: 13px;
    color: var(--text3);
    cursor: pointer;
    transition: color var(--transition-fast);

    &:hover {
      color: var(--brand-primary);
    }
  }

  .history-separator {
    padding: 8px 16px 4px;
    font-size: 12px;
    color: var(--text4);
    pointer-events: none;

    &__text {
      letter-spacing: 1px;
    }
  }

  .hot-item {
    cursor: pointer;

    &__fire {
      font-size: 14px;
      flex-shrink: 0;
    }

    &__count {
      font-size: 12px;
      color: var(--text4);
      flex-shrink: 0;
    }
  }
}
</style>
