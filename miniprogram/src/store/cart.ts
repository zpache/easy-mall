import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { cartApi } from '@/api/cart'
import type { CartItem } from '@/types/api'

/**
 * 购物车状态：列表、角标数量、勾选合计。
 */
export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const count = ref(0)

  const checkedItems = computed(() => items.value.filter((item) => item.checked === 1 && !item.invalid))

  const checkedAmount = computed(() =>
    checkedItems.value.reduce((sum, item) => sum + Number(item.subtotal), 0)
  )

  const allChecked = computed(
    () => items.value.length > 0 && items.value.every((item) => item.checked === 1)
  )

  async function refresh() {
    items.value = await cartApi.list()
    count.value = items.value.reduce((sum, item) => sum + item.quantity, 0)
  }

  async function refreshCount() {
    count.value = await cartApi.count()
  }

  async function add(goodsId: number, quantity = 1) {
    await cartApi.add(goodsId, quantity)
    await refreshCount()
  }

  async function updateQuantity(id: number, quantity: number) {
    await cartApi.updateQuantity(id, quantity)
    await refresh()
  }

  async function toggleChecked(id: number, checked: boolean) {
    await cartApi.updateChecked(checked, id)
    await refresh()
  }

  async function toggleAll(checked: boolean) {
    await cartApi.updateChecked(checked)
    await refresh()
  }

  async function remove(id: number) {
    await cartApi.remove(id)
    await refresh()
  }

  return {
    items,
    count,
    checkedItems,
    checkedAmount,
    allChecked,
    refresh,
    refreshCount,
    add,
    updateQuantity,
    toggleChecked,
    toggleAll,
    remove
  }
})
