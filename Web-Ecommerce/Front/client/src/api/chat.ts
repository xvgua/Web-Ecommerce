import request from './request'
import type { ApiResponse } from '@shared/types'
import type { Conversation, ChatMessage } from '@shared/types/chat'

export function createConversation(data: {
  sourceType: number
  sourceId?: number
  sourceName?: string
  firstMessage?: string
}): Promise<ApiResponse<Conversation>> {
  return request.post('/conversations', data)
}

export function getUserConversations(): Promise<ApiResponse<Conversation[]>> {
  return request.get('/conversations')
}

export function getMessages(conversationId: number): Promise<ApiResponse<ChatMessage[]>> {
  return request.get(`/conversations/${conversationId}/messages`)
}

export function sendMessage(conversationId: number, content: string): Promise<ApiResponse<ChatMessage>> {
  return request.post(`/conversations/${conversationId}/messages`, { content })
}

export function closeConversation(conversationId: number): Promise<ApiResponse<null>> {
  return request.put(`/conversations/${conversationId}/close`)
}

export function getCsStatus(): Promise<ApiResponse<{ online: boolean }>> {
  return request.get('/customer-service/status')
}
