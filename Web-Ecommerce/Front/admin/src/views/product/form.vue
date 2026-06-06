<template>
  <div class="product-form-page">
    <div class="page-header">
      <h1 class="page-title">{{ isEdit ? '编辑商品' : '新增商品' }}</h1>
    </div>

    <el-card shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="product-form">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择二级分类" filterable clearable>
            <el-option
              v-for="cat in subCategories"
              :key="cat.id"
              :label="cat.label"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="价格" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0"
                :precision="2"
                disabled
                controls-position="right"
              />
              <div class="field-hint">{{ form.skus && form.skus.length > 0 ? '取自规格最低价' : '添加规格后自动计算' }}</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存" prop="stock">
              <el-input-number
                v-model="form.stock"
                :min="0"
                :step="1"
                disabled
                controls-position="right"
              />
              <div class="field-hint">{{ form.skus && form.skus.length > 0 ? '取自规格库存总和' : '添加规格后自动计算' }}</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品图片" prop="images">
          <div class="images-upload">
            <div
              v-for="(img, i) in form.images"
              :key="i"
              class="images-upload__item"
            >
              <img :src="img" class="images-upload__thumb" />
              <span class="images-upload__tag" v-if="i === 0">主图</span>
              <el-button
                class="images-upload__remove"
                type="danger"
                :icon="Delete"
                circle
                size="small"
                @click="removeImage(i)"
              />
            </div>
            <el-upload
              action="/api/admin/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleImageUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
            >
              <el-button :type="form.images.length ? '' : 'primary'" :disabled="form.images.length >= 9">
                {{ form.images.length ? '+ 添加图片' : '上传图片' }}
              </el-button>
            </el-upload>
          </div>
          <div class="field-hint">{{ form.images.length ? `已选 ${form.images.length} 张，第一张为主图` : '请上传商品图片（最多 9 张）' }}</div>
        </el-form-item>
        <el-form-item label="商品状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">上架</el-radio-button>
            <el-radio-button :value="0">下架</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="详细参数" prop="detail">
          <el-input v-model="form.detail" type="textarea" :rows="8" placeholder="请输入商品详细参数（支持 HTML 表格）" />
        </el-form-item>
        <el-form-item label="商品详情" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="8" placeholder="请输入商品描述（支持 HTML）" />
        </el-form-item>
        <el-form-item label="商品规格">
          <div class="sku-list">
            <div class="sku-list__header">
              <span class="sku-list__label">规格组</span>
              <span class="sku-list__label">规格值</span>
              <span class="sku-list__label sku-list__label--img">图片</span>
              <span class="sku-list__label">价格</span>
              <span class="sku-list__label">库存</span>
              <span class="sku-list__label"></span>
            </div>
            <div v-for="(sku, index) in form.skus" :key="index" class="sku-row">
              <el-input v-model="sku.specName" placeholder="如：颜色分类" />
              <el-input v-model="sku.specValue" placeholder="如：深棕色" />
              <div class="sku-row__img">
                <img v-if="sku.image" :src="sku.image" class="sku-row__thumb" />
                <el-upload
                  v-else
                  action="/api/admin/upload"
                  :headers="uploadHeaders"
                  :show-file-list="false"
                  :on-success="(res: any) => { sku.image = res.data.url; ElMessage.success('规格图上传成功') }"
                  :on-error="handleUploadError"
                  :before-upload="beforeUpload"
                >
                  <el-button text size="small" type="primary">上传</el-button>
                </el-upload>
                <el-button v-if="sku.image" text size="small" type="danger" @click="sku.image = ''">移除</el-button>
              </div>
              <el-input-number v-model="sku.price" :min="0" :precision="2" controls-position="right" />
              <el-input-number v-model="sku.stock" :min="0" controls-position="right" />
              <el-button type="danger" :icon="Delete" circle @click="removeSku(index)" />
            </div>
            <el-button type="primary" plain size="small" @click="addSku">+ 添加规格</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">保存</el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createProduct, updateProduct, getProductById, getCategoryList } from '@/api/admin'
import { Delete } from '@element-plus/icons-vue'
import { requiredRule } from '@shared/validators'
import { ADMIN_TOKEN_KEY } from '@shared/constants'
import type { Category, ProductForm, SkuForm } from '@shared/types/product'

