<template>
  <div class="order-confirm">
    <h1 class="page-title">确认订单</h1>

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
      <el-table :data="isDirectBuy ? [directBuyItem] : cartStore.checkedItems">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div class="order-product">
              <div class="order-product__img">
                <ProductImage :src="row.productImage" :seed="(row.productName || row.name || '') + (row.productId || row.id || 0)" fit="cover" />
              </div>
              <div>
                <div>{{ row.productName || row.name }}</div>
                <div class="order-product__spec">{{ row.specDesc || row.specName }}</div>
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

    <div class="confirm-section" v-if="availableCoupons.length > 0 || !isDirectBuy">
      <h2>优惠券</h2>
      <div class="coupon-select" v-if="availableCoupons.length">
        <div
          :class="['coupon-option', { 'coupon-option--active': selectedCouponIds.length === 0 }]"
          @click="selectedCouponIds = []"
        >
          <span>不使用优惠券</span>
        </div>
        <div
          v-for="uc in availableCoupons"
          :key="uc.id"
          :class="['coupon-option', {
            'coupon-option--active': selectedCouponIds.includes(uc.id),
            'coupon-option--disabled': isCouponDisabled(uc)
          }]"
          @click="!isCouponDisabled(uc) && selectCoupon(uc.id)"
        >
          <span class="coupon-option__name">
            {{ uc.coupon?.name }}
            <el-tag v-if="uc.coupon?.stackable" size="small" type="success" style="margin-left:8px">可叠加</el-tag>
          </span>
          <span class="coupon-option__discount">-{{ formatPrice(getCouponDiscount(uc)) }}</span>
        </div>
      </div>
      <el-empty v-else description="暂无可用优惠券" :image-size="40" />
    </div>

    <div class="confirm-section">
      <h2>费用明细</h2>
      <div class="fee-breakdown">
        <div class="fee-row">
          <span>商品合计</span>
          <span>{{ formatPrice(orderTotal) }}</span>
        </div>
        <div class="fee-row fee-row--discount" v-if="couponDiscount > 0">
          <span>优惠券抵扣</span>
          <span>-{{ formatPrice(couponDiscount) }}</span>
        </div>
        <div class="fee-row fee-row--total">
          <span>应付金额</span>
          <span class="fee-row__price">{{ formatPrice(payAmount) }}</span>
        </div>
      </div>
    </div>

    <div class="confirm-footer">
      <div class="confirm-footer__total">
        应付：<span class="confirm-footer__price">{{ formatPrice(payAmount) }}</span>
      </div>
      <el-button type="danger" size="large" :loading="submitting" :disabled="!selectedAddressId" @click="handleSubmit">
        提交订单
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { getAddressList, createAddress } from '@/api/user'
import { createOrder } from '@/api/order'
import { getProductById } from '@/api/product'
import { getAvailableForOrder } from '@/api/coupon'
import { formatPrice } from '@/utils/format'
import { requiredRule, phoneRules } from '@shared/validators'
import type { Address, AddressForm } from '@shared/types/user'
import type { UserCoupon } from '@shared/types/coupon'
import ProductImage from '@/components/common/ProductImage.vue'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()

const isDirectBuy = computed(() => !!route.query.productId)

const directBuyItem = ref<any>({
  productId: 0,
  productImage: '',
  productName: '',
  name: '',
  specDesc: '',
  price: 0,
  quantity: 1,
})

const addresses = ref<Address[]>([])
const selectedAddressId = ref<number>(0)
const submitting = ref(false)

const availableCoupons = ref<UserCoupon[]>([])
const selectedCouponIds = ref<number[]>([])

// Whether any selected coupon is non-stackable
const hasNonStackableSelected = computed(() => {
  return selectedCouponIds.value.some(id => {
    const uc = availableCoupons.value.find(c => c.id === id)
    return uc?.coupon && (uc.coupon.stackable === 0 || uc.coupon.stackable == null)
  })
})

const orderTotal = computed(() => {
  if (isDirectBuy.value) {
    return directBuyItem.value.price * directBuyItem.value.quantity
  }
  return cartStore.checkedTotal
})

const couponDiscount = computed(() => {
  return selectedCouponIds.value.reduce((sum, id) => {
    const uc = availableCoupons.value.find(c => c.id === id)
    return sum + (uc ? getCouponDiscount(uc) : 0)
  }, 0)
})

const payAmount = computed(() => {
  return Math.max(0, orderTotal.value - couponDiscount.value)
})

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

function getCouponDiscount(uc: UserCoupon): number {
  if (!uc.coupon) return 0
  const amount = orderTotal.value
  if (uc.coupon.type === 1) {
    return uc.coupon.discount
  } else if (uc.coupon.type === 2) {
    return Math.round(amount * (1 - uc.coupon.discount) * 100) / 100
  }
  return 0
}

