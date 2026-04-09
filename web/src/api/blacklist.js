import request from '@/utils/request'

export const getBlacklist = () => request.get('/blacklist')
export const addToBlacklist = (userId) => request.post(`/blacklist/${userId}`)
export const removeFromBlacklist = (userId) => request.delete(`/blacklist/${userId}`)
export const checkBlacklist = (userId) => request.get(`/blacklist/check/${userId}`)
