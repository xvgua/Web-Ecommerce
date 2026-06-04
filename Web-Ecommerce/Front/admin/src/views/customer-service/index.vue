<template>
  <div class="cs-page">
    <h1 class="page-title">客服管理</h1>

    <div class="cs-layout">
      <!-- Left: conversation list -->
      <div class="cs-left">
        <div class="cs-left__toolbar">
          <el-radio-group v-model="statusFilter" size="small" @change="loadConversations">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="1">进行中</el-radio-button>
            <el-radio-button :value="2">已关闭</el-radio-button>
          </el-radio-group>
        </div>

        <div class="cs-left__list" v-loading="convLoading">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            :class="['cs-conv', { 'is-active': activeConvId === conv.id }]"
            @click="selectConversation(conv)"
          >
            <div class="cs-conv__header">
              <span class="cs-conv__user">{{ conv.username || '匿名用户' }}</span>
              <span v-if="conv.unreadCount" class="cs-conv__badge">{{ conv.unreadCount }}</span>
              <span v-if="conv.status === 2" class="cs-conv__closed-tag">已关闭</span>
            </div>
            <div class="cs-conv__source">{{ conv.sourceName || '在线咨询' }}</div>
            <div class="cs-conv__last">{{ conv.lastMessage || '暂无消息' }}</div>
            <div class="cs-conv__time">{{ formatDate(conv.lastActive) }}</div>
          </div>
          <el-empty v-if="!convLoading && !conversations.length" description="暂无会话" :image-size="60" />
        </div>

        <div class="cs-left__pager" v-if="convTotal > 0">
          <el-pagination
            v-model:current-page="convPage"
            v-model:page-size="convPageSize"
            :total="convTotal"
            :page-sizes="[10, 20]"
            layout="total, prev, pager, next"
            small
            background
            @current-change="loadConversations"
          />
        </div>
      </div>

      <!-- Right: chat area -->
      <div class="cs-right">
        <template v-if="activeConvId">
          <div class="cs-right__header">
            <div class="cs-right__header-info">
              <span class="cs-right__user">{{ activeConv?.username || '匿名用户' }}</span>
              <span class="cs-right__source">来源: {{ activeConv?.sourceName || '在线咨询' }}</span>
            </div>
            <el-button
              v-if="activeConv?.status === 1"
              size="small"
              type="warning"
              @click="handleCloseConv"
            >
              关闭会话
            </el-button>
          </div>

          <div class="cs-right__body" ref="msgBody" v-loading="msgLoading">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['cs-msg', msg.senderType === 1 ? 'cs-msg--user' : 'cs-msg--cs']"
            >
              <div v-if="msg.contentType === 2 && msg.extraData" class="cs-msg__product-card">
                <div class="cs-msg__pc-name">{{ getCardName(msg.extraData) }}</div>
                <div class="cs-msg__pc-price">&yen;{{ getCardPrice(msg.extraData) }}</div>
              </div>
              <div v-else class="cs-msg__bubble">{{ msg.content }}</div>
              <div class="cs-msg__time">{{ formatTime(msg.createTime) }}</div>
            </div>
            <el-empty v-if="!msgLoading && !messages.length" description="暂无消息" :image-size="40" />
          </div>

          <div class="cs-right__quick" v-if="activeConv?.status === 1">
            <el-dropdown @command="insertQuickReply" placement="top-start">
              <el-button size="small">快捷回复</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="qr in quickReplies"
                    :key="qr.id"
                    :command="qr.content"
                  >
                    {{ qr.title }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <div class="cs-right__footer" v-if="activeConv?.status === 1">
            <el-input
              v-model="replyText"
              placeholder="输入回复..."
              maxlength="500"
              show-word-limit
              @keyup.enter="handleReply"
            />
            <el-button type="primary" :loading="replying" @click="handleReply">发送</el-button>
          </div>
        </template>
        <el-empty v-else description="请选择一个会话" :image-size="80" class="cs-right__empty" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getConversations, getConversationMessages, replyConversation,
  adminCloseConversation, getQuickReplies
} from '@/api/chat'
import { formatDate } from '@/utils/format'
import type { Conversation, ChatMessage, QuickReply } from '@shared/types/chat'

const conversations = ref<Conversation[]>([])
const convPage = ref(1)
const convPageSize = ref(20)
const convTotal = ref(0)
const convLoading = ref(false)
const statusFilter = ref<number | undefined>(1)

const activeConvId = ref<number | null>(null)
const activeConv = ref<Conversation | null>(null)
const messages = ref<ChatMessage[]>([])
const msgLoading = ref(false)
const replyText = ref('')
const replying = ref(false)
const quickReplies = ref<QuickReply[]>([])
const msgBody = ref<HTMLElement | null>(null)

let pollTimer: ReturnType<typeof setInterval> | null = null

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

function parseCardData(extraData: string) {
  try { return JSON.parse(extraData) } catch { return {} }
}

function getCardName(extraData: string) {
  return parseCardData(extraData).productName || ''
}

function getCardPrice(extraData: string) {
  return parseCardData(extraData).price || 0
}

