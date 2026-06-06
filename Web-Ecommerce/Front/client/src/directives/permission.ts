import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * v-permission 权限指令
 * 基于 Pinia 用户角色控制元素显隐
 *
 * 用法：
 *   <el-button v-permission="'ADMIN'">管理员操作</el-button>
 *   <el-tab-pane v-permission="'ADMIN,SUPER_ADMIN'">多角色</el-tab-pane>
 */
export const vPermission: Directive<HTMLElement, string> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
    const userStore = useUserStore()
    const allowed = binding.value?.split(',').map(s => s.trim()) || []
    const role = userStore.user?.role || 'USER'

    if (allowed.length > 0 && !allowed.includes(role)) {
      el.style.display = 'none'
    }
  },
}
