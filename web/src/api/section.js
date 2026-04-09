import request from '@/utils/request'

// 公共接口：前台首页/详情使用
export const listSections = () => request.get('/sections')
export const getSectionById = (id) => request.get(`/sections/${id}`)

// 管理员接口：分区管理后台使用
export const adminListAllSections = () => request.get('/sections/all')
export const adminCreateSection = (data) => request.post('/sections', data)
export const adminUpdateSection = (id, data) => request.put(`/sections/${id}`, data)
export const adminDisableSection = (id) => request.put(`/sections/${id}/disable`)
export const adminEnableSection = (id) => request.put(`/sections/${id}/enable`)

