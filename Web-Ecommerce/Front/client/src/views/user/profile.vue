<template>
  <div class="profile-page">
    <h1 class="page-title">个人中心</h1>
    <div class="profile-layout">
      <div class="profile-sidebar">
        <el-upload
          class="avatar-uploader"
          action="/api/upload"
          :headers="uploadHeaders"
          accept="image/*"
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :on-success="onAvatarSuccess"
        >
          <div class="profile-avatar">
            <el-avatar :size="80" :src="form.avatar">
              <span style="font-size:32px">{{ form.nickname?.[0] || form.username?.[0] || 'U' }}</span>
            </el-avatar>
            <div class="avatar-overlay">
              <el-icon><Picture /></el-icon>
              <span>更换头像</span>
            </div>
            <h3>{{ form.username }}</h3>
            <p class="profile-email">{{ form.email }}</p>
          </div>
        </el-upload>
        <div class="profile-nav">
          <div class="profile-nav__item profile-nav__item--active">个人信息</div>
          <div class="profile-nav__item" @click="$router.push('/user/address')">收货地址</div>
          <div class="profile-nav__item" @click="$router.push('/orders')">我的订单</div>
          <div class="profile-nav__item" @click="$router.push('/user/favorites')">我的收藏</div>
        </div>
      </div>

      <div class="profile-main">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight:600;font-size:16px">基本信息</span>
          </template>
          <el-form :model="form" label-width="100px" class="profile-form">
            <el-form-item label="账号ID">
              <el-input :model-value="String(form.accountId)" disabled />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input
                v-model="form.username"
                :disabled="!canEditUsername"
                placeholder="请输入用户名"
              />
              <span v-if="!canEditUsername && nextUsernameDate" class="field-hint">
                用户名每月可修改一次，下次可修改时间：{{ nextUsernameDate }}
              </span>
              <span v-else-if="!canEditUsername" class="field-hint">
                用户名每月可修改一次
              </span>
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
            </el-form-item>
            <el-form-item label="自我介绍">
              <el-input
                v-model="form.intro"
                type="textarea"
                placeholder="介绍一下自己吧..."
                maxlength="200"
                show-word-limit
                :rows="3"
              />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio :value="0">保密</el-radio>
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="完善手机号以便接收物流通知" />
              <span v-if="!form.phone" class="field-hint field-hint--warn">
                建议完善手机号，方便接收物流通知
              </span>
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
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo } from '@/api/user'
import { TOKEN_KEY } from '@shared/constants'

const userStore = useUserStore()
const saving = ref(false)
const showPwdDialog = ref(false)

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY) || ''}`,
}))

const form = reactive({
  accountId: 0,
  username: '',
  nickname: '',
  intro: '',
  gender: 0,
  email: '',
  phone: '',
  avatar: '',
  usernameUpdateTime: '',
})

const canEditUsername = computed(() => {
  if (!form.usernameUpdateTime) return true
  const lastUpdate = new Date(form.usernameUpdateTime)
  const nextAvailable = new Date(lastUpdate)
  nextAvailable.setMonth(nextAvailable.getMonth() + 1)
  return Date.now() >= nextAvailable.getTime()
})

const nextUsernameDate = computed(() => {
  if (!form.usernameUpdateTime) return ''
  const lastUpdate = new Date(form.usernameUpdateTime)
  const nextAvailable = new Date(lastUpdate)
  nextAvailable.setMonth(nextAvailable.getMonth() + 1)
  if (Date.now() >= nextAvailable.getTime()) return ''
  return nextAvailable.toLocaleDateString('zh-CN')
})

function beforeAvatarUpload(file: File) {
  const isValid = /^image\/(jpeg|png|gif|webp)$/.test(file.type)
  if (!isValid) {
    ElMessage.error('仅支持 JPG/PNG/GIF/WebP 格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过2MB')
    return false
  }
  return true
}

function onAvatarSuccess(response: { code: number; data: { url: string } }) {
  if (response.code === 200) {
    form.avatar = response.data.url
    ElMessage.success('头像上传成功，点击"保存修改"生效')
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateUserInfo({
      nickname: form.nickname,
      email: form.email,
      phone: form.phone,
      avatar: form.avatar,
      gender: form.gender,
      intro: form.intro,
      username: canEditUsername.value ? form.username : undefined,
    })
    ElMessage.success('个人信息已更新')
    await userStore.fetchUser()
    if (userStore.user) {
      form.usernameUpdateTime = userStore.user.usernameUpdateTime || ''
    }
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await userStore.fetchUser()
  if (userStore.user) {
    form.accountId = userStore.user.accountId
    form.username = userStore.user.username
    form.nickname = userStore.user.nickname || ''
    form.intro = userStore.user.intro || ''
    form.gender = userStore.user.gender ?? 0
    form.email = userStore.user.email || ''
    form.phone = userStore.user.phone || ''
    form.avatar = userStore.user.avatar || ''
    form.usernameUpdateTime = userStore.user.usernameUpdateTime || ''
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

.avatar-uploader {
  display: block;

  :deep(.el-upload) {
    display: block;
  }
}

.profile-avatar {
  padding: 32px 20px 20px;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  position: relative;
  cursor: pointer;

  .avatar-overlay {
    position: absolute;
    top: 32px;
    left: 50%;
    transform: translateX(-50%);
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.45);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.2s;
    color: #fff;
    font-size: 12px;

    .el-icon {
      font-size: 20px;
    }
  }

  &:hover .avatar-overlay {
    opacity: 1;
  }

  h3 { margin-top: 12px; font-size: 16px; }
  .profile-email {
    font-size: 12px;
    opacity: 0.7;
    margin-top: 4px;
    word-break: break-all;
  }
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

  .field-hint {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
    display: inline-block;

    &--warn {
      color: #e6a23c;
    }
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
  .profile-form {
    max-width: 100%;
  }
}
</style>
