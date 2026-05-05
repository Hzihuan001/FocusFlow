<template>
  <div class="dashboard-page">
    <!-- ═══════════════════════════════════════════════════════════════════════ -->
    <!-- 一、核心指标概览 -->
    <!-- ═══════════════════════════════════════════════════════════════════════ -->
    <div class="section-title">
      <el-icon><DataAnalysis /></el-icon>
      <span>核心指标概览</span>
    </div>
    
    <el-row :gutter="16" class="stats-row">
      <!-- 用户生态 -->
      <el-col :xs="12" :sm="8" :lg="6">
        <div class="stat-card user-eco">
          <div class="stat-header">
            <el-icon class="stat-icon"><User /></el-icon>
            <span class="stat-title">用户生态</span>
          </div>
          <div class="stat-items">
            <div class="stat-item">
              <span class="stat-value">{{ formatNumber(stats.totalUsers) }}</span>
              <span class="stat-label">总注册用户</span>
            </div>
            <div class="stat-item">
              <span class="stat-value highlight">{{ stats.todayNewUsers || 0 }}</span>
              <span class="stat-label">今日新增</span>
            </div>
            <div class="stat-item">
              <span class="stat-value highlight">{{ stats.todayDAU || 0 }}</span>
              <span class="stat-label">今日活跃</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <!-- 专注大盘 -->
      <el-col :xs="12" :sm="8" :lg="6">
        <div class="stat-card focus-board">
          <div class="stat-header">
            <el-icon class="stat-icon"><Timer /></el-icon>
            <span class="stat-title">专注大盘</span>
          </div>
          <div class="stat-items">
            <div class="stat-item">
              <span class="stat-value">{{ formatDuration(stats.totalDurationMinutes) }}</span>
              <span class="stat-label">累计专注</span>
            </div>
            <div class="stat-item">
              <span class="stat-value highlight">{{ formatDuration(stats.todayDurationMinutes) }}</span>
              <span class="stat-label">今日专注</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.totalFocusRecords || 0 }}</span>
              <span class="stat-label">总记录数</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <!-- 经济系统 -->
      <el-col :xs="12" :sm="8" :lg="6">
        <div class="stat-card economy">
          <div class="stat-header">
            <el-icon class="stat-icon"><Coin /></el-icon>
            <span class="stat-title">经济系统</span>
          </div>
          <div class="stat-items">
            <div class="stat-item">
              <span class="stat-value">{{ formatNumber(stats.totalProducedTimeFlux) }}</span>
              <span class="stat-label">累计产出光流</span>
            </div>
            <div class="stat-item">
              <span class="stat-value highlight">{{ formatNumber(stats.circulatingTimeFlux) }}</span>
              <span class="stat-label">流通光流</span>
            </div>
          </div>
        </div>
      </el-col>
      
      <!-- 快捷入口 -->
      <el-col :xs="12" :sm="8" :lg="6">
        <div class="stat-card quick-entry">
          <div class="stat-header">
            <el-icon class="stat-icon"><Grid /></el-icon>
            <span class="stat-title">业务管控</span>
          </div>
          <div class="quick-actions">
            <el-button size="small" type="primary" @click="$router.push('/user')">
              <el-icon><User /></el-icon>用户管理
            </el-button>
            <el-button size="small" type="success" @click="$router.push('/plant')">
              <el-icon><Cherry /></el-icon>植物图鉴
            </el-button>
            <el-button size="small" type="warning" @click="$router.push('/config')">
              <el-icon><Setting /></el-icon>系统配置
            </el-button>
            <el-button size="small" type="info" @click="$router.push('/ai')">
              <el-icon><Cpu /></el-icon>AI风控
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ═══════════════════════════════════════════════════════════════════════ -->
    <!-- 二、用户行为趋势分析 -->
    <!-- ═══════════════════════════════════════════════════════════════════════ -->
    <div class="section-title">
      <el-icon><TrendCharts /></el-icon>
      <span>用户行为趋势分析</span>
    </div>
    
    <el-row :gutter="16" class="chart-row">
      <!-- 专注流派偏好分布 -->
      <el-col :xs="24" :lg="8">
        <el-card class="chart-card">
          <template #header>
            <span class="card-title">专注流派偏好分布</span>
          </template>
          <div ref="modeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <!-- 近7日专注时长趋势 -->
      <el-col :xs="24" :lg="8">
        <el-card class="chart-card">
          <template #header>
            <span class="card-title">近7日专注时长趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <!-- 活跃时段热力图 -->
      <el-col :xs="24" :lg="8">
        <el-card class="chart-card">
          <template #header>
            <span class="card-title">活跃时段分布</span>
          </template>
          <div ref="hourlyChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ═══════════════════════════════════════════════════════════════════════ -->
    <!-- 三、用户活跃度分析 -->
    <!-- ═══════════════════════════════════════════════════════════════════════ -->
    <div class="section-title">
      <el-icon><DataLine /></el-icon>
      <span>用户活跃度分析</span>
    </div>
    
    <el-row :gutter="16" class="chart-row">
      <!-- DAU趋势 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header-flex">
              <span class="card-title">DAU趋势（日活跃用户）</span>
              <el-radio-group v-model="dauDays" size="small" @change="initDauChart">
                <el-radio-button :value="7">7日</el-radio-button>
                <el-radio-button :value="14">14日</el-radio-button>
                <el-radio-button :value="30">30日</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="dauChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      
      <!-- 留存分析 -->
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header-flex">
              <span class="card-title">留存分析</span>
              <el-radio-group v-model="retentionDays" size="small" @change="initRetentionChart">
                <el-radio-button :value="7">7日</el-radio-button>
                <el-radio-button :value="14">14日</el-radio-button>
                <el-radio-button :value="30">30日</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="retentionChartRef" class="chart-container"></div>
          <!-- 留存率汇总 -->
          <div class="retention-summary" v-if="retentionData.avgDay1Retention">
            <div class="retention-item">
              <span class="retention-label">平均次日留存</span>
              <span class="retention-value">{{ retentionData.avgDay1Retention }}%</span>
            </div>
            <div class="retention-item">
              <span class="retention-label">平均3日留存</span>
              <span class="retention-value">{{ retentionData.avgDay3Retention }}%</span>
            </div>
            <div class="retention-item">
              <span class="retention-label">平均7日留存</span>
              <span class="retention-value">{{ retentionData.avgDay7Retention }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { 
  getDashboardStats, 
  getFocusTrend, 
  getFocusModes, 
  getHourlyDistribution,
  getDauTrend,
  getRetentionAnalysis
} from '@/api/dashboard'
import { formatDuration } from '@/utils/format'

const stats = ref({})

// Chart refs
const modeChartRef = ref(null)
const trendChartRef = ref(null)
const hourlyChartRef = ref(null)
const dauChartRef = ref(null)
const retentionChartRef = ref(null)
let modeChart = null
let trendChart = null
let hourlyChart = null
let dauChart = null
let retentionChart = null

// DAU和留存参数
const dauDays = ref(7)
const retentionDays = ref(7)
const retentionData = ref({})

// 格式化数字
const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num.toLocaleString()
}

