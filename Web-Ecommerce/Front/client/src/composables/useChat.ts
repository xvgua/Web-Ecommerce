import { ref, onUnmounted } from 'vue'
import {
  createConversation, getUserConversations, getMessages,
  sendMessage, closeConversation, getCsStatus
} from '@/api/chat'
import type { Conversation, ChatMessage } from '@shared/types/chat'

const isOpen = ref(false)
const conversationId = ref<number | null>(null)
const conversations = ref<Conversation[]>([])
const messages = ref<ChatMessage[]>([])
const csOnline = ref(false)
const inputText = ref('')
const sending = ref(false)
const loading = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null
let initSourceType = 0
let initSourceId: number | null = null
let initSourceName = ''

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!conversationId.value) return
    try {
      const res = await getMessages(conversationId.value)
      messages.value = res.data
    } catch { /* ignore polling errors */ }
    try {
      const statusRes = await getCsStatus()
      csOnline.value = statusRes.data.online
    } catch { /* ignore */ }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

export function useChat() {
  async function openChat(opts?: { sourceType?: number; sourceId?: number; sourceName?: string; firstMessage?: string }) {
    initSourceType = opts?.sourceType || 3
    initSourceId = opts?.sourceId || null
    initSourceName = opts?.sourceName || ''

    isOpen.value = true
    loading.value = true

    try {
      // Load existing conversations
      const convRes = await getUserConversations()
      conversations.value = convRes.data

      // Try to find existing conversation or create one
      if (opts?.sourceType && opts?.sourceId) {
        const existing = conversations.value.find(
          c => c.sourceType === opts.sourceType && c.sourceId === opts.sourceId && c.status === 1
        )
        if (existing) {
          conversationId.value = existing.id
        } else {
          const createRes = await createConversation({
            sourceType: opts.sourceType,
            sourceId: opts.sourceId,
            sourceName: opts.sourceName,
            firstMessage: opts.firstMessage || '',
          })
          conversationId.value = createRes.data.id
        }
      } else if (conversations.value.length > 0 && conversations.value[0].status === 1) {
        conversationId.value = conversations.value[0].id
      } else {
        const createRes = await createConversation({
          sourceType: 3,
          firstMessage: '',
        })
        conversationId.value = createRes.data.id
      }

      // Load messages
      if (conversationId.value) {
        const msgRes = await getMessages(conversationId.value)
        messages.value = msgRes.data
        startPolling()
      }

      // Check online status
      try {
        const statusRes = await getCsStatus()
        csOnline.value = statusRes.data.online
      } catch { /* ignore */ }
    } finally {
      loading.value = false
    }
  }

  function closeChat() {
    stopPolling()
    isOpen.value = false
    conversationId.value = null
    messages.value = []
  }

  async function handleSend() {
    const text = inputText.value.trim()
    if (!text || !conversationId.value) return
    sending.value = true
    try {
      const res = await sendMessage(conversationId.value, text)
      messages.value.push(res.data)
      inputText.value = ''
    } catch { /* handled by interceptor */ }
    finally { sending.value = false }
  }

  async function handleCloseConversation() {
    if (!conversationId.value) return
    await closeConversation(conversationId.value)
    closeChat()
  }

  async function switchConversation(id: number) {
    stopPolling()
    conversationId.value = id
    loading.value = true
    try {
      const res = await getMessages(id)
      messages.value = res.data
      startPolling()
    } finally {
      loading.value = false
    }
  }

  // Cleanup on component unmount
  onUnmounted(() => stopPolling())

  return {
    isOpen,
    conversationId,
    conversations,
    messages,
    csOnline,
    inputText,
    sending,
    loading,
    openChat,
    closeChat,
    handleSend,
    handleCloseConversation,
    switchConversation,
  }
}
