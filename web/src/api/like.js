import request from '@/utils/request'

export const likePost = (postId) => request.post(`/likes/posts/${postId}`)
export const unlikePost = (postId) => request.delete(`/likes/posts/${postId}`)
export const hasLikedPost = (postId) => request.get(`/likes/posts/${postId}`)

export const likeComment = (commentId) => request.post(`/likes/comments/${commentId}`)
export const unlikeComment = (commentId) => request.delete(`/likes/comments/${commentId}`)
export const hasLikedComment = (commentId) => request.get(`/likes/comments/${commentId}`)

// 当前登录用户的点赞帖子列表（“我的-点赞”）
export const listMyLikes = (params) => request.get('/likes/mine', { params })
