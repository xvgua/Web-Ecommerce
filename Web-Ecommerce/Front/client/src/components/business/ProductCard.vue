<template>
  <div class="product-card" @click="goDetail">
    <div class="product-card__image">
      <ProductImage :src="product.mainImage" :seed="product.name + product.id" fit="cover" />
      <span v-if="isNew" class="product-card__badge">NEW</span>
    </div>
    <div class="product-card__info">
      <h3 class="product-card__name">{{ product.name }}</h3>
      <div class="product-card__price-row">
        <span class="product-card__price">{{ formatPrice(product.price) }}</span>
      </div>
      <div class="product-card__meta">
        <span>已售 {{ product.sales }}</span>
        <span v-if="product.reviewCount > 0" class="product-card__rating">
          ★ {{ product.avgRating }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatPrice } from '@/utils/format'
import type { Product } from '@shared/types/product'
import ProductImage from '@/components/common/ProductImage.vue'

const props = defineProps<{ product: Product }>()
const router = useRouter()

const isNew = computed(() => {
  const created = new Date(props.product.createTime).getTime()
  const now = Date.now()
  return now - created < 7 * 24 * 3600 * 1000
})

function goDetail() {
  router.push(`/products/${props.product.id}`)
}
</script>

<style lang="scss" scoped>
.product-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform .25s, box-shadow .25s;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 32px rgba(0,0,0,.12);

    .product-card__image :deep(.product-placeholder) {
      transform: scale(1.06);
    }
  }

  &__image {
    aspect-ratio: 1;
    overflow: hidden;
    position: relative;
    background: #f8f8f8;

    :deep(.product-image),
    :deep(.el-image) {
      width: 100%;
      height: 100%;
      transition: transform .4s;
    }
  }

  &__badge {
    position: absolute;
    top: 10px;
    left: 10px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
    font-size: 11px;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 4px;
    letter-spacing: .5px;
  }

  &__info {
    padding: 14px 16px;
  }

  &__name {
    font-size: 14px;
    font-weight: 500;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    margin-bottom: 10px;
    min-height: 39px;
    color: #333;
  }

  &__price-row {
    display: flex;
    align-items: baseline;
    gap: 8px;
  }

  &__price {
    font-size: 20px;
    font-weight: 700;
    color: #e6423a;
    font-family: 'SF Mono', 'Helvetica Neue', monospace;
  }

  &__meta {
    font-size: 12px;
    color: #999;
    margin-top: 6px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__rating {
    color: #f7ba2a;
    font-weight: 600;
  }
}
</style>