// 获取统计数据
const fetchStats = async () => {
  try {
    stats.value = await getDashboardStats()
  } catch (e) {
    console.error('获取统计失败', e)
  }
}

// 初始化图表
const initCharts = async () => {
  await Promise.all([
    initModeChart(),
    initTrendChart(),
    initHourlyChart(),
    initDauChart(),
    initRetentionChart()
  ])
}

// 专注模式分布图
const initModeChart = async () => {
  try {
    const data = await getFocusModes()
    modeChart = echarts.init(modeChartRef.value)
    modeChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}次 ({d}%)' },
      legend: {
        orient: 'vertical',
        right: 10,
        top: 'center',
        textStyle: { color: '#333333' }
      },
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#E0E0E0',
          borderWidth: 2
        },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#333333' }
        },
        data: data.map(item => ({
          name: item.name,
          value: item.value,
          itemStyle: { color: item.color }
        }))
      }]
    })
  } catch (e) {
    console.error('初始化模式图失败', e)
  }
}

// 趋势图
const initTrendChart = async () => {
  try {
    const data = await getFocusTrend(7)
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { 
        trigger: 'axis',
        formatter: '{b}<br/>专注时长: {c}分钟'
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map(d => d.date),
        axisLine: { lineStyle: { color: '#E0E0E0' } },
        axisLabel: { color: '#333333' }
      },
      yAxis: {
        type: 'value',
        name: '分钟',
        nameTextStyle: { color: '#333333' },
        axisLine: { lineStyle: { color: '#E0E0E0' } },
        axisLabel: { color: '#333333' },
        splitLine: { lineStyle: { color: '#E0E0E0' } }
      },
      series: [{
        name: '专注时长',
        type: 'line',
        smooth: true,
        data: data.map(d => d.duration),
        lineStyle: { color: '#00ff88', width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 255, 136, 0.3)' },
            { offset: 1, color: 'rgba(0, 255, 136, 0)' }
          ])
        },
        itemStyle: { color: '#00ff88' }
      }]
    })
  } catch (e) {
    console.error('初始化趋势图失败', e)
  }
}

