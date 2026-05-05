<template>
  <div class="user-page">
    <!-- 操作栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input 
            v-model="searchForm.keyword" 
            placeholder="搜索账号/昵称" 
            clearable 
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 批量操作栏 -->
      <div class="batch-actions" v-if="selectedUsers.length > 0">
        <span class="selected-count">已选择 {{ selectedUsers.length }} 个用户</span>
        <el-button size="small" type="danger" @click="handleBatchBan">批量封禁</el-button>
        <el-button size="small" type="success" @click="handleBatchUnban">批量解封</el-button>
        <el-button size="small" type="warning" @click="showBatchFluxDialog">批量修改光流</el-button>
        <el-button size="small" @click="clearSelection">取消选择</el-button>
      </div>
    </el-card>
    
    <!-- 用户列表 -->
    <el-card class="table-card">
      <el-table 
        :data="userList" 
        v-loading="loading" 
        class="cyber-table"
        stripe
        empty-text="暂无用户数据"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="account" label="账号" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120">
          <template #default="{ row }">
            <span>{{ row.nickname || '专注者' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="avatarId" label="头像" width="80">
          <template #default="{ row }">
            <el-avatar :size="40" :src="`/api/avatar/${row.avatarId}.png`">
              {{ row.nickname?.charAt(0) || 'F' }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="timeFlux" label="光流余额" width="120">
          <template #default="{ row }">
            <span class="flux-value">{{ row.timeFlux || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="streakDays" label="连续天数" width="100">
          <template #default="{ row }">
            <el-tag type="warning" size="small">{{ row.streakDays || 0 }}天</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="320">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleBag(row)">背包</el-button>
            <el-button 
              v-if="row.status === 0"
              size="small" 
              type="danger" 
              @click="handleBan(row)"
            >封禁</el-button>
            <el-button 
              v-else
              size="small" 
              type="success" 
              @click="handleUnban(row)"
            >解封</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchUserList"
          @current-change="fetchUserList"
        />
      </div>
    </el-card>
    
    <!-- 编辑用户对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑用户信息" width="450px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="80px">
        <el-form-item label="用户ID">
          <span>{{ editForm.userId }}</span>
        </el-form-item>
        <el-form-item label="账号" prop="account">
          <el-input v-model="editForm.account" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input 
            v-model="editForm.password" 
            type="password" 
            placeholder="留空则不修改密码" 
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 新增用户对话框 -->
    <el-dialog v-model="addDialogVisible" title="新增用户" width="450px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="80px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="addForm.account" placeholder="请输入账号（3-20字符）" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="addForm.nickname" placeholder="请输入昵称（可选）" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="addForm.password" 
            type="password" 
            placeholder="请输入密码（至少6字符）" 
            show-password
          />
        </el-form-item>
        <el-form-item label="初始光流">
          <el-input-number v-model="addForm.timeFlux" :min="0" :max="10000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 调整光流对话框 -->
    <el-dialog v-model="fluxDialogVisible" title="调整用户光流" width="400px">
      <el-form :model="fluxForm" label-width="80px">
        <el-form-item label="用户">
          <span>{{ currentUser?.nickname || currentUser?.account }}</span>
        </el-form-item>
        <el-form-item label="当前光流">
          <span>{{ currentUser?.timeFlux || 0 }}</span>
        </el-form-item>
        <el-form-item label="新光流">
          <el-input-number v-model="fluxForm.timeFlux" :min="0" :max="999999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fluxDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFlux" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 用户背包对话框 -->
    <el-dialog v-model="bagDialogVisible" :title="`${currentBagUser?.nickname || '用户'}的背包`" width="750px">
      <div class="bag-dialog-content">
        <div class="bag-header">
          <el-button type="primary" size="small" @click="handleAddItem">
            <el-icon><Plus /></el-icon>
            添加道具
          </el-button>
          <el-button type="danger" size="small" @click="handleBatchDeleteBagItems" :disabled="selectedBagItems.length === 0">
            <el-icon><Delete /></el-icon>
            批量删除 ({{ selectedBagItems.length }})
          </el-button>
          <el-button size="small" @click="fetchUserBag">刷新</el-button>
        </div>
        
        <el-table :data="userBagList" v-loading="bagLoading" size="small" max-height="400" empty-text="背包空空如也" @selection-change="handleBagSelectionChange">
          <el-table-column type="selection" width="40" />
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
          <el-table-column prop="rarity" label="稀有度" width="80">
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
          <el-table-column prop="obtainedAt" label="获取时间">
            <template #default="{ row }">
              {{ formatTime(row.obtainedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button size="small" type="danger" text @click="handleDeleteBagItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div v-if="userBagList.length === 0 && !bagLoading" class="empty-bag">
          背包空空如也
        </div>
      </div>
    </el-dialog>
    
    <!-- 添加道具对话框 -->
    <el-dialog v-model="addItemDialogVisible" title="添加道具" width="400px">
      <el-form :model="addItemForm" label-width="80px">
        <el-form-item label="道具类型">
          <el-select v-model="addItemForm.plantId" placeholder="选择道具类型" style="width: 100%">
            <el-option label="盲盒种子（需解析）" :value="0" />
            <el-option 
              v-for="plant in plantList" 
              :key="plant.plantId" 
              :label="plant.plantName"
              :value="plant.plantId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="addItemForm.count" :min="1" :max="99" />
        </el-form-item>
        <el-form-item v-if="addItemForm.plantId === 0">
          <el-text type="info" size="small">盲盒种子需要用户消耗光流解析，随机获得植物</el-text>
        </el-form-item>
        <el-form-item v-else-if="addItemForm.plantId">
          <el-text type="success" size="small">指定植物可直接使用，无需解析</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addItemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddItem" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 批量修改光流对话框 -->
    <el-dialog v-model="batchFluxDialogVisible" title="批量修改光流" width="450px">
      <div class="batch-flux-info">
        <p>已选择 <strong>{{ selectedUsers.length }}</strong> 个用户</p>
      </div>
      <el-form :model="batchFluxForm" label-width="100px">
        <el-form-item label="操作方式">
          <el-radio-group v-model="batchFluxForm.mode">
            <el-radio value="set">设置为</el-radio>
            <el-radio value="add">增加</el-radio>
            <el-radio value="subtract">减少</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="光流数量">
          <el-input-number v-model="batchFluxForm.timeFlux" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item v-if="batchFluxForm.mode === 'set'">
          <el-text type="info" size="small">将选中用户的光流设置为指定值</el-text>
        </el-form-item>
        <el-form-item v-else-if="batchFluxForm.mode === 'add'">
          <el-text type="success" size="small">为选中用户增加指定数量的光流</el-text>
        </el-form-item>
        <el-form-item v-else>
          <el-text type="warning" size="small">扣除选中用户的指定光流（不会为负）</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchFluxDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchFlux" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, updateTimeFlux, banUser, unbanUser, updateUserInfo, addUser } from '@/api/user'
import request from '@/api/request'
import { formatTime, getRarityName, getRarityType, getRarityColor } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const userList = ref([])
const fluxDialogVisible = ref(false)
const editDialogVisible = ref(false)
const addDialogVisible = ref(false)
const currentUser = ref(null)
const editFormRef = ref(null)
const addFormRef = ref(null)

// 批量操作相关
const selectedUsers = ref([])
const batchFluxDialogVisible = ref(false)
const batchFluxForm = reactive({
  mode: 'add',
  timeFlux: 100
})

// 背包相关
const bagDialogVisible = ref(false)
const bagLoading = ref(false)
const addItemDialogVisible = ref(false)
const currentBagUser = ref(null)
const userBagList = ref([])
const plantList = ref([])
const selectedBagItems = ref([])

const addItemForm = reactive({
  plantId: null,
  count: 1
})

const searchForm = reactive({
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const fluxForm = reactive({
  timeFlux: 0
})

const editForm = reactive({
  userId: null,
  account: '',
  nickname: '',
  password: ''
})

const editRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度3-20个字符', trigger: 'blur' }
  ],
  nickname: [
    { max: 20, message: '昵称最多20个字符', trigger: 'blur' }
  ],
  password: [
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ]
}

const addForm = reactive({
  account: '',
  nickname: '',
  password: '',
  timeFlux: 0
})

const addRules = {
  account: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  nickname: [
    { max: 20, message: '昵称最多20个字符', trigger: 'blur' }
  ]
}

const fetchUserList = async () => {
  loading.value = true
  try {
    const res = await getUserList({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword || undefined
    })
    userList.value = res.list || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('获取用户列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchUserList()
}

const handleReset = () => {
  searchForm.keyword = ''
  pagination.page = 1
  fetchUserList()
}

const handleEditFlux = (row) => {
  currentUser.value = row
  fluxForm.timeFlux = row.timeFlux || 0
  fluxDialogVisible.value = true
}

const handleEdit = (row) => {
  currentUser.value = row
  editForm.userId = row.userId
  editForm.account = row.account || ''
  editForm.nickname = row.nickname || ''
  editForm.password = ''
  editDialogVisible.value = true
}

const handleAdd = () => {
  addForm.account = ''
  addForm.nickname = ''
  addForm.password = ''
  addForm.timeFlux = 0
  addDialogVisible.value = true
}

const submitAdd = async () => {
  try {
    await addFormRef.value.validate()
  } catch {
    return
  }
  
  submitting.value = true
  try {
    await addUser({
      account: addForm.account,
      nickname: addForm.nickname || addForm.account,
      password: addForm.password,
      timeFlux: addForm.timeFlux
    })
    ElMessage.success('用户创建成功')
    addDialogVisible.value = false
    fetchUserList()
  } catch (e) {
    console.error('创建用户失败', e)
  } finally {
    submitting.value = false
  }
}

const submitEdit = async () => {
  try {
    await editFormRef.value.validate()
  } catch {
    return
  }
  
  submitting.value = true
  try {
    const data = {
      account: editForm.account,
      nickname: editForm.nickname
    }
    // 只有输入了密码才传递
    if (editForm.password) {
      data.password = editForm.password
    }
    
    await updateUserInfo(currentUser.value.userId, data)
    ElMessage.success('用户信息更新成功')
    
    // 更新本地数据
    currentUser.value.account = editForm.account
    currentUser.value.nickname = editForm.nickname
    
    editDialogVisible.value = false
  } catch (e) {
    console.error('更新用户信息失败', e)
  } finally {
    submitting.value = false
  }
}

const submitFlux = async () => {
  submitting.value = true
  try {
    await updateTimeFlux(currentUser.value.userId, fluxForm.timeFlux)
    ElMessage.success('光流调整成功')
    currentUser.value.timeFlux = fluxForm.timeFlux
    fluxDialogVisible.value = false
  } catch (e) {
    console.error('调整光流失败', e)
  } finally {
    submitting.value = false
  }
}

const handleBan = async (row) => {
  try {
    await ElMessageBox.confirm('确定要封禁该用户吗？', '提示', {
      type: 'warning'
    })
    await banUser(row.userId)
    ElMessage.success('用户已封禁')
    row.status = 1
  } catch (e) {
    if (e !== 'cancel') {
      console.error('封禁失败', e)
    }
  }
}

const handleUnban = async (row) => {
  try {
    await unbanUser(row.userId)
    ElMessage.success('用户已解封')
    row.status = 0
  } catch (e) {
    console.error('解封失败', e)
  }
}

// ========== 批量操作 ==========

const handleSelectionChange = (selection) => {
  selectedUsers.value = selection
}

const clearSelection = () => {
  selectedUsers.value = []
}

const handleBatchBan = async () => {
  const count = selectedUsers.value.length
  try {
    await ElMessageBox.confirm(`确定要封禁选中的 ${count} 个用户吗？`, '批量封禁', {
      type: 'warning'
    })
    
    const userIds = selectedUsers.value.map(u => u.userId)
    const res = await request.put('/admin/user/batch/ban', { userIds })
    ElMessage.success(`成功封禁 ${res.successCount || count} 个用户`)
    
    // 更新本地状态
    selectedUsers.value.forEach(u => {
      u.status = 1
    })
    clearSelection()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量封禁失败', e)
      ElMessage.error('批量封禁失败')
    }
  }
}

const handleBatchUnban = async () => {
  const count = selectedUsers.value.length
  try {
    await ElMessageBox.confirm(`确定要解封选中的 ${count} 个用户吗？`, '批量解封', {
      type: 'warning'
    })
    
    const userIds = selectedUsers.value.map(u => u.userId)
    const res = await request.put('/admin/user/batch/unban', { userIds })
    ElMessage.success(`成功解封 ${res.successCount || count} 个用户`)
    
    // 更新本地状态
    selectedUsers.value.forEach(u => {
      u.status = 0
    })
    clearSelection()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量解封失败', e)
      ElMessage.error('批量解封失败')
    }
  }
}

const showBatchFluxDialog = () => {
  batchFluxForm.mode = 'add'
  batchFluxForm.timeFlux = 100
  batchFluxDialogVisible.value = true
}

const submitBatchFlux = async () => {
  const count = selectedUsers.value.length
  
  submitting.value = true
  try {
    const userIds = selectedUsers.value.map(u => u.userId)
    const res = await request.put('/admin/user/batch/flux', {
      userIds,
      mode: batchFluxForm.mode,
      timeFlux: batchFluxForm.timeFlux
    })
    
    ElMessage.success(`成功修改 ${res.successCount || count} 个用户的光流`)
    
    // 刷新列表以获取最新数据
    fetchUserList()
    clearSelection()
    batchFluxDialogVisible.value = false
  } catch (e) {
    console.error('批量修改光流失败', e)
    ElMessage.error('批量修改光流失败')
  } finally {
    submitting.value = false
  }
}

// ========== 背包管理 ==========

const handleBag = async (row) => {
  currentBagUser.value = row
  bagDialogVisible.value = true
  await fetchUserBag()
  await fetchPlantList()
}

const fetchUserBag = async () => {
  if (!currentBagUser.value) return
  
  bagLoading.value = true
  try {
    const data = await request.get(`/admin/bag/user/${currentBagUser.value.userId}`)
    userBagList.value = data || []
  } catch (e) {
    console.error('获取背包失败', e)
    userBagList.value = []
  } finally {
    bagLoading.value = false
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

const handleAddItem = () => {
  addItemForm.plantId = null
  addItemForm.count = 1
  addItemDialogVisible.value = true
}

const submitAddItem = async () => {
  if (addItemForm.plantId === null || addItemForm.plantId === undefined || addItemForm.plantId === '') {
    ElMessage.warning('请选择道具类型')
    return
  }
  
  submitting.value = true
  try {
    await request.post('/admin/bag/add', {
      userId: currentBagUser.value.userId,
      plantId: addItemForm.plantId,
      count: addItemForm.count
    })
    ElMessage.success('添加成功')
    addItemDialogVisible.value = false
    fetchUserBag()
  } catch (e) {
    ElMessage.error('添加失败')
  } finally {
    submitting.value = false
  }
}

const handleDeleteBagItem = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该物品吗？', '提示', { type: 'warning' })
    await request.delete(`/admin/bag/${row.bagId}`)
    ElMessage.success('删除成功')
    fetchUserBag()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 背包批量删除
const handleBagSelectionChange = (selection) => {
  selectedBagItems.value = selection
}

const handleBatchDeleteBagItems = async () => {
  const count = selectedBagItems.value.length
  if (count === 0) return
  
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${count} 个物品吗？`, '批量删除', { type: 'warning' })
    
    const bagIds = selectedBagItems.value.map(item => item.bagId)
    const res = await request.delete('/admin/bag/batch', { data: { bagIds } })
    ElMessage.success(`成功删除 ${res.successCount || count} 个物品`)
    
    selectedBagItems.value = []
    fetchUserBag()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量删除失败', e)
      ElMessage.error('批量删除失败')
    }
  }
}

onMounted(() => {
  fetchUserList()
})
</script>

<style lang="scss" scoped>
.user-page {
  .search-card {
    margin-bottom: 20px;
  }
  
  .batch-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 16px;
    padding: 12px 16px;
    background: rgba(33, 150, 243, 0.1);
    border: 1px solid rgba(33, 150, 243, 0.3);
    border-radius: 6px;
    
    .selected-count {
      color: #2196F3;
      font-weight: 500;
      margin-right: auto;
    }
  }
  
  .flux-value {
    color: #2196F3;
    font-weight: 600;
  }
  
  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

.bag-dialog-content {
  .bag-header {
    margin-bottom: 16px;
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
  
  .empty-bag {
    text-align: center;
    color: #757575;
    padding: 40px 0;
  }
}

.batch-flux-info {
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(33, 150, 243, 0.1);
  border-radius: 6px;
  
  strong {
    color: #2196F3;
  }
}
</style>
