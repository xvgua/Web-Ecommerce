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
  const listed = props.product.listedAt || props.product.createTime
  const listedTs = new Date(listed).getTime()
  const now = Date.now()
  return now - listedTs < 7 * 24 * 3600 * 1000
})

function goDetail() {
  router.push(`/products/${props.product.id}`)
}
</script>

<style lang="scss" scoped>
.product-card {
  background: var(--bg1);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  border: none;
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);

    .product-card__image :deep(.product-placeholder),
    .product-card__image :deep(.el-image img) {
      transform: scale(1.05);
    }
  }

  &__image {
    aspect-ratio: 4 / 5;
    overflow: hidden;
    position: relative;
    background: var(--bg2);

    :deep(.product-image),
    :deep(.el-image),
    :deep(.el-image img) {
      width: 100%;
      height: 100%;
      transition: transform 0.5s var(--transition-slow);
    }
  }

  &__badge {
    position: absolute;
    top: 10px;
    left: 10px;
    background: var(--brand-primary);
    color: #fff;
    font-size: 10px;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 4px;
    letter-spacing: 0.8px;
    text-transform: uppercase;
  }

  &__info {
    padding: 14px 16px 16px;
  }

  &__name {
    font-size: 14px;
    font-weight: 500;
    line-height: 1.45;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    margin-bottom: 6px;
    min-height: 20px;
    color: var(--text1);
  }

  &__price-row {
    display: flex;
    align-items: baseline;
    gap: 6px;
  }

  &__price {
    font-size: 20px;
    font-weight: 700;
    color: var(--brand-primary);
    font-family: 'Outfit', 'SF Mono', 'Helvetica Neue', monospace;
    font-variant-numeric: tabular-nums;
    letter-spacing: -0.5px;
  }

  &__meta {
    font-size: 12px;
    color: var(--text3);
    margin-top: 6px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__rating {
    font-size: 11px;
    color: var(--color-warning);
    font-weight: 600;
    display: flex;
    align-items: center;
    gap: 1px;
  }
}

@media (max-width: 768px) {
  .product-card {
    &__info {
      padding: 10px 12px 12px;
    }

    &__name {
      font-size: 13px;
      -webkit-line-clamp: 2;
    }

    &__price {
      font-size: 17px;
    }

    &__meta {
      font-size: 11px;
    }
  }
}
</style>
