import request from './request'

// 获取所有植物
export function getPlantList() {
  return request.get('/admin/plant')
}

// 获取单个植物
export function getPlantDetail(plantId) {
  return request.get(`/admin/plant/${plantId}`)
}

// 新增植物
export function addPlant(data) {
  return request.post('/admin/plant', data)
}

// 更新植物
export function updatePlant(plantId, data) {
  return request.put(`/admin/plant/${plantId}`, data)
}

// 删除植物
export function deletePlant(plantId) {
  return request.delete(`/admin/plant/${plantId}`)
}

// 更新植物状态
export function updatePlantStatus(plantId, status) {
  return request.put(`/admin/plant/${plantId}/status`, null, { params: { status } })
}

// 上传植物图片
export function uploadPlantImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/plant/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
