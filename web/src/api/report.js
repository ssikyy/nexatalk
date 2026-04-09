import request from '@/utils/request'

// 提交举报（登录用户）
export const createReport = (data) => request.post('/reports', data)

// 管理员：查询举报列表（可按状态过滤）
export const adminListReports = (params) =>
  request.get('/reports', { params })

// 管理员：查询待审核举报列表
export const adminListPendingReports = (params) =>
  request.get('/reports/pending', { params })

// 管理员：审核举报
export const adminReviewReport = (reportId, data) =>
  request.put(`/reports/${reportId}/review`, data)

// 管理员：按被举报实体清理所有相关举报记录
export const adminClearReportsByEntity = (params) =>
  request.delete('/reports/by-entity', { params })

