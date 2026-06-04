export interface Feedback {
  id: number
  userId: number
  type: number
  typeText: string
  title: string
  content: string
  contact: string
  images: string
  status: number
  statusText: string
  adminReply: string | null
  adminId: number | null
  username: string
  userEmail: string
  handleTime: string | null
  createTime: string
  updateTime: string
}

export interface FeedbackSubmitForm {
  type: number
  title: string
  content: string
  contact?: string
  images?: string[]
}
