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
            @current-change="loadConversations"
            @size-change="loadConversations"
          />
        </div>
      </div>

      <!-- Right: chat area -->
      <div class="cs-right">
        <template v-if="activeConvId">
          <div class="cs-right__header">
            <div class="cs-right__header-info">
              <span class="cs-right__user">{{ activeConv?.username || '匿名用户' }}</span>
              <span class="cs-right__source">{{ activeConv?.sourceName || '在线咨询' }}</span>
            </div>
            <el-button
              v-if="activeConv?.status !== 2"
              type="warning"
              size="small"
              @click="closeConversation"
            >
              关闭会话
            </el-button>
          </div>

          <div class="cs-right__body" ref="msgListRef">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['cs-msg', msg.senderType === 1 ? 'cs-msg--user' : 'cs-msg--cs']"
            >
              <div class="cs-msg__bubble">{{ msg.content }}</div>
              <div class="cs-msg__time">{{ formatDate(msg.createTime) }}</div>
            </div>
            <div v-if="!messages.length" class="cs-right__empty">
              <el-empty description="暂无消息" :image-size="40" />
            </div>
          </div>

          <div class="cs-right__quick">
            <el-select
              v-model="selectedQuickReply"
              placeholder="选择快捷回复..."
              clearable
              size="small"
              @change="insertQuickReply"
            >
              <el-option
                v-for="qr in quickReplies"
                :key="qr.id"
                :label="qr.title"
                :value="qr.content"
              />
            </el-select>
          </div>

          <div class="cs-right__footer">
            <el-input
              v-model="inputMsg"
              placeholder="输入消息..."
              @keyup.enter="sendMessage"
            />
            <el-button type="primary" :disabled="!inputMsg.trim()" @click="sendMessage">
              发送
            </el-button>
          </div>
        </template>
        <div v-else class="cs-right__empty">
          <el-empty description="选择一个会话开始" :image-size="80" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getConversationList,
  getMessages,
  sendMessage as apiSendMessage,
  closeConversation as apiCloseConversation,
  getQuickReplies,
} from '@/api/chat'
import { formatDate } from '@/utils/format'

interface Conversation {
  id: number
  username: string
  sourceName: string
  status: number
  lastMessage: string
  lastActive: string
  unreadCount: number
}

interface Message {
  id: number
  content: string
  senderType: number
  createTime: string
}

const conversations = ref<Conversation[]>([])
const convLoading = ref(false)
const convPage = ref(1)
const convPageSize = ref(20)
const convTotal = ref(0)
const statusFilter = ref<number | undefined>(undefined)

const activeConvId = ref<number | null>(null)
const activeConv = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const inputMsg = ref('')
const msgListRef = ref<HTMLElement>()
const selectedQuickReply = ref('')
const quickReplies = ref<{ id: number; title: string; content: string }[]>([])

async function loadConversations() {
  convLoading.value = true
  try {
    const res = await getConversationList({
      page: convPage.value,
      pageSize: convPageSize.value,
      status: statusFilter.value,
    })
    conversations.value = res.data.records || []
    convTotal.value = res.data.total || 0
  } finally {
    convLoading.value = false
  }
}

async function selectConversation(conv: Conversation) {
  activeConvId.value = conv.id
  activeConv.value = conv
  try {
    const res = await getMessages(conv.id)
    messages.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch {
    messages.value = []
  }
}

async function sendMessage() {
  const content = inputMsg.value.trim()
  if (!content || !activeConvId.value) return
  inputMsg.value = ''
  try {
    const res = await apiSendMessage({ conversationId: activeConvId.value, content, senderType: 2 })
    messages.value.push(res.data)
    await nextTick()
    scrollToBottom()
  } catch {
    ElMessage.error('发送失败')
  }
}

function scrollToBottom() {
  if (msgListRef.value) {
    msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  }
}

async function closeConversation() {
  await ElMessageBox.confirm('确定关闭此会话？', '提示', { type: 'warning' })
  if (!activeConvId.value) return
  await apiCloseConversation(activeConvId.value)
  ElMessage.success('会话已关闭')
  loadConversations()
}

function insertQuickReply(content: string | undefined) {
  if (!content) return
  inputMsg.value = content
  selectedQuickReply.value = ''
}

async function loadQuickReplies() {
  try {
    const res = await getQuickReplies({ page: 1, pageSize: 100, enabled: true })
    quickReplies.value = (res.data || []) as any
  } catch {
    // silent
  }
}

onMounted(() => {
  loadConversations()
  loadQuickReplies()
})
</script>

<style lang="scss" scoped>
.cs-page {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}
.page-title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 20px;
  color: var(--org-text);
  letter-spacing: -.4px;
}

