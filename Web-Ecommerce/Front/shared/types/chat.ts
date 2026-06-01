export interface Conversation {
  id: number
  userId: number
  username: string
  avatar: string
  subject: string
  sourceType: number
  sourceId: number | null
  sourceName: string
  status: number          // 1=进行中 2=已关闭
  unreadCount: number
  userUnread: number
  lastMessage: string
  lastActive: string
  createTime: string
  closeTime?: string
  totalMessages?: number
}

export interface ChatMessage {
  id: number
  conversationId: number
  senderType: number      // 1=用户 2=客服
  senderId: number
  senderName: string
  senderAvatar: string
  content: string
  contentType: number
  isRead: number
  createTime: string
}

export interface QuickReply {
  id: number
  title: string
  content: string
  sortOrder: number
  status: number
  createTime: string
}
