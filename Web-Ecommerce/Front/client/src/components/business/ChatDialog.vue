<template>
  <el-dialog
    v-model="store.isOpen"
    title=""
    :width="440"
    :close-on-click-modal="false"
    destroy-on-close
    :fullscreen="isMobile"
    class="chat-dialog"
    @closed="store.closeChat"
  >
    <template #header>
      <div class="chat-dialog__header">
        <div class="chat-dialog__header-info">
          <span class="chat-dialog__status-dot" :class="{ 'is-online': store.csOnline }" />
          <span class="chat-dialog__title">客服小二</span>
          <span class="chat-dialog__status-text">{{ store.csOnline ? '在线' : '离线' }}</span>
        </div>
      </div>
    </template>

    <div class="chat-dialog__body">
      <!-- Conversation list -->
      <div v-if="!store.conversationId && !store.loading" class="chat-dialog__conv-list">
        <div
          v-for="conv in store.conversations"
          :key="conv.id"
          class="chat-dialog__conv-item"
          :class="{ 'is-closed': conv.status === 2 }"
          @click="conv.status === 1 && store.switchConversation(conv.id)"
        >
          <div class="chat-dialog__conv-subject">{{ conv.sourceName || '在线咨询' }}</div>
          <div class="chat-dialog__conv-last">{{ conv.lastMessage || '暂无消息' }}</div>
          <span v-if="conv.status === 2" class="chat-dialog__conv-closed">已关闭</span>
        </div>
        <el-empty v-if="!store.conversations.length" description="暂无会话" :image-size="60" />
      </div>

      <!-- Messages area -->
      <div v-else class="chat-dialog__messages" ref="msgBodyRef">
        <div v-if="store.loading" class="chat-dialog__loading">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>

        <!-- Product context banner -->
        <div v-if="store.productContext && !store.loading" class="chat-dialog__product-context">
          <div class="chat-dialog__product-context-img">
            <el-image :src="store.productContext.productImage" fit="cover" style="width: 48px; height: 48px; border-radius: 6px">
              <template #error><div class="img-placeholder" /></template>
            </el-image>
          </div>
          <div class="chat-dialog__product-context-info">
            <div class="chat-dialog__product-context-name">{{ store.productContext.productName }}</div>
            <div class="chat-dialog__product-context-price">&yen;{{ store.productContext.price }}</div>
          </div>
        </div>

        <div
          v-for="msg in store.messages"
          :key="msg.id"
          :class="['chat-dialog__msg', msg.senderType === 1 ? 'chat-dialog__msg--user' : 'chat-dialog__msg--cs']"
        >
          <!-- Product card message -->
          <div v-if="msg.contentType === 2 && msg.extraData" class="chat-dialog__product-card" @click="onProductCardClick(msg.extraData)">
            <div class="chat-dialog__product-card-img">
              <el-image :src="parseCardData(msg.extraData).productImage" fit="cover" style="width: 80px; height: 80px; border-radius: 6px">
                <template #error><div class="img-placeholder" /></template>
              </el-image>
            </div>
            <div class="chat-dialog__product-card-info">
              <div class="chat-dialog__product-card-name">{{ parseCardData(msg.extraData).productName }}</div>
              <div class="chat-dialog__product-card-price">&yen;{{ parseCardData(msg.extraData).price }}</div>
              <div class="chat-dialog__product-card-link">查看详情 &gt;</div>
            </div>
          </div>
          <!-- Text message -->
          <div v-else class="chat-dialog__msg-bubble">
            {{ msg.content }}
          </div>
          <div class="chat-dialog__msg-time">{{ formatTime(msg.createTime) }}</div>
        </div>
        <div v-if="!store.loading && !store.messages.length" class="chat-dialog__empty-msg">
          开始您的咨询吧~
        </div>
      </div>
    </div>

    <template #footer>
      <div class="chat-dialog__footer" v-if="store.conversationId">
        <div v-if="!store.csOnline" class="chat-dialog__offline-hint">
          客服暂时不在线，您的消息将以留言方式发送
        </div>
        <div class="chat-dialog__input-row">
          <el-button
            v-if="store.productContext"
            size="small"
            :icon="Goods"
            :loading="store.sending"
            class="chat-dialog__send-product-btn"
            @click="store.sendProductCard()"
          >
            发送商品
          </el-button>
          <el-input
            v-model="store.inputText"
            :placeholder="store.csOnline ? '输入消息...' : '客服不在线，请留言...'"
            maxlength="500"
            show-word-limit
            @keyup.enter="store.handleSend()"
          />
          <el-button type="primary" :loading="store.sending" @click="store.handleSend()">发送</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Loading, Goods } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'

