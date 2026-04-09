import request from '@/utils/request'

/** 获取帖子列表（支持分区过滤、分页） */
export const listPosts = (params) => request.get('/posts', { params })

/** 获取帖子详情（同时增加浏览量） */
export const getPost = (id) => request.get(`/posts/${id}`)

/** 获取我的帖子列表 */
export const listMyPosts = (params) => request.get('/posts/mine', { params })

/** 发布帖子 */
export const createPost = (data) => request.post('/posts', data)

/** 编辑帖子 */
export const updatePost = (id, data) => request.put(`/posts/${id}`, data)

/** 删除帖子（软删除） */
export const deletePost = (id) => request.delete(`/posts/${id}`)

/** 上传帖子图片 */
export const uploadPostImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/posts/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
