import request from './request'

// 获取仪表盘统计数据
export function getDashboardStats() {
  return request.get('/admin/stats')
}

// 获取专注趋势（近N天）
export function getFocusTrend(days = 7) {
  return request.get('/admin/focus/trend', { params: { days } })
}

// 获取专注模式分布
export function getFocusModes() {
  return request.get('/admin/focus/modes')
}

// 获取24小时专注分布
export function getHourlyDistribution() {
  return request.get('/admin/focus/hourly')
}

// 获取DAU趋势（近N天）
export function getDauTrend(days = 7) {
  return request.get('/admin/dau/trend', { params: { days } })
}

// 获取留存分析
export function getRetentionAnalysis(days = 7) {
  return request.get('/admin/retention', { params: { days } })
}

// 聚合接口：一次性获取所有仪表盘数据
export function getDashboardAll(days = 7) {
  return request.get('/admin/stats/all', { params: { days } })
}

// 获取AI配置
export function getAIConfig() {
  return request.get('/admin/ai/config')
}

// 更新AI系统Prompt
export function updateAIPrompt(prompt) {
  return request.put('/admin/ai/prompt', { prompt })
}

// 切换AI熔断开关
export function toggleAISwitch(enabled) {
  return request.put('/admin/ai/switch', { enabled })
}