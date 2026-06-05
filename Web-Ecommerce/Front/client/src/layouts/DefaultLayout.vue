<template>
  <div class="default-layout">
    <header class="header" :class="{ 'header--scrolled': scrolled }">
      <!-- Row 1: Top nav bar (hides on scroll) -->
      <div class="header__top">
        <div class="header__top-inner">
          <nav class="header__nav">
            <router-link to="/" class="header__nav-link" exact-active-class="header__nav-link--active">首页</router-link>
            <router-link to="/products" class="header__nav-link" active-class="header__nav-link--active">全部商品</router-link>
            <router-link to="/seckill" class="header__nav-link" active-class="header__nav-link--active">限时秒杀</router-link>
            <router-link to="/coupons" class="header__nav-link" active-class="header__nav-link--active">领券中心</router-link>
          </nav>
          <div class="header__top-right">
            <el-badge :value="cartStore.totalCount" :hidden="!cartStore.totalCount" :max="99">
              <router-link to="/cart" class="header__top-link">
                <el-icon :size="17"><ShoppingCart /></el-icon>
                <span>购物车</span>
              </router-link>
            </el-badge>
            <template v-if="userStore.isLoggedIn">
              <el-dropdown trigger="hover">
                <span class="header__user">
                  <el-avatar :size="26" :src="userStore.user?.avatar">
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
              <router-link to="/login" class="header__top-link">请登录</router-link>
              <span class="header__pipe">|</span>
              <router-link to="/register" class="header__top-link">免费注册</router-link>
            </template>
          </div>
        </div>
      </div>

      <!-- Row 2: Logo + Search bar -->
      <div class="header__search-row">
        <div class="header__search-inner">
          <router-link to="/" class="header__logo">
            <svg class="header__logo-icon" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
              <rect x="4" y="10" width="24" height="19" rx="3" fill="currentColor"/>
              <path d="M10 10V8a6 6 0 0 1 12 0v2" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"/>
              <circle cx="12" cy="22" r="1.5" fill="#fff"/>
              <circle cx="20" cy="22" r="1.5" fill="#fff"/>
            </svg>
            <span>乐购电商平台</span>
          </router-link>
          <div class="search-area">
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
              <button class="search-box__btn" @click="handleSearch">
                <el-icon :size="18"><Search /></el-icon>
                <span>搜索</span>
              </button>
            </div>
            <div v-if="hotKeywords.length" class="search-hot-tags">
              <span class="search-hot-tags__label">热门搜索：</span>
              <span
                v-for="kw in hotKeywords.slice(0, 5)"
                :key="kw.keyword"
                class="search-hot-tags__tag"
                @click="handleHotClick(kw)"
              >{{ kw.keyword }}</span>
            </div>
          </div>
          <div class="header__search-spacer"></div>
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
import { ref, watch, onMounted, onUnmounted } from 'vue'
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
const categories = ref<Category[]>([])
const scrolled = ref(false)

function onScroll() {
  scrolled.value = window.scrollY > 10
}
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

/** Match a keyword to a category name and return the category id.
 *  Returns 0 if no match found.
 *  Matches exact name, keyword-contained-in-category, or category-contained-in-keyword. */
function findCategoryIdByName(tree: Category[], keyword: string): number {
  const kw = keyword.toLowerCase()
  for (const node of tree) {
    const name = node.name.toLowerCase()
    if (name === kw || name.includes(kw) || kw.includes(name)) return node.id
    if (node.children) {
      for (const child of node.children) {
        const childName = child.name.toLowerCase()
        if (childName === kw || childName.includes(kw) || kw.includes(childName)) return child.id
      }
    }
  }
  return 0
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
  const catId = findCategoryIdByName(categories.value, item.keyword)
  if (catId > 0) {
    router.push({ path: '/products', query: { categoryId: String(catId) } })
  } else {
    router.push({ path: '/products', query: { keyword: item.keyword } })
  }
}

function handleHotClick(kw: { keyword: string }) {
  keyword.value = kw.keyword
  const catId = findCategoryIdByName(categories.value, kw.keyword)
  if (catId > 0) {
    router.push({ path: '/products', query: { categoryId: String(catId) } })
  } else {
    router.push({ path: '/products', query: { keyword: kw.keyword } })
  }
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
  if (!kw) return
  addHistory(kw)
  const catId = findCategoryIdByName(categories.value, kw)
  if (catId > 0) {
    router.push({ path: '/products', query: { categoryId: String(catId) } })
  } else {
    router.push({ path: '/products', query: { keyword: kw } })
  }
}

watch(() => route.query.keyword, (kw) => {
  keyword.value = (kw as string) || ''
})

watch(() => route.query.categoryId, (catId) => {
  const cid = catId ? Number(catId) : 0
  if (!cid) return
  const name = findCategoryName(categories.value, cid)
  if (name) keyword.value = name
})

// Clear search box and refresh cart/stats when returning to home page
watch(() => route.path, (path) => {
  if (path === '/') {
    keyword.value = ''
    if (userStore.isLoggedIn) {
      cartStore.fetchCart(true)
    }
  }
})

