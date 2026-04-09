import request from '@/utils/request'

// 管理员：下处罚（禁言 / 封号）
export const adminCreatePunishment = (data) =>
  request.post('/punishments', data)

// 管理员：解除处罚
export const adminLiftPunishment = (punishmentId) =>
  request.delete(`/punishments/${punishmentId}`)

// 管理员：查询当前生效的处罚列表（分页）
export const adminListActivePunishments = (params) =>
  request.get('/punishments/active', { params })

// 管理员：查询某用户处罚历史（分页）
export const adminListUserPunishments = (userId, params) =>
  request.get(`/punishments/user/${userId}`, { params })

