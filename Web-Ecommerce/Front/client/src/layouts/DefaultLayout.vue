<template>
  <div class="default-layout">
    <header class="header">
      <!-- Row 1: Main bar -->
      <div class="header__bar">
        <div class="header__bar-inner">
          <div class="header__bar-left">
            <router-link to="/" class="header__logo">
              &#x1F6D2; 电商平台
            </router-link>
            <nav class="header__nav">
              <router-link to="/products" class="header__nav-link">全部商品</router-link>
              <router-link to="/products?sort=newest" class="header__nav-link">新品</router-link>
              <router-link to="/products?sort=sales_desc" class="header__nav-link">热销</router-link>
            </nav>
          </div>

          <div class="header__bar-search">
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
              class="header-search-input"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #default="{ item }">
                <div v-if="item.type === 'clear'" class="history-clear" @click.stop="handleClearHistory">
                  清除全部历史
                </div>
                <div v-else class="history-item">
                  <el-icon class="history-item__clock"><Clock /></el-icon>
                  <span class="history-item__text">{{ item.keyword }}</span>
                  <el-icon class="history-item__del" @click.stop="handleRemoveHistory(item.keyword)"><Close /></el-icon>
                </div>
              </template>
            </el-autocomplete>
          </div>

          <div class="header__bar-right">
            <template v-if="userStore.isLoggedIn">
              <el-dropdown>
                <span class="header__user">
                  <el-avatar :size="32">
                    <span>{{ (userStore.user?.nickname || userStore.user?.username)?.[0]?.toUpperCase() }}</span>
                  </el-avatar>
                  <span class="header__user-name">
                    {{ userStore.user?.nickname || userStore.user?.username }}
                  </span>
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
              <el-button text @click="$router.push('/login')">登录</el-button>
              <el-button type="primary" size="small" @click="$router.push('/register')">注册</el-button>
            </template>
            <el-badge :value="cartStore.totalCount" :hidden="!cartStore.totalCount" :max="99">
              <el-button circle :icon="ShoppingCart" class="cart-btn" @click="$router.push('/cart')" />
            </el-badge>
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
          <a href="#">联系客服</a>
        </div>
        <div class="footer__col">
          <h4>关于我们</h4>
          <a href="#">公司介绍</a>
          <a href="#">联系我们</a>
          <a href="#">隐私政策</a>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search, ShoppingCart, Clock, Close,
  User, Document, SwitchButton, Star,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useSearchHistory } from '@/composables/useSearchHistory'
import type { SearchHistoryItem } from '@/composables/useSearchHistory'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const keyword = ref('')
const { getAll: getHistory, add: addHistory, remove: removeHistory, clear: clearHistory } = useSearchHistory()

function fetchSuggestions(queryString: string, callback: (data: any[]) => void) {
  if (queryString.trim()) {
    callback([])
    return
  }
  const history = getHistory()
  callback([...history, { type: 'clear' }])
}

function handleSelect(item: any) {
  if (item.type === 'clear') return
  router.push({ path: '/products', query: { keyword: item.keyword } })
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
  /* Brand */
  --brand-primary: #409eff;
  --brand-primary-rgb: 64, 158, 255;
  --brand-primary-hover: #66b1ff;
  --brand-primary-active: #337ecc;

  /* Text */
  --text1: #1d1d1f;
  --text2: #555;
  --text3: #909399;
  --text4: #c0c4cc;

  /* Background */
  --bg1: #fff;
  --bg2: #f5f7fa;
  --bg3: #eef1f6;

  /* Border */
  --line-light: #e8e8e8;
  --line-regular: #dcdfe6;

  /* Radius */
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 12px;
  --radius-full: 24px;

  /* Transition */
  --transition-fast: 0.15s ease;
  --transition-normal: 0.25s ease;

  /* Shadow */
  --shadow-sm: 0 1px 4px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 1px 6px rgba(0, 0, 0, 0.06);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.10);
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
  background: #fff;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  box-shadow: var(--shadow-md);
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
    flex: 1;
    display: flex;
    align-items: center;
    gap: 24px;
  }

  &-search {
    flex: 0 0 520px;
  }

  &-right {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;
  }
}