async function loadConversations(silent = false) {
  if (!silent) convLoading.value = true
  try {
    const res = await getConversations({
      page: convPage.value,
      pageSize: convPageSize.value,
      status: statusFilter.value,
    })
    conversations.value = res.data.records
    convTotal.value = res.data.total
  } finally {
    if (!silent) convLoading.value = false
  }
}

async function selectConversation(conv: Conversation) {
  stopPolling()
  activeConvId.value = conv.id
  activeConv.value = conv
  msgLoading.value = true
  try {
    const res = await getConversationMessages(conv.id)
    messages.value = res.data
    // Reload conversations to update unread counts
    loadConversations(true)
    startPolling()
  } finally {
    msgLoading.value = false
  }
}

async function handleReply() {
  const text = replyText.value.trim()
  if (!text || !activeConvId.value) return
  replying.value = true
  try {
    const res = await replyConversation(activeConvId.value, text)
    messages.value.push(res.data)
    replyText.value = ''
    await nextTick()
    scrollToBottom()
  } catch { /* handled by interceptor */ }
  finally { replying.value = false }
}

async function handleCloseConv() {
  if (!activeConvId.value) return
  await ElMessageBox.confirm('确定要关闭该会话吗？', '提示', { type: 'warning' })
  await adminCloseConversation(activeConvId.value)
  if (activeConv.value) activeConv.value.status = 2
  ElMessage.success('会话已关闭')
  loadConversations()
}

function insertQuickReply(content: string) {
  replyText.value = content
}

function scrollToBottom() {
  if (msgBody.value) {
    msgBody.value.scrollTop = msgBody.value.scrollHeight
  }
}

watch(messages, () => {
  nextTick(() => scrollToBottom())
}, { deep: true })

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!activeConvId.value) return
    try {
      const res = await getConversationMessages(activeConvId.value)
      messages.value = res.data
    } catch { /* ignore */ }
    loadConversations(true)
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(async () => {
  await loadConversations()
  try {
    const res = await getQuickReplies()
    quickReplies.value = res.data
  } catch { /* ignore */ }
})

onUnmounted(() => stopPolling())
</script>

<style lang="scss" scoped>
.cs-page { height: calc(100vh - 80px); display: flex; flex-direction: column; }
.page-title { font-size: 20px; font-weight: 600; margin: 0 0 16px; }

.cs-layout {
  flex: 1;
  display: flex;
  gap: 0;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
}

.cs-left {
  width: 340px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;

  &__toolbar { padding: 12px; border-bottom: 1px solid #ebeef5; }
  &__list { flex: 1; overflow-y: auto; }
  &__pager { padding: 10px 12px; border-top: 1px solid #ebeef5; display: flex; justify-content: center; }
}

.cs-conv {
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background .15s;

  &:hover { background: #f5f7fa; }
  &.is-active { background: #ecf5ff; border-left: 3px solid #409eff; padding-left: 13px; }

  &__header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
  &__user { font-size: 14px; font-weight: 500; }
  &__badge {
    background: #f56c6c;
    color: #fff;
    font-size: 10px;
    padding: 1px 6px;
    border-radius: 10px;
    line-height: 1.5;
  }
  &__closed-tag { font-size: 11px; color: #999; background: #f0f0f0; padding: 1px 6px; border-radius: 4px; }
  &__source { font-size: 12px; color: #909399; margin-bottom: 4px; }
  &__last {
    font-size: 13px; color: #606266;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
  &__time { font-size: 11px; color: #c0c4cc; margin-top: 4px; }
}

.cs-right {
  flex: 1;
  display: flex;
  flex-direction: column;

  &__empty { margin: auto; }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 20px;
    border-bottom: 1px solid #ebeef5;
    background: #fafafa;
  }

  &__header-info { display: flex; flex-direction: column; gap: 2px; }
  &__user { font-size: 15px; font-weight: 600; }
  &__source { font-size: 12px; color: #909399; }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 16px 20px;
    background: #f5f7fa;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__quick { padding: 8px 20px; border-top: 1px solid #ebeef5; background: #fafafa; }

  &__footer {
    display: flex;
    gap: 10px;
    padding: 12px 20px;
    border-top: 1px solid #ebeef5;
    background: #fff;
  }
}

.cs-msg {
  display: flex;
  flex-direction: column;
  max-width: 75%;

  &--user {
    align-self: flex-end;
    align-items: flex-end;

    .cs-msg__bubble {
      background: #409eff;
      color: #fff;
      border-radius: 16px 4px 16px 16px;
    }
  }

  &--cs {
    align-self: flex-start;
    align-items: flex-start;

    .cs-msg__bubble {
      background: #fff;
      color: #333;
      border-radius: 4px 16px 16px 16px;
    }
  }

  &__bubble {
    padding: 10px 14px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
  }

  &__time { font-size: 11px; color: #bbb; margin-top: 4px; padding: 0 4px; }

  &__product-card {
    padding: 8px 12px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    min-width: 160px;
  }

  &__pc-name {
    font-size: 13px;
    color: #333;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 4px;
  }

  &__pc-price {
    font-size: 14px;
    color: #e6423a;
    font-weight: 600;
  }
}
</style>