function isCouponDisabled(uc: UserCoupon): boolean {
  if (selectedCouponIds.value.length === 0) return false
  const coupon = uc.coupon
  const isNonStackable = !coupon || coupon.stackable === 0 || coupon.stackable == null
  // If a non-stackable coupon is already selected, disable everything else
  if (hasNonStackableSelected.value) return true
  // If we're selecting another coupon and it's non-stackable, only allow if nothing is selected
  if (isNonStackable) return true
  return false
}

function selectCoupon(id: number) {
  const idx = selectedCouponIds.value.indexOf(id)
  if (idx >= 0) {
    selectedCouponIds.value.splice(idx, 1)
  } else {
    // If selecting a non-stackable coupon, clear all others
    const uc = availableCoupons.value.find(c => c.id === id)
    const isNonStackable = !uc?.coupon || uc.coupon.stackable === 0 || uc.coupon.stackable == null
    if (isNonStackable) {
      selectedCouponIds.value = [id]
    } else {
      selectedCouponIds.value.push(id)
    }
  }
}

async function loadDirectBuyProduct() {
  const productId = Number(route.query.productId)
  const skuId = Number(route.query.skuId) || 0
  const qty = Number(route.query.quantity) || 1
  try {
    const res = await getProductById(productId)
    const p = res.data
    let price = p.price
    let specDesc = ''
    let image = p.mainImage || ''
    if (skuId && p.skus) {
      const sku = p.skus.find((s: any) => s.id === skuId)
      if (sku) {
        price = sku.price
        specDesc = sku.specName || sku.specValue || ''
        image = sku.image || image
      }
    }
    directBuyItem.value = {
      productId: p.id,
      productImage: image,
      productName: p.name,
      name: p.name,
      specDesc,
      specName: specDesc,
      price,
      quantity: qty,
      id: p.id,
    }
  } catch { /* ignore */ }
}

async function loadAvailableCoupons() {
  try {
    const res = await getAvailableForOrder(orderTotal.value)
    availableCoupons.value = res.data || []
  } catch { /* ignore */ }
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
  if (!selectedAddressId.value) {
    ElMessage.warning('请先选择或添加收货地址')
    return
  }
  submitting.value = true
  try {
    const orderData: any = {
      addressId: selectedAddressId.value,
      remark: '',
      userCouponIds: selectedCouponIds.value.length > 0 ? selectedCouponIds.value : undefined,
    }
    if (isDirectBuy.value) {
      orderData.productId = directBuyItem.value.productId
      orderData.skuId = Number(route.query.skuId) || 0
      orderData.quantity = directBuyItem.value.quantity
    } else {
      orderData.cartItemIds = cartStore.checkedItems.map((item) => item.id)
    }
    const res = await createOrder(orderData)
    ElMessage.success('订单已提交')
    if (!isDirectBuy.value) {
      cartStore.fetchCart()
    }
    router.push(`/orders/${res.data.id}/pay`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (isDirectBuy.value) {
    await loadDirectBuyProduct()
  }
  loadAddresses()
  loadAvailableCoupons()
})
</script>

<style lang="scss" scoped>
.order-confirm {
  max-width: 1000px;
  margin: 0 auto;

  .page-title {
    font-size: 22px;
    font-weight: 700;
    margin-bottom: 20px;
  }

  .confirm-section {
    background: #fff;
    padding: 24px;
    border-radius: 12px;
    margin-bottom: 16px;

    h2 {
      font-size: 16px;
      font-weight: 600;
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
    border-radius: 10px;
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
      width: 56px;
      height: 56px;
      border-radius: 6px;
      overflow: hidden;
      flex-shrink: 0;
    }

    &__spec {
      font-size: 12px;
      color: #999;
      margin-top: 2px;
    }
  }

  .coupon-select {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .coupon-option {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border: 2px solid #eee;
    border-radius: 8px;
    cursor: pointer;
    transition: border-color .2s;

    &--active {
      border-color: #409eff;
      background: rgba(64, 158, 255, .04);
    }

    &--disabled {
      opacity: .45;
      cursor: not-allowed;

      &:hover {
        border-color: #eee;
      }
    }

    &:hover:not(.coupon-option--disabled) {
      border-color: #409eff;
    }

    &__name {
      font-weight: 500;
    }

    &__discount {
      color: #e6423a;
      font-weight: 600;
    }
  }

  .fee-breakdown {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .fee-row {
    display: flex;
    justify-content: space-between;
    font-size: 14px;
    color: #666;

    &--discount {
      color: #e6423a;
    }

    &--total {
      font-size: 16px;
      font-weight: 600;
      color: #1a1a1a;
      padding-top: 10px;
      border-top: 1px solid #eee;
    }

    &__price {
      font-size: 20px;
      font-weight: 700;
      color: #e6423a;
    }
  }

  .confirm-footer {
    position: sticky;
    bottom: 0;
    z-index: 100;
    background: #fff;
    margin-top: 20px;
    padding: 16px 24px;
    border-radius: 12px;
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
