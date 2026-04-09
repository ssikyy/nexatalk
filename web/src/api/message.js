import request from '@/utils/request'

export const sendMessage = (data) => request.post('/messages', data)
export const listConversations = (params) => request.get('/messages/conversations', { params })
export const listMessages = (conversationId, params) =>
  request.get(`/messages/conversations/${conversationId}`, { params })
export const readConversation = (conversationId) =>
  request.post(`/messages/conversations/${conversationId}/read`)
export const countUnreadMessages = () => request.get('/messages/unread')
export const deleteConversation = (conversationId) =>
  request.delete(`/messages/conversations/${conversationId}`)
export const clearMessages = (conversationId) =>
  request.delete(`/messages/conversations/${conversationId}/messages`)
