<template>
  <div class="default-layout">
    <header class="header">
      <div class="header__inner">
        <router-link to="/" class="header__logo">
          &#x1F6D2; 电商平台
        </router-link>

        <div class="header__nav">
          <router-link to="/products" class="header__nav-link">全部商品</router-link>
          <router-link to="/products?sort=newest" class="header__nav-link">新品</router-link>
          <router-link to="/products?sort=sales_desc" class="header__nav-link">热销</router-link>
        </div>

        <div class="header__search">
          <el-input
            v-model="keyword"
            placeholder="搜索商品..."
            size="large"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="header__actions">
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
  Search, ShoppingCart,
  User, Document, SwitchButton,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const keyword = ref('')

function handleSearch() {
  if (keyword.value.trim()) {
    router.push({ path: '/products', query: { keyword: keyword.value.trim() } })
  }
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style lang="scss" scoped>
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

/* ── Header ── */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 1px 6px rgba(0,0,0,.06);

  &__inner {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    gap: 20px;
    height: 60px;
    padding: 0 24px;
  }

  &__logo {
    font-size: 20px;
    font-weight: 700;
    color: #409eff;
    text-decoration: none;
    white-space: nowrap;
  }

  &__nav {
    display: flex;
    gap: 4px;

    &-link {
      padding: 6px 12px;
      font-size: 14px;
      color: #555;
      border-radius: 6px;
      transition: all .15s;

      &:hover {
        color: #409eff;
        background: rgba(64,158,255,.06);
      }

      &.router-link-exact-active {
        color: #409eff;
        font-weight: 600;
      }
    }
  }

  &__search {
    flex: 1;
    max-width: 360px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-left: auto;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    &-name {
      font-size: 14px;
      font-weight: 500;
      color: #333;
    }
  }
}

.cart-btn {
  font-size: 18px;
  border: 1px solid #e0e0e0;
  transition: all .15s;

  &:hover {
    color: #409eff;
    border-color: #409eff;
  }
}

/* ── Main ── */
.main {
  flex: 1;
  padding: 24px;
  min-height: calc(100vh - 60px - 200px);
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
      transition: color .15s;

      &:hover { color: #fff; }
    }

    &--brand {
      text-align: right;
      p { font-size: 18px; font-weight: 600; color: #ddd; margin-bottom: 6px; }
      span { font-size: 12px; }
    }
  }
}

@media (max-width: 768px) {
  .header {
    &__inner { gap: 10px; padding: 0 12px; }
    &__nav { display: none; }
    &__search { max-width: 200px; }
    &__logo { font-size: 16px; }
  }

  .main { padding: 12px; }

  .footer__inner {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }
}
</style>
