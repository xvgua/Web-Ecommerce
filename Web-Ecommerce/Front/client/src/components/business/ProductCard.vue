<template>
  <div class="product-card" @click="goDetail">
    <div class="product-card__image">
      <el-image :src="product.mainImage" fit="cover" loading="lazy">
        <template #error>
          <div class="product-card__image-placeholder">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
      </el-image>
    </div>
    <div class="product-card__info">
      <h3 class="product-card__name">{{ product.name }}</h3>
      <div class="product-card__price">{{ formatPrice(product.price) }}</div>
      <div class="product-card__sales">已售 {{ product.sales }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Picture } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { formatPrice } from '@/utils/format'
import type { Product } from '@shared/types/product'

const props = defineProps<{ product: Product }>()
const router = useRouter()

function goDetail() {
  router.push(`/products/${props.product.id}`)
}
</script>

<style lang="scss" scoped>
.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  &__image {
    aspect-ratio: 1;
    overflow: hidden;

    .el-image {
      width: 100%;
      height: 100%;
    }
  }

  &__image-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
    color: #ccc;
    font-size: 40px;
  }

  &__info {
    padding: 12px;
  }

  &__name {
    font-size: 14px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 8px;
  }

  &__price {
    font-size: 18px;
    font-weight: 700;
    color: #e6423a;
  }

  &__sales {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}
</style>
