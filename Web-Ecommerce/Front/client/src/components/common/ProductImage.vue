<template>
  <div class="product-image">
    <el-image
      v-if="src"
      :src="src"
      :fit="fit"
      :loading="lazy ? 'lazy' : undefined"
      class="product-image__el"
    >
      <template #error>
        <slot name="placeholder">
          <ProductPlaceholder :seed="seed" :size="placeholderSize" />
        </slot>
      </template>
      <template #placeholder>
        <div class="product-image__skeleton" />
      </template>
    </el-image>
    <ProductPlaceholder v-else :seed="seed" :size="placeholderSize" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ProductPlaceholder from './ProductPlaceholder.vue'

const props = withDefaults(
  defineProps<{
    src?: string
    seed?: string | number
    fit?: 'cover' | 'contain' | 'fill' | 'none' | 'scale-down'
    lazy?: boolean
  }>(),
  {
    fit: 'cover',
    lazy: true,
  },
)

const placeholderSize = computed(() => (props.fit === 'cover' ? 200 : 120))
</script>

<style lang="scss" scoped>
.product-image {
  width: 100%;
  height: 100%;

  &__el {
    width: 100%;
    height: 100%;
  }

  &__skeleton {
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
