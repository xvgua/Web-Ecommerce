import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Conversation, ChatMessage, QuickReply } from '@shared/types/chat'

export function getConversations(params: {
  page: number
  pageSize: number
  status?: number
  keyword?: string
}): Promise<ApiResponse<PageResponse<Conversation>>> {
  return request.get('/admin/conversations', { params })
}

export function getConversationMessages(id: number, skipErrorToast = false): Promise<ApiResponse<ChatMessage[]>> {
  return request.get(`/admin/conversations/${id}/messages`, {
    _skipErrorToast: skipErrorToast,
  } as any)
}

export function replyConversation(id: number, content: string): Promise<ApiResponse<ChatMessage>> {
  return request.post(`/admin/conversations/${id}/messages`, { content })
}

export function adminCloseConversation(id: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/conversations/${id}/close`)
}

export function getQuickReplies(): Promise<ApiResponse<QuickReply[]>> {
  return request.get('/admin/quick-replies')
}

export function createQuickReply(data: Partial<QuickReply>): Promise<ApiResponse<QuickReply>> {
  return request.post('/admin/quick-replies', data)
}

export function updateQuickReply(id: number, data: Partial<QuickReply>): Promise<ApiResponse<null>> {
  return request.put(`/admin/quick-replies/${id}`, data)
}

export function deleteQuickReply(id: number): Promise<ApiResponse<null>> {
  return request.delete(`/admin/quick-replies/${id}`)
}

export function toggleQuickReplyStatus(id: number, status: number): Promise<ApiResponse<null>> {
  return request.put(`/admin/quick-replies/${id}/status`, { status })
}
