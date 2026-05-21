<template>
  <div class="product-manage">
    <div class="page-header">
      <h1 class="page-title">商品管理</h1>
      <el-button type="primary" @click="$router.push('/products/create')">
        <el-icon><Plus /></el-icon> 新增商品
      </el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索商品名称" clearable class="toolbar-search" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="categoryId" placeholder="选择分类" clearable @change="handleSearch" class="toolbar-select">
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="商品状态" clearable @change="handleSearch" class="toolbar-select-sm">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <div class="table-card">
      <el-table :data="products" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="主图" width="90">
          <template #default="{ row }">
            <div style="width:60px;height:60px;border-radius:6px;overflow:hidden">
              <ProductPlaceholder :seed="row.name + row.id" :size="60" />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="价格" width="110" sortable>
          <template #default="{ row }">
            <span class="price-cell">{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" align="center" />
        <el-table-column prop="sales" label="销量" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="dark">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
            <el-button
              text
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadProducts"
        @size-change="loadProducts"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getProductList, deleteProduct, updateProduct, getCategoryList } from '@/api/admin'
import { formatPrice } from '@/utils/format'
import type { Product, Category, ProductForm } from '@shared/types/product'
import ProductPlaceholder from '@/components/common/ProductPlaceholder.vue'

const products = ref<Product[]>([])
const categories = ref<Category[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const categoryId = ref<number | ''>('')
const statusFilter = ref<number | ''>('')

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      status: statusFilter.value !== '' ? (statusFilter.value as number) : undefined,
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
  const label = newStatus === 1 ? '上架' : '下架'
  await ElMessageBox.confirm(`确定要${label}该商品吗？`, '提示', { type: 'warning' })
  await updateProduct(row.id, { ...row, status: newStatus } as ProductForm)
  ElMessage.success(`已${label}`)
  loadProducts()
}

onMounted(async () => {
  loadProducts()
  const res = await getCategoryList()
  categories.value = res.data
})
</script>

<style lang="scss" scoped>
.product-manage { max-width: 1400px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;

  &-search { width: 260px; }
  &-select { width: 160px; }
  &-select-sm { width: 120px; }
}

.table-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
}

.price-cell {
  color: #e6423a;
  font-weight: 600;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
