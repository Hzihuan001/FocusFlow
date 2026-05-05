<template>
  <div class="focus-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-icon total">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalRecords || 0 }}</div>
            <div class="stat-label">总专注次数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-icon duration">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatDuration(stats.totalDuration) }}</div>
            <div class="stat-label">总专注时长</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="8">
        <div class="stat-card">
          <div class="stat-icon today">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.todayRecords || 0 }}</div>
            <div class="stat-label">今日专注</div>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <div class="search-row">
          <el-form-item label="用户ID">
            <el-input v-model="searchForm.userId" placeholder="用户ID" clearable style="width: 120px" />
          </el-form-item>
          <el-form-item label="任务名称">
            <el-input v-model="searchForm.taskName" placeholder="模糊搜索" clearable style="width: 150px" />
          </el-form-item>
          <el-form-item label="日期范围">
            <el-date-picker
              v-model="searchForm.dateRange"
              type="daterange"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 240px"
            />
          </el-form-item>
        </div>
        <div class="search-row">
          <el-form-item label="最短时长">
            <el-input-number v-model="searchForm.minDuration" :min="1" :max="999" placeholder="分钟" style="width: 120px" />
          </el-form-item>
          <el-form-item label="最长时长">
            <el-input-number v-model="searchForm.maxDuration" :min="1" :max="999" placeholder="分钟" style="width: 120px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="danger" @click="handleBatchDelete" :disabled="selectedRecords.length === 0">
              <el-icon><Delete /></el-icon>
              批量删除 ({{ selectedRecords.length }})
            </el-button>
          </el-form-item>
        </div>
      </el-form>
    </el-card>
    
    <!-- 专注记录列表 -->
    <el-card class="table-card">
      <el-table :data="focusList" v-loading="loading" class="cyber-table" stripe empty-text="暂无专注记录" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="recordId" label="记录ID" width="280">
          <template #default="{ row }">
            <span class="id-text">{{ row.recordId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="taskName" label="任务名称" width="150">
          <template #default="{ row }">
            {{ row.taskName || '专注' }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMinutes" label="时长(分钟)" width="120">
          <template #default="{ row }">
            <el-tag type="success" size="small">{{ row.durationMinutes }}分钟</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="syncTime" label="同步时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.syncTime) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchFocusList"
          @current-change="fetchFocusList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import { formatTime, formatDuration } from '@/utils/format'
import dayjs from 'dayjs'

const loading = ref(false)
const focusList = ref([])
const selectedRecords = ref([])

const stats = ref({
  totalRecords: 0,
  totalDuration: 0,
  todayRecords: 0
})

const searchForm = reactive({
  userId: '',
  taskName: '',
  dateRange: null,
  minDuration: null,
  maxDuration: null
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const fetchStats = async () => {
  try {
    const res = await request.get('/admin/focus/stats')
    stats.value = res || {}
  } catch (e) {
    console.error('获取专注统计失败', e)
  }
}

const fetchFocusList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    
    if (searchForm.userId) {
      params.userId = searchForm.userId
    }
    
    if (searchForm.taskName) {
      params.taskName = searchForm.taskName
    }
    
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = dayjs(searchForm.dateRange[0]).startOf('day').valueOf()
      params.endDate = dayjs(searchForm.dateRange[1]).endOf('day').valueOf()
    }
    
    if (searchForm.minDuration) {
      params.minDuration = searchForm.minDuration
    }
    
    if (searchForm.maxDuration) {
      params.maxDuration = searchForm.maxDuration
    }
    
    const res = await request.get('/admin/focus/records', { params })
    focusList.value = res.list || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('获取专注记录失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchFocusList()
}

const handleReset = () => {
  searchForm.userId = ''
  searchForm.taskName = ''
  searchForm.dateRange = null
  searchForm.minDuration = null
  searchForm.maxDuration = null
  pagination.page = 1
  fetchFocusList()
}

// 批量删除
const handleSelectionChange = (selection) => {
  selectedRecords.value = selection
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该专注记录吗？', '提示', { type: 'warning' })
    await request.delete(`/admin/focus/${row.recordId}`)
    ElMessage.success('删除成功')
    fetchStats()
    fetchFocusList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  const count = selectedRecords.value.length
  if (count === 0) return
  
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${count} 条专注记录吗？`, '批量删除', { type: 'warning' })
    
    const recordIds = selectedRecords.value.map(r => r.recordId)
    const res = await request.delete('/admin/focus/batch', { data: { recordIds } })
    ElMessage.success(`成功删除 ${res.successCount || count} 条记录`)
    
    selectedRecords.value = []
    fetchStats()
    fetchFocusList()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量删除失败', e)
      ElMessage.error('批量删除失败')
    }
  }
}

onMounted(() => {
  fetchStats()
  fetchFocusList()
})
</script>

<style lang="scss" scoped>
@import '@/styles/index.scss';

.focus-page {
  .stat-row {
    margin-bottom: 20px;
  }
  
  .stat-card {
    display: flex;
    align-items: center;
    gap: 15px;
    
    .stat-icon {
      width: 50px;
      height: 50px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      
      &.total { background: rgba(76, 175, 80, 0.15); color: #4CAF50; }
      &.duration { background: rgba(33, 150, 243, 0.15); color: #2196f3; }
      &.today { background: rgba(255, 152, 0, 0.15); color: #FF9800; }
    }
    
    .stat-info {
      .stat-value {
        font-size: 24px;
        font-weight: 600;
        color: $primary-color;
      }
      
      .stat-label {
        font-size: 14px;
        color: $text-secondary;
        margin-top: 4px;
      }
    }
  }
  
  .search-card {
    margin-bottom: 20px;
    
    .search-form {
      .search-row {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 8px;
        
        &:last-child {
          margin-bottom: 0;
        }
      }
    }
    
    :deep(.el-form-item) {
      margin-bottom: 0;
      margin-right: 16px;
      
      &:last-child {
        margin-right: 0;
      }
    }
  }
  
  .table-card {
    // Uses global card styles
  }
  
  .id-text {
    font-size: 12px;
    color: $text-secondary;
    font-family: monospace;
  }
  
  .flux-value {
    color: $primary-color;
    font-weight: 600;
  }
  
  .text-muted {
    color: $text-secondary;
  }
  
  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
