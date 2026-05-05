<template>
  <div class="bag-page">
    <!-- 操作栏 -->
    <el-card class="action-card">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-button type="primary" @click="handleBatchAdd">
            <el-icon><Present /></el-icon>
            批量发放植物
          </el-button>
          <el-button type="success" @click="handleAddFlux">
            <el-icon><Coin /></el-icon>
            发放光流
          </el-button>
          <el-button type="danger" @click="handleBatchDelete" :disabled="selectedItems.length === 0">
            <el-icon><Delete /></el-icon>
            批量删除 ({{ selectedItems.length }})
          </el-button>
          <el-button @click="fetchBagList">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-col>
        <el-col :span="8" style="text-align: right">
          <el-tag>总物品: {{ stats.totalItems || 0 }}</el-tag>
          <el-tag type="success" style="margin-left: 8px">种子: {{ stats.seedCount || 0 }}</el-tag>
          <el-tag type="warning" style="margin-left: 8px">植物: {{ stats.plantCount || 0 }}</el-tag>
        </el-col>
      </el-row>
    </el-card>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="输入用户ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="种子" :value="0" />
            <el-option label="植物" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 背包列表 -->
    <el-card class="table-card">
      <el-table :data="bagList" v-loading="loading" class="cyber-table" stripe empty-text="暂无背包数据" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="bagId" label="背包ID" width="280">
          <template #default="{ row }">
            <span class="id-text">{{ row.bagId?.substring(0, 8) }}...</span>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="plantName" label="植物" width="150">
          <template #default="{ row }">
            <div class="plant-cell">
              <el-image 
                v-if="row.imageUrl"
                :src="row.imageUrl" 
                fit="cover"
                lazy
                class="plant-thumb"
              />
              <span :style="{ color: getRarityColor(row.rarity) }">{{ row.plantName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="rarity" label="稀有度" width="100">
          <template #default="{ row }">
            <el-tag :type="getRarityType(row.rarity)" size="small">
              {{ getRarityName(row.rarity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '植物' : '种子' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="obtainedAt" label="获取时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.obtainedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchBagList"
        @current-change="fetchBagList"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 批量发放对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量发放植物" width="500px">
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="用户列表">
          <el-select 
            v-model="batchForm.userIds" 
            multiple 
            filterable
            placeholder="选择用户"
            style="width: 100%"
          >
            <el-option 
              v-for="user in userList" 
              :key="user.userId" 
              :label="`${user.nickname} (${user.userId})`"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择植物">
          <el-select v-model="batchForm.plantId" placeholder="选择植物" style="width: 100%">
            <el-option 
              v-for="plant in plantList" 
              :key="plant.plantId" 
              :label="plant.plantName"
              :value="plant.plantId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="batchForm.count" :min="1" :max="99" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchAdd" :loading="submitting">确定发放</el-button>
      </template>
    </el-dialog>

    <!-- 发放光流对话框 -->
    <el-dialog v-model="fluxDialogVisible" title="发放光流" width="500px">
      <el-form :model="fluxForm" label-width="80px">
        <el-form-item label="用户列表">
          <el-select 
            v-model="fluxForm.userIds" 
            multiple 
            filterable
            placeholder="选择用户"
            style="width: 100%"
          >
            <el-option 
              v-for="user in userList" 
              :key="user.userId" 
              :label="`${user.nickname} (光流: ${user.timeFlux || 0})`"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="光流数量">
          <el-input-number v-model="fluxForm.amount" :min="-9999" :max="9999" :step="10" style="width: 100%" />
          <div class="field-tip">正数为发放，负数为扣除</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fluxDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddFlux" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Present, Coin, Delete, Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'
import { formatTime, getRarityName, getRarityType, getRarityColor } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const bagList = ref([])
const userList = ref([])
const plantList = ref([])
const batchDialogVisible = ref(false)
const fluxDialogVisible = ref(false)
const selectedItems = ref([])

const stats = ref({
  totalItems: 0,
  seedCount: 0,
  plantCount: 0
})

const searchForm = reactive({
  userId: '',
  status: null
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const batchForm = reactive({
  userIds: [],
  plantId: null,
  count: 1
})

const fluxForm = reactive({
  userIds: [],
  amount: 100
})

const fetchStats = async () => {
  try {
    const data = await request.get('/admin/bag/stats')
    stats.value = data || {}
  } catch (e) {
    console.error('获取统计失败', e)
  }
}

const fetchBagList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (searchForm.userId) params.userId = searchForm.userId
    if (searchForm.status !== null) params.status = searchForm.status
    
    const data = await request.get('/admin/bag/list', { params })
    bagList.value = data.list || []
    pagination.total = data.total || 0
  } catch (e) {
    console.error('获取背包列表失败', e)
  } finally {
    loading.value = false
  }
}

const fetchUserList = async () => {
  try {
    const data = await request.get('/admin/user', { params: { size: 100 } })
    userList.value = data.list || []
  } catch (e) {
    console.error('获取用户列表失败', e)
  }
}

const fetchPlantList = async () => {
  try {
    const data = await request.get('/admin/plant')
    plantList.value = data || []
  } catch (e) {
    console.error('获取植物列表失败', e)
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchBagList()
}

const handleReset = () => {
  searchForm.userId = ''
  searchForm.status = null
  pagination.page = 1
  fetchBagList()
}

const handleBatchAdd = () => {
  batchForm.userIds = []
  batchForm.plantId = null
  batchForm.count = 1
  batchDialogVisible.value = true
}

const handleAddFlux = () => {
  fluxForm.userIds = []
  fluxForm.amount = 100
  fluxDialogVisible.value = true
}

const submitBatchAdd = async () => {
  if (batchForm.userIds.length === 0) {
    ElMessage.warning('请选择用户')
    return
  }
  if (!batchForm.plantId) {
    ElMessage.warning('请选择植物')
    return
  }
  
  submitting.value = true
  try {
    await request.post('/admin/bag/batch', {
      userIds: batchForm.userIds,
      plantId: batchForm.plantId,
      count: batchForm.count
    })
    ElMessage.success('批量发放成功')
    batchDialogVisible.value = false
    fetchStats()
    fetchBagList()
  } catch (e) {
    ElMessage.error('发放失败')
  } finally {
    submitting.value = false
  }
}

const submitAddFlux = async () => {
  if (fluxForm.userIds.length === 0) {
    ElMessage.warning('请选择用户')
    return
  }
  if (!fluxForm.amount || fluxForm.amount === 0) {
    ElMessage.warning('请输入有效的光流数量')
    return
  }
  
  const action = fluxForm.amount > 0 ? '发放' : '扣除'
  const amount = Math.abs(fluxForm.amount)
  
  try {
    await ElMessageBox.confirm(
      `确定${action} ${fluxForm.userIds.length} 位用户 ${amount} 光流吗？`,
      '确认操作',
      { type: 'warning' }
    )
  } catch {
    return
  }
  
  submitting.value = true
  try {
    const res = await request.post('/admin/bag/flux', {
      userIds: fluxForm.userIds,
      amount: fluxForm.amount
    })
    ElMessage.success(res.msg || `${action}成功`)
    fluxDialogVisible.value = false
    fetchUserList()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该物品吗？', '提示', { type: 'warning' })
    await request.delete(`/admin/bag/${row.bagId}`)
    ElMessage.success('删除成功')
    fetchStats()
    fetchBagList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 批量删除
const handleSelectionChange = (selection) => {
  selectedItems.value = selection
}

const handleBatchDelete = async () => {
  const count = selectedItems.value.length
  if (count === 0) return
  
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${count} 个物品吗？`, '批量删除', { type: 'warning' })
    
    const bagIds = selectedItems.value.map(item => item.bagId)
    const res = await request.delete('/admin/bag/batch', { data: { bagIds } })
    ElMessage.success(`成功删除 ${res.successCount || count} 个物品`)
    
    selectedItems.value = []
    fetchStats()
    fetchBagList()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量删除失败', e)
      ElMessage.error('批量删除失败')
    }
  }
}

onMounted(() => {
  fetchStats()
  fetchBagList()
  fetchUserList()
  fetchPlantList()
})
</script>

<style lang="scss" scoped>
.bag-page {
  .action-card, .filter-card, .table-card {
    margin-bottom: 16px;
  }
  
  .id-text {
    font-family: monospace;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
  
  .plant-cell {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .plant-thumb {
      width: 32px;
      height: 32px;
      border-radius: 4px;
    }
  }
  
  .field-tip {
    color: var(--el-text-color-placeholder);
    font-size: 11px;
    margin-top: 4px;
  }
  
  :deep(.el-tag) {
    border: none;
  }
}
</style>
