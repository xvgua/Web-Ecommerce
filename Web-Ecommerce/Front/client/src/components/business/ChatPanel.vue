<template>
  <Teleport to="body">
    <Transition name="chat-slide">
      <div v-if="isOpen" class="chat-panel">
        <div class="chat-panel__header">
          <div class="chat-panel__header-info">
            <span class="chat-panel__status-dot" :class="{ 'is-online': csOnline }" />
            <span class="chat-panel__title">客服小二</span>
            <span class="chat-panel__status-text">{{ csOnline ? '在线' : '离线' }}</span>
          </div>
          <div class="chat-panel__header-actions">
            <button class="chat-panel__close-btn" @click="closeChat">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>

        <!-- Conversation list (when no active conversation) -->
        <div v-if="!conversationId && !loading" class="chat-panel__conv-list">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="chat-panel__conv-item"
            :class="{ 'is-closed': conv.status === 2 }"
            @click="conv.status === 1 && switchConversation(conv.id)"
          >
            <div class="chat-panel__conv-subject">{{ conv.sourceName || '在线咨询' }}</div>
            <div class="chat-panel__conv-last">{{ conv.lastMessage || '暂无消息' }}</div>
            <span v-if="conv.status === 2" class="chat-panel__conv-closed">已关闭</span>
          </div>
          <el-empty v-if="!conversations.length" description="暂无会话" :image-size="60" />
        </div>

        <!-- Messages area -->
        <div v-else class="chat-panel__body" ref="msgBody">
          <div v-if="loading" class="chat-panel__loading">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['chat-panel__msg', msg.senderType === 1 ? 'chat-panel__msg--user' : 'chat-panel__msg--cs']"
          >
            <div class="chat-panel__msg-bubble">
              {{ msg.content }}
            </div>
            <div class="chat-panel__msg-time">{{ formatTime(msg.createTime) }}</div>
          </div>
          <div v-if="!loading && !messages.length" class="chat-panel__empty-msg">
            开始您的咨询吧~
          </div>
        </div>

        <!-- Input area -->
        <div class="chat-panel__footer" v-if="conversationId">
          <div v-if="!csOnline" class="chat-panel__offline-hint">
            客服暂时不在线，您的消息将以留言方式发送
          </div>
          <div class="chat-panel__input-row">
            <el-input
              v-model="inputText"
              :placeholder="csOnline ? '输入消息...' : '客服不在线，请留言...'"
              maxlength="500"
              show-word-limit
              @keyup.enter="handleSend"
            />
            <el-button type="primary" :loading="sending" @click="handleSend">发送</el-button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Close, Loading } from '@element-plus/icons-vue'
import { useChat } from '@/composables/useChat'

const {
  isOpen, conversationId, conversations, messages,
  csOnline, inputText, sending, loading,
  closeChat, handleSend, switchConversation
} = useChat()

const msgBody = ref<HTMLElement | null>(null)

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

watch(messages, () => {
  nextTick(() => {
    if (msgBody.value) {
      msgBody.value.scrollTop = msgBody.value.scrollHeight
    }
  })
}, { deep: true })
</script>

<style lang="scss" scoped>
.chat-panel {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 380px;
  height: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 40px rgba(0,0,0,.15);
  display: flex;
  flex-direction: column;
  z-index: 9999;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    background: linear-gradient(135deg, #409eff, #337ecc);
    color: #fff;
    flex-shrink: 0;
  }

  &__header-info {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #bbb;

    &.is-online { background: #67c23a; }
  }

  &__title { font-size: 15px; font-weight: 600; }
  &__status-text { font-size: 12px; opacity: .8; }

  &__close-btn {
    background: none;
    border: none;
    color: #fff;
    cursor: pointer;
    font-size: 18px;
    padding: 4px;
    display: flex;
    align-items: center;
    opacity: .8;

    &:hover { opacity: 1; }
  }

  &__conv-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
  }

  &__conv-item {
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    border-bottom: 1px solid #f0f0f0;
    transition: background .15s;
    position: relative;

    &:hover { background: #f5f7fa; }

    &.is-closed {
      opacity: .5;
      cursor: default;
    }
  }

  &__conv-subject {
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 4px;
  }

  &__conv-last {
    font-size: 12px;
    color: #999;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__conv-closed {
    position: absolute;
    right: 12px;
    top: 50%;
    transform: translateY(-50%);
    font-size: 11px;
    color: #999;
    background: #f0f0f0;
    padding: 2px 6px;
    border-radius: 4px;
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background: #f5f7fa;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 20px;
    color: #999;
  }

  &__msg {
    display: flex;
    flex-direction: column;
    max-width: 80%;

    &--user {
      align-self: flex-end;
      align-items: flex-end;

      .chat-panel__msg-bubble {
        background: #409eff;
        color: #fff;
        border-radius: 16px 4px 16px 16px;
      }
    }

    &--cs {
      align-self: flex-start;
      align-items: flex-start;

      .chat-panel__msg-bubble {
        background: #fff;
        color: #333;
        border-radius: 4px 16px 16px 16px;
      }
    }
  }

  &__msg-bubble {
    padding: 10px 14px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
  }

  &__msg-time {
    font-size: 11px;
    color: #bbb;
    margin-top: 4px;
    padding: 0 4px;
  }

  &__empty-msg {
    text-align: center;
    color: #bbb;
    padding: 40px 0;
  }

  &__footer {
    flex-shrink: 0;
    border-top: 1px solid #ebeef5;
    padding: 10px 12px;
    background: #fff;
  }

  &__offline-hint {
    text-align: center;
    font-size: 12px;
    color: #e6a23c;
    margin-bottom: 8px;
  }

  &__input-row {
    display: flex;
    gap: 8px;
  }
}

.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: all .3s ease;
}

.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
