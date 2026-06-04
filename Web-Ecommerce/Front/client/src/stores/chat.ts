import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createConversation, getUserConversations, getMessages,
  sendMessage, closeConversation, getCsStatus
} from '@/api/chat'
import type { Conversation, ChatMessage } from '@shared/types/chat'

export interface ProductChatContext {
  productId: number
  productName: string
  productImage: string
  price: number
  specDesc?: string
}

export const useChatStore = defineStore('chat', () => {
  const isOpen = ref(false)
  const conversationId = ref<number | null>(null)
  const conversations = ref<Conversation[]>([])
  const messages = ref<ChatMessage[]>([])
  const csOnline = ref(false)
  const inputText = ref('')
  const sending = ref(false)
  const loading = ref(false)
  const productContext = ref<ProductChatContext | null>(null)

  let pollTimer: ReturnType<typeof setInterval> | null = null

  function startPolling() {
    stopPolling()
    pollTimer = setInterval(async () => {
      if (!conversationId.value) return
      try {
        const res = await getMessages(conversationId.value)
        messages.value = res.data
      } catch { /* ignore */ }
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

  async function openChat(product?: ProductChatContext) {
    isOpen.value = true
    loading.value = true
    productContext.value = product || null

    try {
      const convRes = await getUserConversations()
      conversations.value = convRes.data

      if (product) {
        const existing = conversations.value.find(
          c => c.sourceType === 1 && c.sourceId === product.productId && c.status === 1
        )
        if (existing) {
          conversationId.value = existing.id
        } else {
          const createRes = await createConversation({
            sourceType: 1,
            sourceId: product.productId,
            sourceName: product.productName,
            firstMessage: '',
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

      if (conversationId.value) {
        const msgRes = await getMessages(conversationId.value)
        messages.value = msgRes.data
        startPolling()
      }

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
    productContext.value = null
  }

  async function handleSend(contentType?: number, extraData?: string) {
    const text = inputText.value.trim()
    if ((!text && !extraData) || !conversationId.value) return
    sending.value = true
    try {
      const content = text || (extraData ? JSON.parse(extraData).productName : '')
      const res = await sendMessage(conversationId.value, content, contentType || 1, extraData)
      messages.value = [...messages.value, res.data]
      inputText.value = ''
    } catch { /* handled by interceptor */ }
    finally { sending.value = false }
  }

  async function sendProductCard() {
    const ctx = productContext.value
    if (!ctx || !conversationId.value) return
    sending.value = true
    try {
      const cardData = JSON.stringify({
        productId: ctx.productId,
        productName: ctx.productName,
        productImage: ctx.productImage,
        price: ctx.price,
        specDesc: ctx.specDesc || '',
      })
      const res = await sendMessage(conversationId.value, ctx.productName, 2, cardData)
      messages.value = [...messages.value, res.data]
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

  return {
    isOpen, conversationId, conversations, messages, csOnline,
    inputText, sending, loading, productContext,
    openChat, closeChat, handleSend, sendProductCard,
    handleCloseConversation, switchConversation, stopPolling,
  }
})
