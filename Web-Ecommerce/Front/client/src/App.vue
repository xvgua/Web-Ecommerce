<template>
  <router-view />
</template>

<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
watch(
  () => router.currentRoute.value.meta.title,
  (title) => {
    if (title) {
      useTitle(title as string)
    }
  },
)
</script>
