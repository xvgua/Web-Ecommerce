<template>
  <div class="product-manage">
    <h1 class="page-title">商品管理</h1>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索商品名称" clearable style="width: 240px" @keyup.enter="handleSearch" />
        <el-select v-model="categoryId" placeholder="选择分类" clearable style="width: 160px" @change="handleSearch">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="$router.push('/products/create')">新增商品</el-button>
      </div>
    </div>

    <el-table :data="products" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="主图" width="80">
        <template #default="{ row }">
          <el-image :src="row.mainImage" fit="cover" style="width: 50px; height: 50px; border-radius: 4px" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="180" />
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column label="价格" width="100">
        <template #default="{ row }">{{ formatPrice(row.price) }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="sales" label="销量" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
          <el-button text :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button text type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap" v-if="total > 0" style="margin-top: 16px">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadProducts"
        @size-change="loadProducts"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getProductList, deleteProduct, updateProduct, getCategoryList } from '@/api/admin'
import { formatPrice } from '@/utils/format'
import type { Product, Category } from '@shared/types/product'

const products = ref<Product[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const categoryId = ref<number | ''>('')

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
    })
    products.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadProducts()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该商品吗？', '提示', { type: 'warning' })
  await deleteProduct(id)
  ElMessage.success('已删除')
  loadProducts()
}

async function toggleStatus(row: Product) {
  const newStatus = row.status === 1 ? 0 : 1
  await updateProduct(row.id, { ...row, status: newStatus } as ProductForm)
  ElMessage.success(newStatus === 1 ? '已上架' : '已下架')
  loadProducts()
}

import type { ProductForm } from '@shared/types/product'

onMounted(async () => {
  loadProducts()
  const res = await getCategoryList()
  categories.value = res.data
})
</script>
