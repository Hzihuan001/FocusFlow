import request from './request'

// 获取用户列表
export function getUserList(params) {
  return request.get('/admin/user', { params })
}

// 获取用户详情
export function getUserDetail(userId) {
  return request.get(`/admin/user/${userId}`)
}

// 新增用户
export function addUser(data) {
  return request.post('/admin/user', data)
}

// 更新用户信息（账号、密码、昵称）
export function updateUserInfo(userId, data) {
  return request.put(`/admin/user/${userId}`, data)
}

// 更新用户光流
export function updateTimeFlux(userId, timeFlux) {
  return request.put(`/admin/user/${userId}/flux`, null, { params: { timeFlux } })
}

// 封禁用户
export function banUser(userId) {
  return request.put(`/admin/user/${userId}/ban`)
}

// 解封用户
export function unbanUser(userId) {
  return request.put(`/admin/user/${userId}/unban`)
}

// 获取用户统计
export function getUserStats() {
  return request.get('/admin/user/stats')
}
