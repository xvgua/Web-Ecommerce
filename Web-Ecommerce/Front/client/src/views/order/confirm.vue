<template>
  <div class="order-confirm">
    <h1>确认订单</h1>

    <div class="confirm-section">
      <div class="section-header">
        <h2>收货地址</h2>
        <el-button size="small" @click="handleAddAddress">新增地址</el-button>
      </div>
      <div class="address-list" v-if="addresses.length">
        <div
          v-for="addr in addresses"
          :key="addr.id"
          :class="['address-card', { 'address-card--active': selectedAddressId === addr.id }]"
          @click="selectedAddressId = addr.id"
        >
          <div class="address-card__header">
            <span class="address-card__name">{{ addr.name }}</span>
            <span class="address-card__phone">{{ addr.phone }}</span>
            <el-tag v-if="addr.isDefault" size="small" type="primary">默认</el-tag>
          </div>
          <div class="address-card__detail">
            {{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detail }}
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无收货地址，请添加" />
    </div>

    <el-dialog v-model="addressDialogVisible" title="新增地址" width="500px">
      <el-form ref="addressFormRef" :model="addressForm" :rules="addressRules" label-width="80px">
        <el-form-item label="收货人" prop="name">
          <el-input v-model="addressForm.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addressForm.phone" />
        </el-form-item>
        <el-form-item label="省市区" prop="district">
          <el-input v-model="addressForm.province" placeholder="省" style="width: 30%" />
          <el-input v-model="addressForm.city" placeholder="市" style="width: 30%; margin: 0 8px" />
          <el-input v-model="addressForm.district" placeholder="区" style="width: 30%" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="addressForm.detail" type="textarea" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addressSubmitting" @click="handleSubmitAddress">保存</el-button>
      </template>
    </el-dialog>

    <div class="confirm-section">
      <h2>商品清单</h2>
      <el-table :data="cartStore.checkedItems">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div class="order-product">
              <div class="order-product__img">
                <ProductImage :src="row.productImage" :seed="row.productName + row.productId" fit="cover" />
              </div>
              <div>
                <div>{{ row.productName }}</div>
                <div class="order-product__spec">{{ row.specDesc }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="80">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">{{ formatPrice(row.price * row.quantity) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="confirm-footer">
      <div class="confirm-footer__total">
        合计：<span class="confirm-footer__price">{{ formatPrice(cartStore.checkedTotal) }}</span>
      </div>
      <el-button type="danger" size="large" :loading="submitting" :disabled="!selectedAddressId" @click="handleSubmit">
        提交订单
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { getAddressList, createAddress } from '@/api/user'
import { createOrder } from '@/api/order'
import { formatPrice } from '@/utils/format'
import { requiredRule, phoneRules } from '@shared/validators'
import type { Address, AddressForm } from '@shared/types/user'
import ProductImage from '@/components/common/ProductImage.vue'

const router = useRouter()
const cartStore = useCartStore()

const addresses = ref<Address[]>([])
const selectedAddressId = ref<number>(0)
const submitting = ref(false)

const addressDialogVisible = ref(false)
const addressSubmitting = ref(false)
const addressFormRef = ref<FormInstance>()

const addressForm = reactive<AddressForm>({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0,
})

const addressRules: FormRules = {
  name: [requiredRule('收货人')],
  phone: phoneRules,
  detail: [requiredRule('详细地址')],
}

async function loadAddresses() {
  try {
    const res = await getAddressList()
    addresses.value = res.data
    const defaultAddr = addresses.value.find((addr) => addr.isDefault)
    selectedAddressId.value = defaultAddr?.id || addresses.value[0]?.id || 0
  } catch {
    // handled by interceptor
  }
}

function handleAddAddress() {
  Object.assign(addressForm, { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
  addressDialogVisible.value = true
}

async function handleSubmitAddress() {
  const valid = await addressFormRef.value?.validate()
  if (!valid) return

  addressSubmitting.value = true
  try {
    const res = await createAddress(addressForm)
    ElMessage.success('地址已添加')
    addressDialogVisible.value = false
    await loadAddresses()
    // auto-select the newly created address
    if (res.data?.id) {
      selectedAddressId.value = res.data.id
    }
  } finally {
    addressSubmitting.value = false
  }
}

async function handleSubmit() {
  submitting.value = true
  try {
    const res = await createOrder({
      addressId: selectedAddressId.value,
      cartItemIds: cartStore.checkedItems.map((item) => item.id),
      remark: '',
    })
    ElMessage.success('订单已提交')
    cartStore.fetchCart()
    router.push(`/orders/${res.data.id}/pay`)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAddresses()
})
</script>

<style lang="scss" scoped>
.order-confirm {
  h1 {
    font-size: 22px;
    margin-bottom: 20px;
  }

  .confirm-section {
    background: #fff;
    padding: 20px;
    border-radius: 8px;
    margin-bottom: 16px;

    h2 {
      font-size: 16px;
      margin-bottom: 0;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
  }

  .address-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .address-card {
    border: 2px solid #eee;
    border-radius: 8px;
    padding: 16px;
    cursor: pointer;
    transition: border-color 0.2s;

    &--active {
      border-color: #409eff;
    }

    &__header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    &__name {
      font-weight: 600;
    }

    &__phone {
      color: #666;
    }

    &__detail {
      font-size: 13px;
      color: #666;
    }
  }

  .order-product {
    display: flex;
    align-items: center;
    gap: 12px;

    &__img {
      width: 50px;
      height: 50px;
      border-radius: 4px;
      flex-shrink: 0;
    }

    &__spec {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }
  }

  .confirm-footer {
    position: sticky;
    bottom: 0;
    z-index: 100;
    background: #fff;
    margin-top: 20px;
    padding: 16px 20px;
    border-radius: 8px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 20px;
    box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.12);

    &__total {
      font-size: 14px;
    }

    &__price {
      font-size: 22px;
      font-weight: 700;
      color: #e6423a;
    }
  }
}
</style>
