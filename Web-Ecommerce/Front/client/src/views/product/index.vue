<template>
  <div class="product-list-page">
    <!-- Sort Bar -->
    <div class="filter-bar">
      <div class="filter-bar__sort">
        <span
          v-for="opt in sortOptions"
          :key="opt.value"
          :class="['sort-btn', { 'sort-btn--active': sort === opt.value }]"
          @click="handleSort(opt.value)"
        >
          {{ opt.label }}
        </span>
        <span
          :class="['sort-btn', { 'sort-btn--active': ratingActive }]"
          @click="handleRatingSort"
        >
          评价高低
          <span v-if="ratingActive" class="sort-arrow">{{ sort === 'rating_desc' ? '↑' : '↓' }}</span>
        </span>
      </div>
    </div>

    <!-- Product Grid -->
    <div class="product-grid" v-loading="loading">
      <product-card v-for="item in products" :key="item.id" :product="item" />
    </div>

    <el-empty v-if="!loading && !products.length" description="暂无商品">
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </el-empty>

    <!-- Pagination -->
    <div class="pagination-wrap" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 40, 60]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="loadProducts"
        @size-change="loadProducts"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProductList } from '@/api/product'
import type { Product, ProductQuery } from '@shared/types/product'
import ProductCard from '@/components/business/ProductCard.vue'

const route = useRoute()

const products = ref<Product[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const sort = ref('')

const sortOptions = [
  { label: '默认', value: '' },
  { label: '销量优先', value: 'sales_desc' },
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' },
  { label: '最新上架', value: 'newest' },
]

async function loadProducts() {
  loading.value = true
  try {
    const kw = (route.query.keyword as string) || ''
    const catId = route.query.categoryId ? Number(route.query.categoryId) : undefined
    const res = await getProductList({
      page: page.value,
      pageSize: pageSize.value,
      keyword: kw.trim() || undefined,
      categoryId: catId,
      sort: (sort.value || undefined) as ProductQuery['sort'],
    })
    products.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const ratingActive = computed(() => sort.value === 'rating_desc' || sort.value === 'rating_asc')

function handleSort(val: string) {
  sort.value = val
  page.value = 1
  loadProducts()
}

function handleRatingSort() {
  if (sort.value === 'rating_desc') {
    sort.value = 'rating_asc'
  } else {
    sort.value = 'rating_desc'
  }
  page.value = 1
  loadProducts()
}

watch(() => route.query.keyword, () => {
  page.value = 1
  loadProducts()
})

watch(() => route.query.categoryId, () => {
  page.value = 1
  loadProducts()
})

onMounted(() => { loadProducts() })
</script>

<style lang="scss" scoped>
.product-list-page {
  max-width: 1200px;
  margin: 0 auto;
}

.filter-bar {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);

  &__sort {
    display: flex;
    gap: 4px;
  }
}

.sort-btn {
  padding: 6px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all .15s;

  &:hover {
    color: #409eff;
    background: rgba(64,158,255,.06);
  }

  &--active {
    color: #fff;
    background: #409eff;
    font-weight: 500;

    &:hover {
      color: #fff;
      background: #409eff;
    }

    .sort-arrow {
      color: #fff;
    }
  }
}

.sort-arrow {
  display: inline-block;
  margin-left: 2px;
  font-size: 12px;
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
    gap: 10px;
  }
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 36px;
  padding-bottom: 24px;
}
</style>
