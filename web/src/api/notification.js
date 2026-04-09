import request from '@/utils/request'

// 用户通知接口（当前产品仅展示管理员发布的系统通知）
export const listNotifications = (params) => request.get('/notifications', { params })
export const getNotificationDetail = (id) => request.get(`/notifications/${id}`)
export const countUnread = (params) => request.get('/notifications/unread', { params })
export const markAllRead = (type) => request.post('/notifications/read', null, { params: type != null ? { type } : {} })
export const markAsRead = (id) => request.post(`/notifications/${id}/read`)
export const deleteNotification = (id) => request.delete(`/notifications/${id}`)

// 管理员通知接口
export const listSystemNotifications = (params) => request.get('/admin/notifications', { params })
export const getSystemNotification = (id) => request.get(`/admin/notifications/${id}`)
export const createSystemNotification = (data) => request.post('/admin/notifications', data)
export const updateSystemNotification = (id, data) => request.put(`/admin/notifications/${id}`, data)
export const deleteSystemNotification = (id) => request.delete(`/admin/notifications/${id}`)
export const listPinnedNotifications = () => request.get('/admin/notifications/pinned')
