<template>
  <div class="chat-float" @click="handleClick">
    <div class="chat-float__icon">
      <el-icon :size="24"><ChatDotSquare /></el-icon>
    </div>
    <span class="chat-float__label">客服</span>
    <span v-if="csOnline" class="chat-float__badge">在线</span>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ChatDotSquare } from '@element-plus/icons-vue'
import { useChat } from '@/composables/useChat'
import { getCsStatus } from '@/api/chat'

const { openChat, isOpen, csOnline } = useChat()
let statusTimer: ReturnType<typeof setInterval> | null = null

function handleClick() {
  if (isOpen.value) return
  openChat()
}

onMounted(() => {
  getCsStatus().then(res => { csOnline.value = res.data.online }).catch(() => {})
  statusTimer = setInterval(async () => {
    try {
      const res = await getCsStatus()
      csOnline.value = res.data.online
    } catch { /* ignore */ }
  }, 30000)
})

onUnmounted(() => {
  if (statusTimer) clearInterval(statusTimer)
})
</script>

<style lang="scss" scoped>
.chat-float {
  position: fixed;
  right: 20px;
  bottom: 140px;
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  z-index: 9998;
  box-shadow: 0 4px 16px rgba(64,158,255,.4);
  transition: transform .2s, box-shadow .2s;

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 6px 24px rgba(64,158,255,.55);
  }

  &__label {
    font-size: 10px;
    margin-top: 2px;
  }

  &__badge {
    position: absolute;
    top: -2px;
    right: -2px;
    background: #67c23a;
    color: #fff;
    font-size: 10px;
    padding: 1px 5px;
    border-radius: 8px;
    line-height: 1.4;
  }
}

@media (max-width: 768px) {
  .chat-float {
    right: 12px;
    bottom: 100px;
    width: 48px;
    height: 48px;
  }
}
</style>
