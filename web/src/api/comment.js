import request from '@/utils/request'

/** 获取帖子一级评论列表（含少量回复预览） */
export const listComments = (postId, params = {}) =>
  request.get('/comments', {
    params: { postId, ...params }
  })

/** 获取某评论的所有回复 */
export const listReplies = (rootId, params) =>
  request.get(`/comments/${rootId}/replies`, { params })

/** 发表评论/回复 */
export const createComment = (data) => request.post('/comments', data)

/** 删除评论 */
export const deleteComment = (id) => request.delete(`/comments/${id}`)

