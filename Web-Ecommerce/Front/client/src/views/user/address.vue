<template>
  <div class="address-page">
    <div class="page-header">
      <h1 class="page-title">收货地址</h1>
      <el-button type="primary" @click="handleAdd">新增地址</el-button>
    </div>

    <div class="address-list" v-loading="loading">
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
      <el-empty v-if="!loading && !addresses.length" description="暂无收货地址" />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑地址' : '新增地址'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="收货人" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="省" prop="province">
          <el-input v-model="form.province" placeholder="请输入省" @blur="autoCorrectProvince" />
        </el-form-item>
        <el-form-item label="市" prop="city">
          <el-input v-model="form.city" placeholder="请输入市" @blur="autoCorrectCity" />
        </el-form-item>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="form.district" placeholder="请输入区/县" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="form.detail" type="textarea" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getAddressList, createAddress, updateAddress, deleteAddress } from '@/api/user'
import { requiredRule, phoneRules } from '@shared/validators'
import type { Address, AddressForm } from '@shared/types/user'

const addresses = ref<Address[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(0)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<AddressForm>({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0,
})

const rules: FormRules = {
  name: [requiredRule('收货人')],
  phone: phoneRules,
  province: [requiredRule('省')],
  city: [requiredRule('市')],
  detail: [requiredRule('详细地址')],
}

const MUNICIPALITIES = new Set(['北京', '天津', '上海', '重庆'])

function autoCorrectProvince() {
  const v = form.province.trim()
  if (!v) return
  if (/[省市自治区]$/.test(v)) return
  if (/特别行政区$/.test(v) || /自治州$/.test(v)) return
  form.province = MUNICIPALITIES.has(v) ? v + '市' : v + '省'
}

function autoCorrectCity() {
  const v = form.city.trim()
  if (!v) return
  if (/[市地区州盟]$/.test(v)) return
  form.city = v + '市'
}

async function loadAddresses() {
  loading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  editId.value = 0
  Object.assign(form, { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
  dialogVisible.value = true
}

function handleEdit(addr: Address) {
  isEdit.value = true
  editId.value = addr.id
  Object.assign(form, {
    name: addr.name,
    phone: addr.phone,
    province: addr.province,
    city: addr.city,
    district: addr.district,
    detail: addr.detail,
    isDefault: addr.isDefault,
  })
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该地址吗？', '提示', { type: 'warning' })
  await deleteAddress(id)
  ElMessage.success('已删除')
  loadAddresses()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateAddress(editId.value, form)
    } else {
      await createAddress(form)
    }
    ElMessage.success(isEdit.value ? '已更新' : '已添加')
    dialogVisible.value = false
    loadAddresses()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAddresses()
})
</script>

<style lang="scss" scoped>
.address-page {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
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
  .address-page {
    padding: 0 8px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .address-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;

    &__actions {
      margin-left: 0;
      width: 100%;
      justify-content: flex-end;
    }

    &__contact {
      flex-wrap: wrap;
      gap: 4px;
    }
  }
}
</style>
