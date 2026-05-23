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
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" :step="1" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品主图" prop="mainImage">
          <div class="upload-area">
            <div class="upload-preview" v-if="form.mainImage">
              <ProductPlaceholder :seed="form.name || 'product'" :size="120" />
            </div>
            <el-upload
              action="/api/admin/upload"
              :show-file-list="false"
              :on-success="handleMainImageSuccess"
              :before-upload="beforeUpload"
            >
              <el-button :type="form.mainImage ? '' : 'primary'">
                {{ form.mainImage ? '更换主图' : '上传主图' }}
              </el-button>
            </el-upload>
          </div>
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
        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">保存</el-button>
          <el-button size="large" @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createProduct, updateProduct, getProductById, getCategoryList } from '@/api/admin'
import { requiredRule, priceRules } from '@shared/validators'
import type { Category, ProductForm } from '@shared/types/product'
import ProductPlaceholder from '@/components/common/ProductPlaceholder.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const isEdit = computed(() => !!route.params.id)
const submitting = ref(false)
const categories = ref<Category[]>([])

const form = reactive<ProductForm>({
  name: '',
  categoryId: 0,
  price: 0,
  stock: 0,
  detail: '',
  description: '',
  mainImage: '',
  images: [],
  status: 1,
})

const rules: FormRules = {
  name: [requiredRule('商品名称')],
  categoryId: [requiredRule('商品分类')],
  price: priceRules,
  stock: [requiredRule('库存')],
}

function handleMainImageSuccess(response: { data: string }) {
  form.mainImage = response.data
}

function beforeUpload(file: File) {
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
  }
  return isLt2M
}

async function handleSubmit() {
  const valid = await formRef.value?.validate()
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(Number(route.params.id), form)
      ElMessage.success('商品已更新')
    } else {
      await createProduct(form)
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
  }
})
</script>

<style lang="scss" scoped>
.product-form-page {
  max-width: 800px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}

.product-form {
  :deep(.el-input-number) { width: 100%; }
}

.upload-area {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.upload-preview {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
}
</style>
