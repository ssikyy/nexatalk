import request from '@/utils/request'

export const favorite = (postId) => request.post(`/favorites/${postId}`)
export const unfavorite = (postId) => request.delete(`/favorites/${postId}`)
export const hasFavorited = (postId) => request.get(`/favorites/${postId}`)
export const listMyFavorites = (params) => request.get('/favorites/mine', { params })