.cs-layout {
  flex: 1;
  display: flex;
  background: var(--org-surface);
  border-radius: var(--org-radius-xl);
  border: 1px solid var(--org-border);
  overflow: hidden;
  box-shadow: var(--org-shadow-sm);
}

/* ── Left panel ── */
.cs-left {
  width: 340px;
  border-right: 1px solid var(--org-border-soft);
  display: flex;
  flex-direction: column;

  &__toolbar {
    padding: 14px 16px;
    border-bottom: 1px solid var(--org-border-soft);
  }

  &__list {
    flex: 1;
    overflow-y: auto;
  }

  &__pager {
    padding: 12px 16px;
    border-top: 1px solid var(--org-border-soft);
    display: flex;
    justify-content: center;
  }
}

/* ── Conversation item ── */
.cs-conv {
  padding: 16px 18px;
  cursor: pointer;
  border-bottom: 1px solid var(--org-border-soft);
  transition: all var(--org-duration-fast);

  &:hover {
    background: var(--org-surface-hover);
  }

  &.is-active {
    background: var(--org-accent-soft);
    border-left: 3px solid var(--org-accent);
    padding-left: 15px;
  }

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }

  &__user {
    font-size: 14px;
    font-weight: 600;
    color: var(--org-text);
  }

  &__badge {
    background: #e08880;
    color: #fff;
    font-size: 10px;
    font-weight: 700;
    padding: 2px 7px;
    border-radius: var(--org-radius-full);
  }

  &__closed-tag {
    font-size: 11px;
    color: var(--org-text-muted);
    background: var(--org-surface-hover);
    padding: 2px 8px;
    border-radius: var(--org-radius-full);
    font-weight: 500;
  }

  &__source {
    font-size: 12px;
    color: var(--org-text-muted);
    margin-bottom: 4px;
  }

  &__last {
    font-size: 13px;
    color: var(--org-text-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__time {
    font-size: 11px;
    color: var(--org-text-muted);
    margin-top: 4px;
    font-weight: 500;
  }
}

/* ── Right panel ── */
.cs-right {
  flex: 1;
  display: flex;
  flex-direction: column;

  &__empty {
    margin: auto;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 24px;
    border-bottom: 1px solid var(--org-border-soft);
    background: #f9f7f4;
  }

  &__header-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__user {
    font-size: 15px;
    font-weight: 700;
    color: var(--org-text);
  }

  &__source {
    font-size: 12px;
    color: var(--org-text-muted);
    font-weight: 500;
  }

  &__body {
    flex: 1;
    overflow-y: auto;
    padding: 20px 24px;
    background: var(--org-bg);
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  &__quick {
    padding: 10px 24px;
    border-top: 1px solid var(--org-border-soft);
    background: #f9f7f4;
  }

  &__footer {
    display: flex;
    gap: 12px;
    padding: 14px 24px;
    border-top: 1px solid var(--org-border-soft);
    background: var(--org-surface);
    align-items: center;
  }
}

/* ── Message bubbles ── */
.cs-msg {
  display: flex;
  flex-direction: column;
  max-width: 75%;

  &--user {
    align-self: flex-end;
    align-items: flex-end;

    .cs-msg__bubble {
      background: linear-gradient(135deg, #6eb89a, #5aad8a);
      color: #fff;
      border-radius: 20px 4px 20px 20px;
      box-shadow: 0 2px 8px rgba(110, 184, 154, .25);
    }
  }

  &--cs {
    align-self: flex-start;
    align-items: flex-start;

    .cs-msg__bubble {
      background: var(--org-surface);
      color: var(--org-text);
      border-radius: 4px 20px 20px 20px;
      border: 1px solid var(--org-border-soft);
      box-shadow: var(--org-shadow-xs);
    }
  }

  &__bubble {
    padding: 12px 16px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
  }

  &__time {
    font-size: 11px;
    color: var(--org-text-muted);
    margin-top: 4px;
    padding: 0 6px;
    font-weight: 500;
  }
}
</style>