onMounted(async () => {
  window.addEventListener('scroll', onScroll, { passive: true })
  const tasks: Promise<unknown>[] = []
  if (userStore.isLoggedIn) {
    tasks.push(userStore.fetchUser())
  }
  tasks.push(getHotKeywords(10).then(res => hotKeywords.value = res.data))
  tasks.push(getCategories().then(res => categories.value = res.data))
  await Promise.allSettled(tasks)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
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
     Swiss Minimalism — Design Tokens
     Monochrome + single accent. No gradients, no glow.
     ═══════════════════════════════════════════════════════════ */

  /* Brand — Swiss Red */
  --brand-primary: #C41E3A;
  --brand-primary-rgb: 196, 30, 58;
  --brand-primary-hover: #A01830;
  --brand-primary-active: #8B1528;
  --brand-primary-light: rgba(196, 30, 58, 0.06);
  --brand-primary-ghost: rgba(196, 30, 58, 0.04);

  /* Text — high-contrast monochrome */
  --text1: #111111;
  --text2: #444444;
  --text3: #777777;
  --text4: #AAAAAA;

  /* Background — pure white / near-white */
  --bg1: #FFFFFF;
  --bg2: #F7F7F7;
  --bg3: #EEEEEE;

  /* Border — hairline gray */
  --line-light: #E8E8E8;
  --line-regular: #D4D4D4;

  /* Radius — restrained */
  --radius-sm: 2px;
  --radius-md: 4px;
  --radius-lg: 6px;
  --radius-xl: 8px;
  --radius-full: 20px;

  /* Transition — snappy */
  --transition-fast: 0.12s ease;
  --transition-normal: 0.18s ease;
  --transition-slow: 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  /* Shadow — none or barely there */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.03);
  --shadow-md: 0 1px 4px rgba(0, 0, 0, 0.05);
  --shadow-lg: 0 2px 8px rgba(0, 0, 0, 0.06);
  --shadow-xl: 0 4px 16px rgba(0, 0, 0, 0.08);

  /* Semantic — restrained */
  --color-success: #2E7D32;
  --color-warning: #E65100;
  --color-danger: #C62828;
  --color-info: #1565C0;
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
  background: var(--bg1);
  border-bottom: 1px solid var(--line-light);
}

/* ── Pipe separator ── */
.header__pipe {
  color: var(--line-regular);
  font-size: 12px;
  user-select: none;
}

/* ══════════════════════════════════════════
   Row 1: Top nav bar (36px, hides on scroll)
   ══════════════════════════════════════════ */
.header__top {
  transition: height .2s, opacity .2s;

  &-inner {
    max-width: 1400px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    height: 36px;
    padding: 0 40px;
  }

  &-right {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-left: auto;
    flex-shrink: 0;
  }

  &-link {
    font-size: 12px;
    color: var(--text2);
    white-space: nowrap;
    transition: color .2s;

    &:hover {
      color: var(--brand-primary);
      text-decoration: none;
    }
  }
}

/* ── Nav links (pure text, left-aligned) ── */
.header__nav {
  display: flex;
  align-items: center;
  gap: 0;

  &-link {
    font-size: 13px;
    color: var(--text2);
    padding: 0 12px;
    white-space: nowrap;
    transition: color .2s;

    &:first-child { padding-left: 0; }

    &:hover {
      color: var(--brand-primary);
      text-decoration: none;
    }

    &--active {
      color: var(--brand-primary);
      font-weight: 600;
    }
  }
}

/* ── User area ── */
.header__user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 2px 8px 2px 2px;
  border-radius: var(--radius-full);
  transition: background .2s;

  &:hover { background: var(--brand-primary-ghost); }

  &-name {
    font-size: 12px;
    font-weight: 500;
    color: var(--text1);
  }
}

.header__user-arrow {
  font-size: 10px;
  color: var(--text3);
  transition: transform .2s;
}


.header__user:hover .header__user-arrow {
  transform: rotate(180deg);
}

/* ── Cart badge ── */
.header__top-right :deep(.el-badge__content.is-fixed) {
  top: 4px;
  right: 6px;
}

/* ══════════════════════════════════════════
   Row 2: Logo + Search bar
   ══════════════════════════════════════════ */
.header__search-row {
  border-top: 1px solid var(--line-light);
}

.header__search-inner {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
}

/* ── Logo (in search row) — left-align with hero-cat ── */
.header__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  white-space: nowrap;
  flex: 0 0 200px;
  transition: all .2s;

  span {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: -0.5px;
    color: var(--text1);
    transition: all .2s;
  }

  &:hover span { color: var(--brand-primary); }

  &-icon {
    width: 32px;
    height: 32px;
    color: var(--brand-primary);
    flex-shrink: 0;
    transition: all .2s;
  }
}

.search-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* ── Right spacer — mirrors hero-right sidebar width ── */
.header__search-spacer {
  flex: 0 0 260px;
}

