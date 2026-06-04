<template>
  <div class="product-manage">
    <div class="page-header">
      <h1 class="page-title">商品管理</h1>
      <el-button type="primary" @click="$router.push('/products/create')">
        <el-icon><Plus /></el-icon> 新增商品
      </el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索商品名称" clearable maxlength="100" class="toolbar-search" @input="debouncedSearch" @keyup.enter="handleSearch">
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
      <el-button @click="handleExport">
        <el-icon><Download /></el-icon> 导出Excel
      </el-button>
      <el-button @click="handleImportClick">
        <el-icon><Upload /></el-icon> 导入Excel
      </el-button>
      <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="handleFileChange" />
    </div>

    <div class="table-card">
      <el-table :data="products" v-loading="loading" stripe row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="sku-expand" v-if="row.skus && row.skus.length > 0">
              <div class="sku-expand__table">
                <div
                  v-for="(sku, idx) in row.skus"
                  :key="sku.id"
                  class="sku-row"
                  :class="{ 'sku-row--off': sku.status === 0 }"
                >
                  <span class="sku-row__offset" />
                  <span class="sku-row__id">#{{ idx + 1 }}</span>
                  <div class="sku-row__img">
                    <el-image v-if="sku.image" :src="sku.image" fit="cover">
                      <template #error>
                        <ProductPlaceholder :seed="sku.specName + sku.id" :size="40" />
                      </template>
                    </el-image>
                    <ProductPlaceholder v-else :seed="sku.specName + sku.id" :size="40" />
                  </div>
                  <span class="sku-row__name" :title="sku.specName">{{ sku.specName }}</span>
                  <span class="sku-row__category" />
                  <span class="sku-row__cell sku-row__cell--price">{{ formatPrice(sku.price) }}</span>
                  <span class="sku-row__cell sku-row__cell--stock">{{ sku.stock }}</span>
                  <span class="sku-row__cell sku-row__cell--sales">{{ sku.sales }}</span>
                  <span class="sku-row__cell sku-row__cell--status">
                    <el-tag
                      :type="sku.status === 1 ? 'success' : 'info'"
                      size="small"
                      effect="dark"
                    >
                      {{ sku.status === 1 ? '上架' : '下架' }}
                    </el-tag>
                  </span>
                  <span class="sku-row__cell sku-row__cell--action">
                    <el-button
                      text
                      :type="sku.status === 1 ? 'warning' : 'success'"
                      size="small"
                      @click.stop="toggleSku(row.id, sku)"
                    >
                      {{ sku.status === 1 ? '下架' : '上架' }}
                    </el-button>
                    <el-button
                      text
                      type="danger"
                      size="small"
                      @click.stop="handleDeleteSku(row.id, sku)"
                    >
                      删除
                    </el-button>
                  </span>
                </div>
              </div>
            </div>
            <span v-else class="sku-none">暂无规格</span>
          </template>
        </el-table-column>
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
          <template #default>
            <span class="price-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="80" align="center">
          <template #default="{ row }">
            {{ skuTotalStock(row) }}
          </template>
        </el-table-column>
        <el-table-column label="销量" width="80" align="center">
          <template #default="{ row }">
            {{ skuTotalSales(row) }}
          </template>
        </el-table-column>
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

    <!-- Import result dialog -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="520px">
      <div v-if="importResult" class="import-result">
        <div class="import-result__stats">
          <div class="import-result__stat import-result__stat--success">
            <span class="import-result__num">{{ importResult.successCount }}</span>
            <span>成功</span>
          </div>
          <div class="import-result__stat import-result__stat--fail">
            <span class="import-result__num">{{ importResult.failCount }}</span>
            <span>失败</span>
          </div>
          <div class="import-result__stat">
            <span class="import-result__num">{{ importResult.totalCount }}</span>
            <span>总计</span>
          </div>
        </div>
        <div v-if="importResult.errors?.length" class="import-result__errors">
          <h4>错误详情</h4>
          <div v-for="(e, i) in importResult.errors" :key="i" class="import-result__error">
            第{{ e.row }}行：{{ e.reason }}
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false; loadProducts()">关闭并刷新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search, Plus, Download, Upload } from '@element-plus/icons-vue'
import { useDebounceFn } from '@vueuse/core'
import { getProductList, deleteProduct, updateProduct, getCategoryList, toggleSkuStatus, deleteSku, exportProducts, importProducts } from '@/api/admin'
import { formatPrice } from '@/utils/format'
import type { Product, ProductSku, Category, ProductForm } from '@shared/types/product'
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
      keyword: keyword.value.trim() || undefined,
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

