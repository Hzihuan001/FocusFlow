import dayjs from 'dayjs'

/**
 * 时间格式化
 */
export const formatTime = (timestamp, format = 'YYYY-MM-DD HH:mm:ss') => {
  if (!timestamp) return '-'
  return dayjs(timestamp).format(format)
}

/**
 * 短日期格式化
 */
export const formatDate = (timestamp) => {
  if (!timestamp) return '-'
  return dayjs(timestamp).format('YYYY-MM-DD')
}

/**
 * 时长格式化（分钟转为小时分钟）
 */
export const formatDuration = (minutes) => {
  if (!minutes || minutes <= 0) return '0分钟'
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (hours > 0 && mins > 0) {
    return `${hours}小时${mins}分钟`
  } else if (hours > 0) {
    return `${hours}小时`
  } else {
    return `${mins}分钟`
  }
}

/**
 * 稀有度名称
 */
export const getRarityName = (rarity) => {
  const names = ['N', 'R', 'SR', 'SSR']
  return names[rarity] || 'N'
}

/**
 * 稀有度标签类型（Element Plus）
 */
export const getRarityType = (rarity) => {
  const types = ['', 'success', 'warning', 'danger']
  return types[rarity] || ''
}

/**
 * 稀有度颜色
 */
export const getRarityColor = (rarity) => {
  const colors = ['#909399', '#67c23a', '#e6a23c', '#f56c6c']
  return colors[rarity] || colors[0]
}

/**
 * 用户状态名称
 */
export const getUserStatusName = (status) => {
  return status === 0 ? '正常' : '封禁'
}

/**
 * 用户状态标签类型
 */
export const getUserStatusType = (status) => {
  return status === 0 ? 'success' : 'danger'
}

/**
 * 数字千分位格式化
 */
export const formatNumber = (num) => {
  if (!num && num !== 0) return '-'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 文件大小格式化
 */
export const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
