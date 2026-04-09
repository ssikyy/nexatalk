import request from '@/utils/request'

export const follow = (userId) => request.post(`/follows/${userId}`)
export const unfollow = (userId) => request.delete(`/follows/${userId}`)
export const isFollowing = (userId) => request.get(`/follows/${userId}/status`)
export const listFollowing = (userId, params) => request.get(`/follows/${userId}/following`, { params })
export const listFollowers = (userId, params) => request.get(`/follows/${userId}/followers`, { params })
export const getFollowStats = (userId) => request.get(`/follows/${userId}/stats`)
