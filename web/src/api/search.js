import request from '@/utils/request'

export const searchPosts = (params) => request.get('/search/posts', { params })
export const searchUsers = (params) => request.get('/search/users', { params })
