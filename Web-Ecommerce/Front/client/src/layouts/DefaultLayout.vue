<template>
  <div class="default-layout">
    <header class="header">
      <div class="header__inner">
        <router-link to="/" class="header__logo">电商平台</router-link>
        <div class="header__search">
          <el-input v-model="keyword" placeholder="搜索商品" size="large" clearable @keyup.enter="handleSearch">
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="header__actions">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown>
              <span class="header__user">
                {{ userStore.user?.nickname || userStore.user?.username }}
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/user/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/orders')">我的订单</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
          <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0">
            <el-button circle :icon="ShoppingCart" @click="$router.push('/cart')" />
          </el-badge>
        </div>
      </div>
    </header>
    <main class="main">
      <router-view />
    </main>
    <footer class="footer">
      <p>&copy; 2026 电商平台. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ShoppingCart, ArrowDown } from '@element-plus/icons-vue'
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
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 24px;

  &__inner {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    gap: 24px;
    height: 64px;
  }

  &__logo {
    font-size: 22px;
    font-weight: 700;
    color: #409eff;
    text-decoration: none;
    white-space: nowrap;
  }

  &__search {
    flex: 1;
    max-width: 480px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-left: auto;
  }

  &__user {
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px;
}

.footer {
  background: #f5f7fa;
  text-align: center;
  padding: 24px;
  color: #999;
  font-size: 13px;
}
</style>
