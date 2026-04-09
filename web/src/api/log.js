import request from '@/utils/request'

/** 管理员分页查询操作日志 */
export const adminListLogs = (params) => request.get('/admin/logs', { params })

/** 清空所有操作日志（仅超级管理员） */
export const adminClearLogs = () => request.post('/admin/logs/clear')