// 时段分布图
const initHourlyChart = async () => {
  try {
    const data = await getHourlyDistribution()
    hourlyChart = echarts.init(hourlyChartRef.value)
    hourlyChart.setOption({
      tooltip: { 
        trigger: 'axis',
        formatter: '{b}<br/>专注次数: {c}'
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map(d => d.label),
        axisLine: { lineStyle: { color: '#E0E0E0' } },
        axisLabel: { 
          color: '#333333',
          interval: 3
        }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: '#E0E0E0' } },
        axisLabel: { color: '#333333' },
        splitLine: { lineStyle: { color: '#E0E0E0' } }
      },
      series: [{
        name: '专注次数',
        type: 'bar',
        data: data.map(d => d.count),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#2196f3' },
            { offset: 1, color: '#00ff88' }
          ]),
          borderRadius: [4, 4, 0, 0]
        }
      }]
    })
  } catch (e) {
    console.error('初始化时段图失败', e)
  }
}

// DAU趋势图
const initDauChart = async () => {
  try {
    const data = await getDauTrend(dauDays.value)
    if (!dauChart) {
      dauChart = echarts.init(dauChartRef.value)
    }
    dauChart.setOption({
      tooltip: { 
        trigger: 'axis',
        formatter: (params) => {
          const dau = params[0]
          const newUsers = params[1]
          return `${dau.axisValue}<br/>
            DAU: ${dau.value}<br/>
            新增用户: ${newUsers.value}`
        }
      },
      legend: {
        data: ['DAU', '新增用户'],
        textStyle: { color: '#333333' },
        top: 0
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map(d => d.date),
        axisLine: { lineStyle: { color: '#E0E0E0' } },
        axisLabel: { color: '#333333' }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: '#E0E0E0' } },
        axisLabel: { color: '#333333' },
        splitLine: { lineStyle: { color: '#E0E0E0' } }
      },
      series: [
        {
          name: 'DAU',
          type: 'line',
          smooth: true,
          data: data.map(d => d.dau),
          lineStyle: { color: '#00ff88', width: 3 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(0, 255, 136, 0.3)' },
              { offset: 1, color: 'rgba(0, 255, 136, 0)' }
            ])
          },
          itemStyle: { color: '#00ff88' }
        },
        {
          name: '新增用户',
          type: 'bar',
          data: data.map(d => d.newUsers),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2196f3' },
              { offset: 1, color: '#1565c0' }
            ]),
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '40%'
        }
      ]
    })
  } catch (e) {
    console.error('初始化DAU趋势图失败', e)
  }
}

// 留存分析图
const initRetentionChart = async () => {
  try {
    const res = await getRetentionAnalysis(retentionDays.value)
    retentionData.value = res
    const data = res.data || []
    
    if (!retentionChart) {
      retentionChart = echarts.init(retentionChartRef.value)
    }
    retentionChart.setOption({
      tooltip: { 
        trigger: 'axis',
        formatter: (params) => {
          const item = data[params[0].dataIndex]
          return `${item.date}<br/>
            新增用户: ${item.newUsers}<br/>
            次日留存: ${item.day1Retention}%<br/>
            3日留存: ${item.day3Retention}%<br/>
            7日留存: ${item.day7Retention}%`
        }
      },
      legend: {
        data: ['次日留存', '3日留存', '7日留存'],
        textStyle: { color: '#333333' },
        top: 0
      },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map(d => d.date),
        axisLine: { lineStyle: { color: '#E0E0E0' } },
        axisLabel: { color: '#333333' }
      },
      yAxis: {
        type: 'value',
        max: 100,
        axisLabel: { 
          color: '#333333',
          formatter: '{value}%'
        },
        axisLine: { lineStyle: { color: '#E0E0E0' } },
        splitLine: { lineStyle: { color: '#E0E0E0' } }
      },
      series: [
        {
          name: '次日留存',
          type: 'line',
          smooth: true,
          data: data.map(d => d.day1Retention),
          lineStyle: { color: '#00ff88', width: 2 },
          itemStyle: { color: '#00ff88' }
        },
        {
          name: '3日留存',
          type: 'line',
          smooth: true,
          data: data.map(d => d.day3Retention),
          lineStyle: { color: '#ffd700', width: 2 },
          itemStyle: { color: '#ffd700' }
        },
        {
          name: '7日留存',
          type: 'line',
          smooth: true,
          data: data.map(d => d.day7Retention),
          lineStyle: { color: '#ff6b6b', width: 2 },
          itemStyle: { color: '#ff6b6b' }
        }
      ]
    })
  } catch (e) {
    console.error('初始化留存分析图失败', e)
  }
}

