<template>
  <svg
    class="product-placeholder"
    :viewBox="`0 0 ${size} ${size}`"
    :width="size"
    :height="size"
    :style="{ borderRadius: rounded ? '8px' : '0' }"
    xmlns="http://www.w3.org/2000/svg"
  >
    <defs>
      <linearGradient :id="gradId" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" :style="`stop-color:${color1}`" />
        <stop offset="100%" :style="`stop-color:${color2}`" />
      </linearGradient>
    </defs>
    <rect width="100%" height="100%" :fill="`url(#${gradId})`" />
    <text
      x="50%"
      y="50%"
      dominant-baseline="central"
      text-anchor="middle"
      font-size="14"
      font-weight="600"
      fill="rgba(255,255,255,0.9)"
      font-family="system-ui, sans-serif"
    >
      {{ displayText }}
    </text>
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    seed?: string | number
    size?: number
    rounded?: boolean
  }>(),
  {
    size: 200,
    rounded: true,
  },
)

const PALETTES: [string, string][] = [
  ['#667eea', '#764ba2'],
  ['#f093fb', '#f5576c'],
  ['#4facfe', '#00f2fe'],
  ['#43e97b', '#38f9d7'],
  ['#fa709a', '#fee140'],
  ['#a18cd1', '#fbc2eb'],
  ['#fccb90', '#d57eeb'],
  ['#e0c3fc', '#8ec5fc'],
  ['#f5576c', '#ff6f3f'],
  ['#667eea', '#5b7fff'],
]

function hashSeed(s: string | number): number {
  const str = String(s)
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (Math.imul(31, h) + str.charCodeAt(i)) | 0
  }
  return Math.abs(h)
}

const idx = computed(() => hashSeed(props.seed) % PALETTES.length)

const color1 = computed(() => PALETTES[idx.value][0])
const color2 = computed(() => PALETTES[idx.value][1])

const gradId = computed(() => `pg-${idx.value}`)

const displayText = computed(() => {
  const str = String(props.seed || 'N/A')
  return str.length > 4 ? str.slice(0, 4) : str.toUpperCase()
})
</script>

<style lang="scss" scoped>
.product-placeholder {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
