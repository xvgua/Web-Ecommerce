<template>
  <div class="profile-page">
    <h1 class="page-title">个人中心</h1>
    <div class="profile-layout">
      <div class="profile-sidebar">
        <div class="profile-avatar">
          <el-avatar :size="80" :src="form.avatar">
            <span style="font-size:32px">{{ form.nickname?.[0] || form.username?.[0] || 'U' }}</span>
          </el-avatar>
          <h3>{{ form.nickname || form.username }}</h3>
          <p>{{ form.email || '未绑定邮箱' }}</p>
        </div>
        <div class="profile-nav">
          <div class="profile-nav__item profile-nav__item--active">个人信息</div>
          <div class="profile-nav__item" @click="$router.push('/user/address')">收货地址</div>
          <div class="profile-nav__item" @click="$router.push('/orders')">我的订单</div>
        </div>
      </div>

      <div class="profile-main">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight:600;font-size:16px">基本信息</span>
          </template>
          <el-form :model="form" label-width="80px" class="profile-form">
            <el-form-item label="头像">
              <el-avatar :size="64" :src="form.avatar" />
              <el-button text type="primary" style="margin-left:12px">更换头像</el-button>
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
              <el-button @click="showPwdDialog = true">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateUserInfo } from '@/api/user'

const userStore = useUserStore()
const saving = ref(false)
const showPwdDialog = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
})

async function handleSave() {
  saving.value = true
  try {
    await updateUserInfo({
      nickname: form.nickname,
      email: form.email,
      phone: form.phone,
    })
    ElMessage.success('个人信息已更新')
    userStore.fetchUser()
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (userStore.user) {
    form.username = userStore.user.username
    form.nickname = userStore.user.nickname || ''
    form.email = userStore.user.email || ''
    form.phone = userStore.user.phone || ''
    form.avatar = userStore.user.avatar || ''
  }
})
</script>

<style lang="scss" scoped>
.profile-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 24px;
}

.profile-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.profile-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.profile-avatar {
  padding: 32px 20px 20px;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 8%);
  color: #fff;

  h3 { margin-top: 12px; font-size: 16px; }
  p  { font-size: 12px; opacity: .75; margin-top: 4px; }
}

.profile-nav {
  padding: 8px 0;

  &__item {
    padding: 12px 20px;
    cursor: pointer;
    font-size: 14px;
    color: #666;
    transition: all .15s;

    &:hover { color: #409eff; background: rgba(64,158,255,.04); }
    &--active {
      color: #409eff;
      font-weight: 600;
      background: rgba(64,158,255,.08);
      border-right: 3px solid #409eff;
    }
  }
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.profile-form {
  max-width: 520px;

  :deep(.el-input.is-disabled .el-input__wrapper) {
    background: #f5f7fa;
  }
}

@media (max-width: 768px) {
  .profile-layout {
    flex-direction: column;
  }
  .profile-sidebar {
    width: 100%;
  }
  .profile-nav {
    display: flex;
    overflow-x: auto;
    &__item {
      white-space: nowrap;
      border-right: none !important;
      border-bottom: 3px solid transparent;
      &--active { border-bottom-color: #409eff; }
    }
  }
}
</style>
