import request from '@/utils/request'

/** 获取最新帖子流 */
export const getLatestFeed = (params) => request.get('/feed/latest', { params })

/** 获取关注的人的帖子流 */
export const getFollowFeed = (params) => request.get('/feed/follow', { params })

/** 获取热门帖子流 */
export const getHotFeed = (params) => request.get('/feed/hot', { params })