.search-box {
  display: flex;
  align-items: center;

  &__input {
    flex: 1;

    :deep(.el-input__wrapper) {
      border: 1px solid var(--text1);
      border-radius: 2px 0 0 2px;
      background: var(--bg1);
      box-shadow: none;
      padding-left: 16px;
      height: 44px;
      border-right: none;
      transition: border-color var(--transition-fast);

      &:hover {
        border-color: var(--text1);
      }

      &.is-focus {
        border-color: var(--text1);
        box-shadow: none;
      }
    }

    :deep(.el-input__inner) {
      color: var(--text1);
      font-size: 14px;

      &::placeholder { color: var(--text4); font-weight: 400; }
    }

    :deep(.el-input__prefix) {
      color: var(--text2);
      font-size: 16px;
    }

    :deep(.el-input__suffix) { color: var(--text4); }
  }

  &__btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    height: 44px;
    padding: 0 28px;
    border-radius: 0 2px 2px 0;
    background: var(--text1);
    color: #fff;
    border: none;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    flex-shrink: 0;
    letter-spacing: -0.01em;
    transition: background var(--transition-fast);

    &:hover { background: #333; }
  }
}

/* ── Hot search tags ── */
.search-hot-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  font-size: 12px;

  &__label {
    flex-shrink: 0;
    color: var(--text4);
  }

  &__tag {
    padding: 2px 10px;
    background: var(--bg3);
    border-radius: 2px;
    color: var(--text2);
    cursor: pointer;
    transition: all var(--transition-fast);
    white-space: nowrap;

    &:hover {
      color: var(--text1);
      background: var(--line-regular);
    }
  }
}

/* ══════════════════════════════════════════
   Scrolled state
   ══════════════════════════════════════════ */
.header--scrolled {
  .header__top {
    height: 0;
    opacity: 0;
    overflow: hidden;
  }

  .search-hot-tags {
    display: none;
  }

  .header__search-inner {
    padding-top: 8px;
    padding-bottom: 8px;
  }

  .header__logo {
    gap: 6px;

    span {
      font-size: 16px;
    }

    &-icon {
      width: 24px;
      height: 24px;
    }
  }

  .search-box__input :deep(.el-input__wrapper) {
    height: 38px;
  }

  .search-box__btn {
    height: 38px;
    padding: 0 20px;
    font-size: 14px;
  }
}

/* ── Main ── */
.main {
  flex: 1;
  padding: 24px;
}

/* ── Footer ── */
.footer {
  background: var(--bg1);
  border-top: 1px solid var(--line-light);
  color: var(--text2);
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
      color: var(--text1);
      font-size: 13px;
      font-weight: 600;
      margin-bottom: 16px;
      letter-spacing: 0;
      text-transform: uppercase;
    }

    a {
      display: block;
      color: var(--text3);
      font-size: 13px;
      margin-bottom: 10px;
      transition: color var(--transition-fast);

      &:hover { color: var(--text1); }
    }

    &--brand {
      text-align: right;
      p {
        font-size: 16px;
        font-weight: 600;
        color: var(--text1);
        margin-bottom: 6px;
      }
      span { font-size: 12px; color: var(--text4); }
    }
  }
}

/* ── Responsive ── */
@media (max-width: 1024px) {
  .header__top-inner { padding: 0 24px; }
  .header__search-inner { padding: 12px 24px; gap: 16px; }
  .header__nav-link { padding: 0 8px; font-size: 12px; }
  .header__user-name { display: none; }
  .header--scrolled .header__search-inner { padding: 8px 24px; }
  .header__logo { flex: 0 0 180px; }
  .header__search-spacer { display: none; }
}

@media (max-width: 768px) {
  .header__top-inner {
    padding: 0 12px;
    height: 36px;
  }

  .header__nav { display: none; }
  .header__top-link span { display: none; }
  .header__top-right { gap: 4px; }

  .header__search-inner {
    padding: 10px 0;
    gap: 10px;
  }

  .header__logo {
    flex: 0 0 auto;
  }

  .header__logo {
    span { font-size: 15px; letter-spacing: 0; }
    &-icon { width: 24px; height: 24px; }
  }

  .header--scrolled .header__logo {
    span { font-size: 14px; }
    &-icon { width: 20px; height: 20px; }
  }

  .search-box__btn {
    padding: 0 14px;
    font-size: 13px;
  }

  .search-hot-tags { display: none; }

  .main { padding: 12px; }

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

  .el-autocomplete-suggestion {
    background: var(--bg1);
    border: 1px solid var(--line-light);
    border-radius: var(--radius-sm);
    box-shadow: var(--shadow-lg);
  }

  li {
    padding: 0;
    line-height: normal;

    &:hover {
      background: var(--bg2);
    }

    &.highlighted {
      background: var(--bg3);
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
      color: var(--text4);
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

/* ── User dropdown items — force single-line ── */
.el-dropdown-menu__item {
  white-space: nowrap;
}
</style>