// 窗口大小变化处理
const handleResize = () => {
  modeChart?.resize()
  trendChart?.resize()
  hourlyChart?.resize()
  dauChart?.resize()
  retentionChart?.resize()
}

onMounted(async () => {
  await fetchStats()
  await nextTick()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  modeChart?.dispose()
  trendChart?.dispose()
  hourlyChart?.dispose()
  dauChart?.dispose()
  retentionChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
@import '@/styles/index.scss';

.dashboard-page {
  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 24px 0 16px;
    padding-left: 12px;
    border-left: 3px solid $primary-color;
    font-size: 16px;
    font-weight: 500;
    color: $text-primary;
    
    &:first-child {
      margin-top: 0;
    }
    
    .el-icon {
      color: $primary-color;
    }
  }
  
  .stats-row {
    margin-bottom: 0;
  }
  
  .stat-card {
    padding: 16px;
    background: $bg-card;
    border: 1px solid $border-color;
    border-radius: 12px;
    height: 160px;
    margin-bottom: 16px;
    display: flex;
    flex-direction: column;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    
    .stat-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      
      .stat-icon {
        font-size: 20px;
        color: $primary-color;
      }
      
      .stat-title {
        font-size: 14px;
        color: $text-secondary;
      }
    }
    
    .stat-items {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      
      .stat-item {
        min-width: 80px;
        
        .stat-value {
          font-size: 22px;
          font-weight: 600;
          color: $text-primary;
          
          &.highlight {
            color: $primary-color;
          }
        }
        
        .stat-label {
          font-size: 12px;
          color: $text-secondary;
        }
      }
    }
    
    &.user-eco .stat-icon { color: #2196f3; }
    &.focus-board .stat-icon { color: #ff9800; }
    &.economy .stat-icon { color: #ffd700; }
    
    &.quick-entry {
      .quick-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        
        .el-button {
          padding: 6px 12px;
        }
      }
    }
  }
  
  .chart-row {
    margin-bottom: 0;
  }
  
  .chart-card {
    background: $bg-card;
    border: 1px solid $border-color;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    
    :deep(.el-card__header) {
      border-bottom: 1px solid #E0E0E0;
      padding: 12px 16px;
    }
    
    .card-title {
      color: $text-primary;
      font-weight: 500;
      font-size: 14px;
    }
    
    .chart-container {
      height: 260px;
    }
    
    .card-header-flex {
      display: flex;
      align-items: center;
      justify-content: space-between;
      
      .card-title {
        flex: 1;
      }
      
      :deep(.el-radio-group) {
        .el-radio-button__inner {
          background: rgba(33, 150, 243, 0.1);
          border-color: rgba(33, 150, 243, 0.3);
          color: $text-secondary;
        }
        
        .el-radio-button__original-radio:checked + .el-radio-button__inner {
          background: $primary-color;
          border-color: $primary-color;
          color: #FFFFFF;
        }
      }
    }
    
    .retention-summary {
      display: flex;
      justify-content: space-around;
      padding: 12px 0;
      border-top: 1px solid #E0E0E0;
      margin-top: 8px;
      
      .retention-item {
        text-align: center;
        
        .retention-label {
          display: block;
          font-size: 12px;
          color: $text-secondary;
          margin-bottom: 4px;
        }
        
        .retention-value {
          font-size: 20px;
          font-weight: 600;
          color: $primary-color;
        }
      }
    }
  }
}
</style>
