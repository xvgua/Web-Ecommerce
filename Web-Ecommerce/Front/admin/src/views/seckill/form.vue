<template>
  <div class="seckill-form-page">
    <el-card>
      <div class="page-header">
        <h1 class="page-title">{{ isEdit ? '编辑秒杀活动' : '新增秒杀活动' }}</h1>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width: 960px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="如：618年中秒杀" maxlength="50" />
        </el-form-item>

        <el-form-item label="背景图片">
          <el-input v-model="form.backgroundImage" placeholder="输入背景图URL，留空则使用默认颜色">
            <template #append>
              <el-upload
                :show-file-list="false"
                :before-upload="(f: any) => handleBgUpload(f)"
                accept="image/*"
              >
                <el-button>本地上传</el-button>
              </el-upload>
            </template>
          </el-input>
          <div v-if="form.backgroundImage" class="bg-preview">
            <el-image :src="form.backgroundImage" fit="cover" style="width:200px;height:80px;border-radius:6px" />
          </div>
        </el-form-item>

        <el-form-item label="活动时间" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-divider content-position="left">秒杀商品</el-divider>

        <!-- Column headers -->
        <div class="product-header">
          <span class="col-product">商品</span>
          <span class="col-sku">规格</span>
          <span class="col-price">秒杀价</span>
          <span class="col-stock">秒杀库存</span>
          <span class="col-limit">限购</span>
          <span class="col-action">操作</span>
        </div>

        <!-- Product rows -->
        <div v-for="(item, index) in form.products" :key="index" class="product-row">
          <div class="col-product">
            <el-select
              v-model="item.productId"
              placeholder="搜索并选择商品"
              filterable
              remote
              :remote-method="(q: string) => searchProducts(q)"
              :loading="searchLoading"
              @focus="onFocusSelect"
              @change="(val: number) => onProductChange(index, val)"
              style="width: 100%"
            >
              <el-option
                v-for="p in productOptions"
                :key="p.id"
                :label="p.name + ' (¥' + (p.price || p.minPrice) + ')'"
                :value="p.id"
              />
            </el-select>
          </div>
          <div class="col-sku">
            <el-select v-model="item.skuId" placeholder="默认" clearable style="width: 100%" @change="(val: number) => onSkuChange(index, val)">
              <el-option label="默认规格" :value="0" />
              <el-option
                v-for="sku in item._skus"
                :key="sku.id"
                :label="sku.specName + ':' + sku.specValue"
                :value="sku.id"
              />
            </el-select>
          </div>
          <div class="col-price">
            <el-input-number v-model="item.seckillPrice" :min="0.01" :precision="2" :controls="false" placeholder="秒杀价" style="width: 100%" />
          </div>
          <div class="col-stock">
            <el-input-number v-model="item.seckillStock" :min="1" :controls="false" placeholder="库存" style="width: 100%" />
          </div>
          <div class="col-limit">
            <el-input-number v-model="item.limitPerUser" :min="1" :max="99" :controls="false" placeholder="1" style="width: 100%" />
          </div>
          <div class="col-action">
            <el-button type="danger" @click="removeProduct(index)" :disabled="form.products.length <= 1" :icon="Delete" circle size="small" />
          </div>
        </div>

        <el-button type="primary" @click="addProduct" :icon="Plus" style="margin-top: 8px; margin-bottom: 24px">添加商品</el-button>

        <el-divider />

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '创建活动' }}
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import request from '@/api/request'
import { getSeckillActivityById, createSeckillActivity, updateSeckillActivity, getProductList } from '@/api/admin'
import type { SeckillActivityForm, SeckillProductForm } from '@shared/types/seckill'

interface ProductRow extends SeckillProductForm {
  _skus?: any[]
}

const route = useRoute()
const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const searchLoading = ref(false)
const productOptions = ref<any[]>([])
const initialLoaded = ref(false)

const activityId = computed(() => route.params.id ? Number(route.params.id) : null)
const isEdit = computed(() => !!activityId.value)

const form = ref<{
  name: string
  backgroundImage: string
  timeRange: [string, string] | null
  products: ProductRow[]
}>({
  name: '',
  backgroundImage: '',
  timeRange: null,
  products: [{ productId: 0, skuId: 0, seckillPrice: 0, seckillStock: 10, limitPerUser: 1, _skus: [] }],
})

const rules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择活动时间', trigger: 'change' }],
}

async function loadInitialProducts() {
  if (initialLoaded.value) return
  initialLoaded.value = true
  searchLoading.value = true
  try {
    const res = await getProductList({ page: 1, pageSize: 50, status: 1 } as any)
    productOptions.value = res.data.records || []
  } catch { /* handled */ } finally { searchLoading.value = false }
}

function onFocusSelect() {
  if (!initialLoaded.value || productOptions.value.length === 0) {
    loadInitialProducts()
  }
}

async function searchProducts(query: string) {
  searchLoading.value = true
  try {
    const res = await getProductList({ page: 1, pageSize: 30, keyword: query } as any)
    productOptions.value = res.data.records || []
  } catch { /* handled */ } finally { searchLoading.value = false }
}