const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem(ADMIN_TOKEN_KEY) || ''}` }

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const isEdit = computed(() => !!route.params.id)
const submitting = ref(false)
const categories = ref<Category[]>([])

interface FlatCategory {
  id: number
  label: string
}

const subCategories = computed<FlatCategory[]>(() => {
  const result: FlatCategory[] = []
  function walk(list: Category[], parentName?: string) {
    for (const cat of list) {
      const fullName = parentName ? `${parentName} > ${cat.name}` : cat.name
      if (cat.children && cat.children.length > 0) {
        walk(cat.children, fullName)
      } else {
        result.push({ id: cat.id, label: fullName })
      }
    }
  }
  walk(categories.value)
  return result
})

interface ProductFormState extends Omit<ProductForm, 'categoryId'> {
  categoryId: number | undefined
}

const form = reactive<ProductFormState>({
  name: '',
  categoryId: undefined,
  price: 0,
  stock: 0,
  detail: '',
  description: '',
  mainImage: '',
  images: [],
  status: 1,
  skus: [],
})

const rules: FormRules = {
  name: [requiredRule('商品名称')],
  categoryId: [requiredRule('商品分类')],
}

function syncPriceAndStock() {
  const skus = form.skus || []
  if (skus.length > 0) {
    form.price = Math.min(...skus.map(s => s.price || 0))
    form.stock = skus.reduce((sum, s) => sum + (s.stock || 0), 0)
  } else {
    form.price = 0
    form.stock = 0
  }
}

watch(() => form.skus, syncPriceAndStock, { deep: true })

function handleImageUploadSuccess(response: { data: { url: string } }) {
  const url = response.data.url
  if (!url) {
    ElMessage.error('图片上传失败')
    return
  }
  form.images.push(url)
  if (!form.mainImage) form.mainImage = url
  ElMessage.success('图片上传成功')
}

function removeImage(index: number) {
  form.images.splice(index, 1)
  form.mainImage = form.images[0] || ''
}

function handleUploadError() {
  ElMessage.error('图片上传失败，请重试')
}

function beforeUpload(file: File) {
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
  }
  return isLt5M
}

function addSku() {
  if (!form.skus) form.skus = []
  form.skus.push({ specName: '', specValue: '', price: 0, stock: 0, image: '' })
  syncPriceAndStock()
}

function removeSku(index: number) {
  form.skus?.splice(index, 1)
  syncPriceAndStock()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    const payload: ProductForm = {
      ...form,
      categoryId: form.categoryId!, // validated above, guaranteed non-undefined
      images: JSON.stringify(form.images || []),
    }
    if (!payload.skus) payload.skus = []
    if (isEdit.value) {
      await updateProduct(Number(route.params.id), payload)
      ElMessage.success('商品已更新')
    } else {
      await createProduct(payload)
      ElMessage.success('商品已创建')
    }
    router.push('/products')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const res = await getCategoryList()
  categories.value = res.data
  if (isEdit.value) {
    const productRes = await getProductById(Number(route.params.id))
    const p = productRes.data
    form.name = p.name
    form.categoryId = p.categoryId
    form.price = p.price
    form.stock = p.stock
    form.detail = p.detail || ''
    form.description = p.description
    form.mainImage = p.mainImage
    form.images = p.images
    form.status = p.status
    form.skus = (p.skus || []).map(s => ({ specName: s.specName, price: s.price, stock: s.stock, image: s.image || '' }))
  }
})
</script>

<style lang="scss" scoped>
.product-form-page {
  max-width: 840px;
}

.page-header {
  margin-bottom: 28px;
}

.product-form {
  :deep(.el-input-number) { width: 100%; }
}

.field-hint {
  font-size: 12px;
  color: var(--org-text-muted);
  line-height: 1.4;
  margin-top: 2px;
  font-weight: 500;
}

.images-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;

  &__item {
    position: relative;
    width: 100px;
    height: 100px;
    border-radius: var(--org-radius-md);
    overflow: hidden;
    border: 1px solid var(--org-border);
    flex-shrink: 0;
  }

  &__thumb {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &__tag {
    position: absolute;
    top: 4px;
    left: 4px;
    background: var(--brand-primary);
    color: #fff;
    font-size: 11px;
    padding: 1px 6px;
    border-radius: 3px;
    font-weight: 500;
  }

  &__remove {
    position: absolute;
    top: 2px;
    right: 2px;
    opacity: 0;
    transition: opacity var(--transition-fast);
  }

  &__item:hover &__remove { opacity: 1; }
}

.sku-list {
  width: 100%;

  &__header {
    display: flex;
    gap: 10px;
    margin-bottom: 10px;
  }

  &__label {
    font-size: 12px;
    color: var(--org-text-muted);
    font-weight: 600;
    &:nth-child(1) { flex: 1; }
    &:nth-child(2) { flex: 1; }
    &:nth-child(3) { width: 70px; }
    &:nth-child(4) { width: 130px; }
    &:nth-child(5) { width: 110px; }
  }
}

.sku-row {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
  padding: 12px;
  background: var(--org-surface-hover);
  border-radius: var(--org-radius-md);
  border: 1px solid var(--org-border-soft);

  .el-input { flex: 1; min-width: 0; }
  .el-input-number { width: 130px; }
  .el-input-number + .el-input-number { width: 110px; }

  &__img {
    width: 80px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  &__thumb {
    width: 40px;
    height: 40px;
    border-radius: var(--org-radius-sm);
    object-fit: cover;
    border: 1px solid var(--org-border);
  }
}
</style>
