import request from '@/utils/request'

// ==================== 系统配置管理接口 ====================

/**
 * 获取系统配置列表（分页）
 * @param {Object} params - { pageNum, pageSize, keyword }
 */
export const getSystemConfigList = (params) => request.get('/admin/system/configs', { params })

/**
 * 获取所有系统配置（不分页）
 */
export const getAllSystemConfigs = () => request.get('/admin/system/configs/all')

/**
 * 获取单个配置详情
 * @param {number} id - 配置ID
 */
export const getSystemConfigById = (id) => request.get(`/admin/system/configs/${id}`)

/**
 * 根据键名获取配置
 * @param {string} key - 配置键
 */
export const getSystemConfigByKey = (key) => request.get(`/admin/system/configs/key/${key}`)

/**
 * 创建系统配置
 * @param {Object} data - { configKey, configValue, configType, description }
 */
export const createSystemConfig = (data) => request.post('/admin/system/configs', data)

/**
 * 更新系统配置
 * @param {number} id - 配置ID
 * @param {Object} data - { configKey, configValue, configType, description }
 */
export const updateSystemConfig = (id, data) => request.put(`/admin/system/configs/${id}`, data)

/**
 * 根据键名更新配置
 * @param {string} key - 配置键
 * @param {Object} data - { configValue, description }
 */
export const updateSystemConfigByKey = (key, data) => request.put(`/admin/system/configs/key/${key}`, data)

/**
 * 删除系统配置
 * @param {number} id - 配置ID
 */
export const deleteSystemConfig = (id) => request.delete(`/admin/system/configs/${id}`)

/**
 * 根据键名删除配置
 * @param {string} key - 配置键
 */
export const deleteSystemConfigByKey = (key) => request.delete(`/admin/system/configs/key/${key}`)

/**
 * 批量更新配置
 * @param {Array} configs - 配置数组 [{ configKey, configValue, description }]
 */
export const batchUpdateSystemConfigs = (configs) => request.post('/admin/system/configs/batch', configs)