async function onProductChange(index: number, productId: number) {
  if (!productId) return
  // Fetch full product detail to get SKUs
  const product = productOptions.value.find(p => p.id === productId)
  if (!product) return
  form.value.products[index]._skus = product.skus || []
  form.value.products[index].skuId = 0
  if (!form.value.products[index].seckillPrice && product.price) {
    form.value.products[index].seckillPrice = Math.floor(product.price * 0.8 * 100) / 100
  }
}

function onSkuChange(index: number, skuId: number) {
  const row = form.value.products[index]
  if (skuId && row._skus) {
    const sku = row._skus.find((s: any) => s.id === skuId)
    if (sku && sku.price) {
      row.seckillPrice = Math.floor(sku.price * 0.8 * 100) / 100
    }
  }
}

function addProduct() {
  form.value.products.push({ productId: 0, skuId: 0, seckillPrice: 0, seckillStock: 10, limitPerUser: 1, _skus: [] })
}

function removeProduct(index: number) {
  form.value.products.splice(index, 1)
}

async function handleBgUpload(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await request.post('/admin/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    form.value.backgroundImage = res.data.url
    ElMessage.success('背景图上传成功')
  } catch { /* handled */ }
  return false
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!form.value.timeRange) {
    ElMessage.warning('请选择活动时间')
    return
  }

  // Validate product rows
  for (let i = 0; i < form.value.products.length; i++) {
    const p = form.value.products[i]
    if (!p.productId || p.productId <= 0) {
      ElMessage.warning(`第 ${i + 1} 行商品未选择`)
      return
    }
    if (!p.seckillPrice || p.seckillPrice <= 0) {
      ElMessage.warning(`第 ${i + 1} 行秒杀价未填写`)
      return
    }
    if (!p.seckillStock || p.seckillStock <= 0) {
      ElMessage.warning(`第 ${i + 1} 行秒杀库存未填写`)
      return
    }
  }

  submitting.value = true
  const data: SeckillActivityForm = {
    name: form.value.name,
    backgroundImage: form.value.backgroundImage || undefined,
    startTime: form.value.timeRange[0],
    endTime: form.value.timeRange[1],
    products: form.value.products.map(p => ({
      productId: p.productId,
      skuId: p.skuId || undefined,
      seckillPrice: p.seckillPrice,
      seckillStock: p.seckillStock,
      limitPerUser: p.limitPerUser || 1,
    })),
  }

  try {
    if (isEdit.value) {
      await updateSeckillActivity(activityId.value!, data)
      ElMessage.success('修改成功')
    } else {
      await createSeckillActivity(data)
      ElMessage.success('创建成功')
    }
    router.push('/seckill')
  } catch { /* handled */ } finally { submitting.value = false }
}

async function loadActivity() {
  if (!activityId.value) return
  try {
    const res = await getSeckillActivityById(activityId.value)
    const act = res.data
    form.value.name = act.name
    form.value.backgroundImage = act.backgroundImage || ''
    form.value.timeRange = [act.startTime, act.endTime]
    form.value.products = (act.products || []).map(p => ({
      productId: p.productId,
      skuId: p.skuId || 0,
      seckillPrice: p.seckillPrice,
      seckillStock: p.seckillStock,
      limitPerUser: p.limitPerUser || 1,
      _skus: [],
    }))
    // Pre-load product options and SKU data
    if (form.value.products.length > 0) {
      const res2 = await getProductList({ page: 1, pageSize: 100, status: 1 } as any)
      productOptions.value = res2.data.records || []
      initialLoaded.value = true
      for (let i = 0; i < form.value.products.length; i++) {
        const p = productOptions.value.find((po: any) => po.id === form.value.products[i].productId)
        if (p) {
          form.value.products[i]._skus = p.skus || []
        }
      }
    }
  } catch { /* handled */ }
}

onMounted(() => {
  if (isEdit.value) {
    loadActivity()
  }
})
</script>

<style lang="scss" scoped>
.seckill-form-page { max-width: 1200px; }

.page-header {
  margin-bottom: 28px;
}

/* Column header */
.product-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #f9f7f4;
  border-radius: var(--org-radius-sm);
  margin-bottom: 12px;
  font-size: 12px;
  font-weight: 700;
  color: var(--org-text-secondary);
  text-transform: uppercase;
  letter-spacing: .5px;
}

/* Product row */
.product-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 10px;
  background: var(--org-surface-warm);
  border-radius: var(--org-radius-md);
  border: 1px solid var(--org-border-soft);
  transition: all var(--org-duration) var(--org-ease-soft);

  &:hover {
    border-color: var(--org-accent-light);
  }
}

/* Column widths */
.col-product { width: 260px; flex-shrink: 0; }
.col-sku     { width: 130px; flex-shrink: 0; }
.col-price   { width: 110px; flex-shrink: 0; }
.col-stock   { width: 100px; flex-shrink: 0; }
.col-limit   { width: 80px;  flex-shrink: 0; }
.col-action  { width: 40px;  flex-shrink: 0; text-align: center; }
</style>
