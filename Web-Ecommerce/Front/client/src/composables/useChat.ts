import { useChatStore } from '@/stores/chat'

export function useChat() {
  const store = useChatStore()

  return {
    isOpen: store.isOpen,
    conversationId: store.conversationId,
    conversations: store.conversations,
    messages: store.messages,
    csOnline: store.csOnline,
    inputText: store.inputText,
    sending: store.sending,
    loading: store.loading,
    openChat: (opts?: { sourceType?: number; sourceId?: number; sourceName?: string }) => {
      store.openChat(opts?.sourceType === 1 && opts?.sourceId
        ? { productId: opts.sourceId, productName: opts.sourceName || '', productImage: '', price: 0 }
        : undefined)
    },
    closeChat: store.closeChat,
    handleSend: () => store.handleSend(),
    handleCloseConversation: store.handleCloseConversation,
    switchConversation: store.switchConversation,
  }
}
