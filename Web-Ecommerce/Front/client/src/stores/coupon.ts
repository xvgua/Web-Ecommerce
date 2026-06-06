import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useCouponStore = defineStore('coupon', () => {
  const version = ref(0)

  function notifyClaimed() {
    version.value++
  }

  return { version, notifyClaimed }
})
