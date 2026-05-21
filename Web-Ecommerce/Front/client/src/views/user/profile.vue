<template>
  <div class="profile-page">
    <h1>个人中心</h1>
    <el-card>
      <el-form :model="form" label-width="80px" style="max-width: 480px">
        <el-form-item label="头像">
          <el-avatar :size="80" :src="form.avatar" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateUserInfo } from '@/api/user'

const userStore = useUserStore()

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
})

const saving = ref(false)

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
  h1 {
    font-size: 22px;
    margin-bottom: 20px;
  }
}
</style>
