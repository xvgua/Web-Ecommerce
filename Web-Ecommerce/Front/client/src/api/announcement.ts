import request from './request'
import type { ApiResponse, PageResponse, Announcement } from '@shared/types'

export function getAnnouncements(limit = 5): Promise<ApiResponse<Announcement[]>> {
  return request.get('/announcements', { params: { limit } })
}

export function getAnnouncementPage(params: { page: number; pageSize: number }): Promise<ApiResponse<PageResponse<Announcement>>> {
  return request.get('/announcements/page', { params })
}
