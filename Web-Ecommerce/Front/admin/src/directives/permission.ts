import type { Directive, DirectiveBinding } from 'vue'
import { useAdminStore } from '@/stores/admin'

/**
 * v-permission 权限指令（管理端）
 */
export const vPermission: Directive<HTMLElement, string> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
    const adminStore = useAdminStore()
    const allowed = binding.value?.split(',').map(s => s.trim()) || []

    if (allowed.length > 0 && !allowed.includes(adminStore.role)) {
      el.style.display = 'none'
    }
  },
}
