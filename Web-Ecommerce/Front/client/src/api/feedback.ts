import request from './request'
import type { ApiResponse, PageResponse } from '@shared/types'
import type { Feedback, FeedbackSubmitForm } from '@shared/types/feedback'

export function submitFeedback(data: FeedbackSubmitForm): Promise<ApiResponse<null>> {
  return request.post('/feedback', data)
}

export function getMyFeedbackList(page: number, pageSize: number): Promise<ApiResponse<PageResponse<Feedback>>> {
  return request.get('/feedback/my', { params: { page, pageSize } })
}

export function getMyFeedbackDetail(id: number): Promise<ApiResponse<Feedback>> {
  return request.get(`/feedback/my/${id}`)
}
