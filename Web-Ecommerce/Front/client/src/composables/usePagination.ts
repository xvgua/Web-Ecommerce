import { ref, watch } from 'vue'
import type { Ref } from 'vue'
import type { PageQuery } from '@shared/types'

export function usePagination(fetchFn: (params: PageQuery) => Promise<void>, defaultPageSize = 20) {
  const page = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)
  const loading = ref(false)

  async function loadPage() {
    loading.value = true
    try {
      await fetchFn({ page: page.value, pageSize: pageSize.value })
    } finally {
      loading.value = false
    }
  }

  function handlePageChange(newPage: number) {
    page.value = newPage
    loadPage()
  }

  function handleSizeChange(newSize: number) {
    pageSize.value = newSize
    page.value = 1
    loadPage()
  }

  function setTotal(val: number) {
    total.value = val
  }

  function reset() {
    page.value = 1
    loadPage()
  }

  return {
    page,
    pageSize,
    total,
    loading,
    loadPage,
    handlePageChange,
    handleSizeChange,
    setTotal,
    reset,
  }
}
