import request from './request'

// 获取所有配置
export function getConfigList() {
  return request.get('/admin/config')
}

// 获取分组配置
export function getConfigGrouped() {
  return request.get('/admin/config/grouped')
}

// 更新单个配置
export function updateConfig(data) {
  return request.put('/admin/config', data)
}

// 批量更新配置
export function batchUpdateConfig(data) {
  return request.put('/admin/config/batch', data)
}

// 新增配置
export function addConfig(data) {
  return request.post('/admin/config', data)
}

// 删除配置
export function deleteConfig(configId) {
  return request.delete(`/admin/config/${configId}`)
}
