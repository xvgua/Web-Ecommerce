import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CartItem } from '@shared/types/cart'
import { getCartList, addToCart, updateCartItem, removeCartItem } from '@/api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])

  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))

  const checkedItems = computed(() => items.value.filter((item) => item.checked))

  const checkedTotal = computed(() =>
    checkedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0),
  )

  const checkedIds = computed(() => checkedItems.value.map((item) => item.id))

  async function fetchCart(skipErrorToast = false) {
    const res = await getCartList(skipErrorToast)
    items.value = res.data.map((item) => ({ ...item, checked: !!item.checked }))
  }

  async function addItem(productId: number, skuId: number, quantity: number) {
    await addToCart({ productId, skuId, quantity })
    await fetchCart()
  }

  async function updateItem(id: number, quantity: number, checked?: boolean, skuId?: number) {
    await updateCartItem({ id, quantity, checked, skuId })
    await fetchCart()
  }

  async function removeItem(id: number) {
    await removeCartItem(id)
    await fetchCart()
  }

  async function removeSelected() {
    for (const id of checkedIds.value) {
      await removeCartItem(id)
    }
    await fetchCart()
  }

  async function toggleCheckAll(checked: boolean) {
    for (const item of items.value) {
      await updateCartItem({ id: item.id, quantity: item.quantity, checked, skuId: item.skuId ?? undefined })
    }
    await fetchCart()
  }

  async function toggleCheckInverse() {
    for (const item of items.value) {
      await updateCartItem({ id: item.id, quantity: item.quantity, checked: !item.checked, skuId: item.skuId ?? undefined })
    }
    await fetchCart()
  }

  return {
    items, totalCount, checkedItems, checkedTotal, checkedIds,
    fetchCart, addItem, updateItem, removeItem, removeSelected,
    toggleCheckAll, toggleCheckInverse,
  }
})
