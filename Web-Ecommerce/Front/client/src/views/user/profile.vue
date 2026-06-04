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
          <div
            class="profile-nav__item"
            :class="{ 'profile-nav__item--active': currentSection === 'profile' }"
            @click="currentSection = 'profile'"
          >个人信息</div>
          <div
            class="profile-nav__item"
            :class="{ 'profile-nav__item--active': currentSection === 'address' }"
            @click="switchToAddress"
          >收货地址</div>
        </div>
      </div>

      <div class="profile-main">
        <!-- 个人信息 -->
        <template v-if="currentSection === 'profile'">
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
                <el-input v-model="form.email" disabled />
              </el-form-item>
              <el-form-item label="手机号">
                <el-input v-model="form.phone" placeholder="完善手机号以便接收物流通知" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
                <el-button @click="showPwdDialog = true">修改密码</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <!-- 收货地址 -->
        <template v-else>
          <div class="address-header">
            <span style="font-weight:600;font-size:16px">收货地址</span>
            <el-button type="primary" @click="handleAdd">新增地址</el-button>
          </div>

          <div class="address-list" v-loading="addrLoading">
            <div v-for="addr in addresses" :key="addr.id" class="address-card">
              <div class="address-card__content">
                <div class="address-card__region">
                  {{ addr.province }} {{ addr.city }} {{ addr.district }}
                </div>
                <div class="address-card__detail">{{ addr.detail }}</div>
                <div class="address-card__contact">
                  <span class="address-card__name">{{ addr.name }}</span>
                  <span class="address-card__phone">{{ addr.phone }}</span>
                  <el-tag v-if="addr.isDefault" size="small" type="primary">默认</el-tag>
                </div>
              </div>
              <div class="address-card__actions">
                <el-button text type="primary" @click="handleEdit(addr)">编辑</el-button>
                <el-button text type="danger" @click="handleDelete(addr.id)">删除</el-button>
              </div>
            </div>
            <el-empty v-if="!addrLoading && !addresses.length" description="暂无收货地址" />
          </div>
        </template>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPwdDialog" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px" @submit.prevent="handleChangePassword">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 地址编辑弹窗 -->
    <el-dialog v-model="addrDialogVisible" :title="isEdit ? '编辑地址' : '新增地址'" width="500px">
      <el-form ref="addrFormRef" :model="addrForm" :rules="addrRules" label-width="80px">
        <el-form-item label="收货人" prop="name">
          <el-input v-model="addrForm.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addrForm.phone" />
        </el-form-item>
        <el-form-item label="省" prop="province">
          <el-input v-model="addrForm.province" placeholder="请输入省" @blur="autoCorrectProvince" />
        </el-form-item>
        <el-form-item label="市" prop="city">
          <el-input v-model="addrForm.city" placeholder="请输入市" @blur="autoCorrectCity" />
        </el-form-item>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="addrForm.district" placeholder="请输入区/县" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="addrForm.detail" type="textarea" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addrForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addrSubmitting" @click="handleAddrSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, changePassword, getAddressList, createAddress, updateAddress, deleteAddress } from '@/api/user'
import { TOKEN_KEY } from '@shared/constants'
import { passwordRules, requiredRule, phoneRules } from '@shared/validators'
import type { Address, AddressForm } from '@shared/types/user'
import { useRoute } from 'vue-router'

const route = useRoute()

// ---- Section switching ----
const currentSection = ref<'profile' | 'address'>('profile')

function switchToAddress() {
  currentSection.value = 'address'
  loadAddresses()
}

const userStore = useUserStore()

// ---- Profile ----
const saving = ref(false)

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

// ---- Change Password ----
const showPwdDialog = ref(false)
const changingPwd = ref(false)
const pwdFormRef = ref<FormInstance>()

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPwd = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: passwordRules,
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' },
  ],
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value?.validate()
  if (!valid) return
  changingPwd.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    showPwdDialog.value = false
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    userStore.logout()
  } finally {
    changingPwd.value = false
  }
}

// ---- Address ----
const addresses = ref<Address[]>([])
const addrLoading = ref(false)
const addrDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(0)
const addrSubmitting = ref(false)
const addrFormRef = ref<FormInstance>()

const addrForm = reactive<AddressForm>({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0,
})

const addrRules: FormRules = {
  name: [requiredRule('收货人')],
  phone: phoneRules,
  province: [requiredRule('省')],
  city: [requiredRule('市')],
  detail: [requiredRule('详细地址')],
}

const MUNICIPALITIES = new Set(['北京', '天津', '上海', '重庆'])

function autoCorrectProvince() {
  const v = addrForm.province.trim()
  if (!v) return
  if (/[省市自治区]$/.test(v)) return
  if (/特别行政区$/.test(v) || /自治州$/.test(v)) return
  addrForm.province = MUNICIPALITIES.has(v) ? v + '市' : v + '省'
}

function autoCorrectCity() {
  const v = addrForm.city.trim()
  if (!v) return
  if (/[市地区州盟]$/.test(v)) return
  addrForm.city = v + '市'
}

async function loadAddresses() {
  addrLoading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data
  } finally {
    addrLoading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  editId.value = 0
  Object.assign(addrForm, { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
  addrDialogVisible.value = true
}

function handleEdit(addr: Address) {
  isEdit.value = true
  editId.value = addr.id
  Object.assign(addrForm, {
    name: addr.name,
    phone: addr.phone,
    province: addr.province,
    city: addr.city,
    district: addr.district,
    detail: addr.detail,
    isDefault: addr.isDefault,
  })
  addrDialogVisible.value = true
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该地址吗？', '提示', { type: 'warning' })
  await deleteAddress(id)
  ElMessage.success('已删除')
  loadAddresses()
}

async function handleAddrSubmit() {
  const valid = await addrFormRef.value?.validate()
  if (!valid) return

  addrSubmitting.value = true
  try {
    if (isEdit.value) {
      await updateAddress(editId.value, addrForm)
    } else {
      await createAddress(addrForm)
    }
    ElMessage.success(isEdit.value ? '已更新' : '已添加')
    addrDialogVisible.value = false
    loadAddresses()
  } finally {
    addrSubmitting.value = false
  }
}

// ---- Init ----
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
  }
}

// ---- Address ----
.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.address-list {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

.address-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: box-shadow .2s;

  &:last-child { margin-bottom: 0; }
  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,.06); }

  &__content { flex: 1; min-width: 0; }
  &__region { font-size: 13px; color: #999; }
  &__detail { font-size: 15px; font-weight: 600; color: #333; margin: 6px 0; }
  &__contact { font-size: 13px; color: #333; display: flex; align-items: center; gap: 8px; }
  &__phone { margin-left: 8px; }
  &__actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
    margin-left: 16px;
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
