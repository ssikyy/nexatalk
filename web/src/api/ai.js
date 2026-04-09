import request from '@/utils/request'

/** 生成帖子摘要 */
export const summarize = (data) => request.post('/ai/summarize', data)

/** 文本润色 */
export const polish = (data) => request.post('/ai/polish', data)

/** AI 扩写 */
export const expand = (data) => request.post('/ai/expand', data)

export const getAiStatus = () => request.get('/ai/status')

// ==================== 管理员接口 ====================

/** 获取 AI 配置（管理员） */
export const getAiConfig = () => request.get('/admin/ai/config')

/** 更新 AI 配置（管理员） */
export const updateAiConfig = (data) => request.post('/admin/ai/configs', data)

/** 测试润色功能 */
export const testPolishApi = (data) => request.post('/ai/polish', data)

/** 测试 AI 对话功能 */
export const testChatApi = (data) => request.post('/messages/ai/chat', data)