.header__logo {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--brand-primary);
  text-decoration: none;
  white-space: nowrap;
}

.header__nav {
  display: flex;
  gap: 4px;

  &-link {
    padding: 6px 12px;
    font-size: 14px;
    color: var(--text2);
    border-radius: var(--radius-sm);
    transition: all var(--transition-fast);

    &:hover {
      color: var(--brand-primary);
      background: rgba(var(--brand-primary-rgb), 0.06);
    }

    &.router-link-exact-active {
      color: var(--brand-primary);
      font-weight: 600;
    }
  }
}

.header__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;

  &-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text1);
  }
}

.cart-btn {
  font-size: 18px;
  border: 1px solid var(--line-light);
  transition: all var(--transition-fast);

  &:hover {
    color: var(--brand-primary);
    border-color: var(--brand-primary);
  }
}

/* ── Search input ── */
.header-search-input {
  :deep(.el-input__wrapper) {
    border: 2px solid var(--line-regular);
    border-radius: var(--radius-full);
    background: var(--bg2);
    box-shadow: none;
    padding-left: 8px;
    transition: all var(--transition-normal);

    &:hover {
      border-color: var(--brand-primary-hover);
      background: #ecf5ff;
      box-shadow: 0 2px 8px rgba(var(--brand-primary-rgb), 0.08);
    }

    &.is-focus {
      border-color: var(--brand-primary);
      background: #fff;
      box-shadow: 0 4px 16px rgba(var(--brand-primary-rgb), 0.15);
    }
  }

  :deep(.el-input__inner) {
    color: var(--text2);
    font-size: 15px;

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
  background: #2c2c2c;
  color: #aaa;
  padding: 40px 24px 24px;

  &__inner {
    max-width: 1000px;
    margin: 0 auto;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 32px;
  }

  &__col {
    h4 {
      color: #ddd;
      font-size: 15px;
      margin-bottom: 14px;
    }

    a {
      display: block;
      color: #999;
      font-size: 13px;
      margin-bottom: 8px;
      transition: color var(--transition-fast);

      &:hover { color: #fff; }
    }

    &--brand {
      text-align: right;
      p { font-size: 18px; font-weight: 600; color: #ddd; margin-bottom: 6px; }
      span { font-size: 12px; }
    }
  }
}

/* ── Responsive ── */
@media (max-width: 1024px) {
  .header__bar {
    &-search {
      flex: 0 0 360px;
    }
  }
}

@media (max-width: 768px) {
  .header__bar {
    &-inner {
      gap: 10px;
      padding: 0 12px;
      height: 56px;
    }

    &-search {
      flex: 1 1 auto;
      max-width: 260px;
    }
  }

  .header__logo {
    font-size: 16px;
  }

  .header__nav {
    display: none;
  }

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
  }

  li {
    padding: 0;
    line-height: normal;

    &:hover {
      background: #f5f7fa;
    }

    &.highlighted {
      background: #eef1f6;
    }

    &:last-child {
      border-top: 1px solid #ebeef5;
    }
  }

  .history-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 16px;

    &__clock {
      color: #a8abb2;
      font-size: 14px;
      flex-shrink: 0;
    }

    &__text {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 14px;
      color: #333;
    }

    &__del {
      color: #c0c4cc;
      font-size: 12px;
      flex-shrink: 0;
      cursor: pointer;
      padding: 2px;
      border-radius: 4px;
      transition: all .15s;

      &:hover {
        color: #f56c6c;
        background: rgba(245,108,108,.08);
      }
    }
  }

  .history-clear {
    padding: 10px 16px;
    text-align: center;
    font-size: 13px;
    color: #909399;
    cursor: pointer;
    transition: color .15s;

    &:hover {
      color: #f56c6c;
    }
  }
}
</style>