const debouncedSearch = useDebounceFn(() => {
  handleSearch()
}, 300)

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

async function toggleSku(productId: number, sku: ProductSku) {
  const newStatus = sku.status === 1 ? 0 : 1
  const label = newStatus === 1 ? '上架' : '下架'
  await ElMessageBox.confirm(`确定要${label}规格「${sku.specName}」吗？`, '提示', { type: 'warning' })
  await toggleSkuStatus(productId, sku.id, newStatus)
  ElMessage.success(`已${label}`)
  loadProducts()
}

async function handleDeleteSku(productId: number, sku: ProductSku) {
  await ElMessageBox.confirm(`确定要删除规格「${sku.specName}」吗？`, '提示', { type: 'warning' })
  await deleteSku(productId, sku.id)
  ElMessage.success('已删除')
  loadProducts()
}

function skuTotalStock(row: Product) {
  if (row.skus && row.skus.length > 0) {
    return row.skus.reduce((sum, sku) => sum + sku.stock, 0)
  }
  return row.stock
}

function skuTotalSales(row: Product) {
  if (row.skus && row.skus.length > 0) {
    return row.skus.reduce((sum, sku) => sum + sku.sales, 0)
  }
  return row.sales
}

const fileInput = ref<HTMLInputElement>()
const importResultVisible = ref(false)
const importResult = ref<{ successCount: number; failCount: number; totalCount: number; errors: { row: number; reason: string }[] } | null>(null)

async function handleExport() {
  try {
    const blob = await exportProducts({
      page: 1,
      pageSize: 9999,
      keyword: keyword.value.trim() || undefined,
      categoryId: categoryId.value || undefined,
      status: statusFilter.value !== '' ? (statusFilter.value as number) : undefined,
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '商品列表.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

function handleImportClick() {
  fileInput.value?.click()
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const res = await importProducts(file)
    importResult.value = res.data
    importResultVisible.value = true
  } catch {
    ElMessage.error('导入失败')
  } finally {
    input.value = ''
  }
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

.price-empty {
  color: #c0c4cc;
}

.sku-expand {
  padding: 0;

  &__table {
    position: relative;
  }
}

.sku-row {
  display: flex;
  align-items: center;
  padding: 7px 0;
  border-bottom: 1px dashed #ebeef5;
  transition: background .15s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f8f9fb;
  }

  &--off {
    opacity: .5;

    .sku-row__img {
      filter: grayscale(1);
    }
  }

  /* 偏移量：跳过 expand(48px) + ID(70px) + 主图(90px)，到达商品名称列 */
  &__offset {
    width: 208px;
    flex-shrink: 0;
  }

  &__id {
    width: 32px;
    flex-shrink: 0;
    font-size: 12px;
    color: #909399;
    text-align: center;
  }

  &__img {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    overflow: hidden;
    flex-shrink: 0;
    background: #f0f0f0;
    margin-right: 10px;

    :deep(.el-image) {
      width: 100%;
      height: 100%;
    }
  }

  &__name {
    flex: 1;
    min-width: 0;
    font-size: 13px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding-right: 12px;
  }

  /* 分类列占位：对齐主表分类列 */
  &__category {
    width: 100px;
    flex-shrink: 0;
  }

  /* 单元格：固定宽度，与主表头对齐 */
  &__cell {
    flex-shrink: 0;
    text-align: center;
    font-size: 13px;
    color: #606266;

    &--price {
      width: 110px;
      color: #e6423a;
      font-weight: 600;
      text-align: right;
      padding-right: 8px;
    }

    &--stock { width: 80px; }
    &--sales { width: 80px; }
    &--status {
      width: 80px;
      display: flex;
      justify-content: center;
    }
    &--action { width: 200px; }
  }
}

.sku-none {
  font-size: 13px;
  color: #c0c4cc;
  padding: 8px 0;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.import-result {
  &__stats {
    display: flex;
    justify-content: center;
    gap: 40px;
    margin-bottom: 16px;
  }

  &__stat {
    text-align: center;
    font-size: 14px;
    color: #666;

    &--success .import-result__num { color: #67c23a; }
    &--fail .import-result__num { color: #f56c6c; }
  }

  &__num {
    display: block;
    font-size: 32px;
    font-weight: 700;
  }

  &__errors {
    h4 {
      font-size: 14px;
      margin-bottom: 8px;
      color: #333;
    }
  }

  &__error {
    font-size: 13px;
    color: #f56c6c;
    padding: 4px 0;
    border-bottom: 1px dashed #f0f0f0;

    &:last-child { border-bottom: none; }
  }
}
</style>