const store = useChatStore()
const router = useRouter()
const msgBodyRef = ref<HTMLElement | null>(null)

const isMobile = computed(() => window.innerWidth < 768)

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

function parseCardData(extraData: string) {
  try {
    return JSON.parse(extraData)
  } catch {
    return { productName: '', productImage: '', price: 0 }
  }
}

function onProductCardClick(extraData: string) {
  const data = parseCardData(extraData)
  if (data.productId) {
    router.push(`/products/${data.productId}`)
  }
}

watch(() => store.messages, () => {
  nextTick(() => {
    if (msgBodyRef.value) {
      msgBodyRef.value.scrollTop = msgBodyRef.value.scrollHeight
    }
  })
}, { deep: true })
</script>

<style scoped>
.chat-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin: 0;
}

.chat-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 400px;
  display: flex;
  flex-direction: column;
}

.chat-dialog :deep(.el-dialog__footer) {
  padding: 0;
}

.chat-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  border-radius: var(--el-dialog-border-radius, 8px) var(--el-dialog-border-radius, 8px) 0 0;
}

.chat-dialog__header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-dialog__status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #bbb;
}

.chat-dialog__status-dot.is-online {
  background: #67c23a;
}

.chat-dialog__title {
  font-size: 15px;
  font-weight: 600;
}

.chat-dialog__status-text {
  font-size: 12px;
  opacity: .8;
}

.chat-dialog__body {
  flex: 1;
  overflow: hidden;
}

/* Conversation list */
.chat-dialog__conv-list {
  padding: 8px;
  height: 100%;
  overflow-y: auto;
}

.chat-dialog__conv-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.chat-dialog__conv-item:hover {
  background: #f5f7fa;
}

.chat-dialog__conv-item.is-closed {
  opacity: .5;
  cursor: default;
}

.chat-dialog__conv-subject {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.chat-dialog__conv-last {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-dialog__conv-closed {
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

/* Messages area */
.chat-dialog__messages {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-dialog__loading {
  display: flex;
  justify-content: center;
  padding: 20px;
  color: #999;
}

.chat-dialog__product-context {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  margin-bottom: 4px;
}

.chat-dialog__product-context-img {
  flex-shrink: 0;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  background: #f0f0f0;
}

.chat-dialog__product-context-info {
  min-width: 0;
}

.chat-dialog__product-context-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-dialog__product-context-price {
  font-size: 14px;
  color: #e6423a;
  font-weight: 600;
}

/* Product card in message */
.chat-dialog__product-card {
  display: flex;
  gap: 10px;
  padding: 10px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: box-shadow .15s;
}

.chat-dialog__product-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, .08);
}

.chat-dialog__product-card-img {
  flex-shrink: 0;
}

.chat-dialog__product-card-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.chat-dialog__product-card-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.chat-dialog__product-card-price {
  font-size: 15px;
  color: #e6423a;
  font-weight: 600;
  margin-bottom: 2px;
}

.chat-dialog__product-card-link {
  font-size: 11px;
  color: #409eff;
}

/* Message bubbles */
.chat-dialog__msg {
  display: flex;
  flex-direction: column;
  max-width: 80%;
}

.chat-dialog__msg--user {
  align-self: flex-end;
  align-items: flex-end;
}

.chat-dialog__msg--user .chat-dialog__msg-bubble {
  background: #409eff;
  color: #fff;
  border-radius: 16px 4px 16px 16px;
}

.chat-dialog__msg--cs {
  align-self: flex-start;
  align-items: flex-start;
}

.chat-dialog__msg--cs .chat-dialog__msg-bubble {
  background: #fff;
  color: #333;
  border-radius: 4px 16px 16px 16px;
}

.chat-dialog__msg-bubble {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-dialog__msg-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 4px;
  padding: 0 4px;
}

.chat-dialog__empty-msg {
  text-align: center;
  color: #bbb;
  padding: 40px 0;
}

/* Footer */
.chat-dialog__footer {
  padding: 0;
}

.chat-dialog__offline-hint {
  text-align: center;
  font-size: 12px;
  color: #e6a23c;
  margin-bottom: 8px;
  padding: 0 4px;
}

.chat-dialog__input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-dialog__send-product-btn {
  flex-shrink: 0;
}
</style>
