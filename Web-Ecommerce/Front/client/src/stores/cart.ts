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

  async function fetchCart() {
    const res = await getCartList()
    items.value = res.data.map((item) => ({ ...item, checked: !!item.checked }))
  }

  async function addItem(productId: number, skuId: number, quantity: number) {
    await addToCart({ productId, skuId, quantity })
    await fetchCart()
  }

  async function updateItem(id: number, quantity: number, checked?: boolean) {
    await updateCartItem({ id, quantity, checked })
    await fetchCart()
  }

  async function removeItem(id: number) {
    await removeCartItem(id)
    await fetchCart()
  }

  function toggleCheckAll(checked: boolean) {
    items.value.forEach((item) => {
      item.checked = checked
    })
  }

  return { items, totalCount, checkedItems, checkedTotal, fetchCart, addItem, updateItem, removeItem, toggleCheckAll }
})
