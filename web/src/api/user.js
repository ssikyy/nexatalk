import request from '@/utils/request'

/** 用户注册 */
export const register = (data) => request.post('/users/register', data)

/** 用户登录 */
export const login = (data) => request.post('/users/login', data)

/** 退出登录 */
export const logout = () => request.post('/users/logout')

/** 获取当前登录用户信息 */
export const getMe = () => request.get('/users/me')

/** 更新个人资料 */
export const updateProfile = (data) => request.put('/users/me', data)

/** 修改密码 */
export const updatePassword = (data) => request.put('/users/me/password', data)

/** 获取指定用户公开信息 */
export const getUserById = (id) => request.get(`/users/${id}`)

/** 分页获取用户列表 */
export const listUsers = (params) => request.get('/users', { params })

/** 上传头像 */
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 上传背景图 */
export const uploadBanner = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/users/me/banner', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ==================== 管理员接口 ====================

/** 管理员分页查询用户列表 */
export const adminListUsers = (params) => request.get('/admin/users', { params })

/** 管理员修改用户信息 */
export const adminUpdateUser = (id, data) => request.put(`/admin/users/${id}`, data)

/** 管理员重置用户密码 */
export const adminResetPassword = (id) => request.post(`/admin/users/${id}/reset-password`)
