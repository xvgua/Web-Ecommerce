<template>
  <div class="product-list-page">
    <div class="filters">
      <el-input v-model="keyword" placeholder="搜索商品" clearable size="large" class="filter-search" @keyup.enter="handleSearch" />
      <el-radio-group v-model="sort" @change="handleSortChange">
        <el-radio-button value="">默认</el-radio-button>
        <el-radio-button value="sales_desc">销量优先</el-radio-button>
        <el-radio-button value="price_asc">价格升序</el-radio-button>
        <el-radio-button value="price_desc">价格降序</el-radio-button>
        <el-radio-button value="newest">最新</el-radio-button>
      </el-radio-group>
    </div>

    <div class="product-grid" v-loading="loading">
      <product-card v-for="item in products" :key="item.id" :product="item" />
    </div>
    <el-empty v-if="!loading && !products.length" description="暂无商品" />

    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 40, 60]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadProducts"
        @size-change="loadProducts"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProductList } from '@/api/product'
import type { Product } from '@shared/types/product'
import ProductCard from '@/components/business/ProductCard.vue'

const route = useRoute()

const products = ref<Product[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const keyword = ref((route.query.keyword as string) || '')
const sort = ref('')

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProductList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      sort: sort.value as ProductQuery['sort'] || undefined,
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

function handleSortChange() {
  page.value = 1
  loadProducts()
}

import type { ProductQuery } from '@shared/types/product'

onMounted(() => {
  loadProducts()
})
</script>

<style lang="scss" scoped>
.product-list-page {
  .filters {
    display: flex;
    gap: 16px;
    align-items: center;
    margin-bottom: 24px;
    flex-wrap: wrap;

    .filter-search {
      width: 280px;
    }
  }

  .product-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;

    @media (max-width: 1024px) {
      grid-template-columns: repeat(3, 1fr);
    }

    @media (max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  .pagination-wrap {
    display: flex;
    justify-content: center;
    margin-top: 32px;
  }
}
</style>
