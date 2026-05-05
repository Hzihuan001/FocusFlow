<template>
  <div class="config-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2>系统配置</h2>
        <p class="subtitle">管理应用运行参数，AI相关配置请前往「AI配置」菜单</p>
      </div>
      <div class="header-actions">
        <el-button @click="fetchConfigList" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增配置
        </el-button>
        <el-button 
          type="success" 
          @click="handleSaveAll" 
          :loading="saving" 
          :disabled="changedItems.size === 0"
        >
          <el-icon><Check /></el-icon>
          保存更改 ({{ changedItems.size }})
        </el-button>
      </div>
    </div>
    
    <!-- 配置分组卡片 -->
    <div class="config-groups" v-loading="loading">
      <el-row :gutter="20">
        <el-col 
          :xs="24" 
          :sm="12" 
          :lg="8" 
          v-for="group in groupedConfigList" 
          :key="group.name"
        >
          <el-card class="group-card" :body-style="{ padding: 0 }">
            <template #header>
              <div class="group-header">
                <div class="group-icon" :style="{ background: getGroupColor(group.name) }">
                  <el-icon><component :is="getGroupIcon(group.name)" /></el-icon>
                </div>
                <div class="group-info">
                  <span class="group-name">{{ group.name }}</span>
                  <span class="group-count">{{ group.items.length }} 项配置</span>
                </div>
              </div>
            </template>
            
            <div class="config-list">
              <div 
                v-for="item in group.items" 
                :key="item.configKey" 
                class="config-item"
                :class="{ changed: changedItems.has(item.configKey) }"
              >
                <div class="config-label">
                  <span class="config-name">{{ item.description || item.configKey }}</span>
                  <span class="config-key">{{ item.configKey }}</span>
                </div>
                <div class="config-input">
                  <el-input 
                    v-model="item.configValue" 
                    size="small"
                    placeholder="输入值"
                    @change="markChanged(item)"
                  />
                </div>
                <el-button 
                  size="small" 
                  type="danger" 
                  text 
                  class="delete-btn"
                  @click="handleDelete(item)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 空状态 -->
      <el-empty v-if="!loading && groupedConfigList.length === 0" description="暂无配置项" />
    </div>
    
    <!-- 新增配置对话框 -->
    <el-dialog v-model="dialogVisible" title="新增配置" width="500px" class="config-dialog">
      <el-form :model="configForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="configForm.configKey" placeholder="如: focus.default_duration" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="configForm.configValue" placeholder="配置值" />
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="configForm.configGroup" placeholder="选择分组" allow-create filterable style="width: 100%">
            <el-option label="连续专注奖励" value="连续专注奖励" />
            <el-option label="花园净化" value="花园净化" />
            <el-option label="专注奖励" value="专注奖励" />
            <el-option label="充能系统" value="充能系统" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="configForm.description" type="textarea" :rows="2" placeholder="配置说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Setting, Trophy, Sunrise, Timer, Lightning, More,
  Plus, Check, Refresh, Delete
} from '@element-plus/icons-vue'
import { getConfigList, batchUpdateConfig, addConfig, deleteConfig } from '@/api/config'

const loading = ref(false)
const submitting = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const configList = ref([])
const changedItems = ref(new Set())
const formRef = ref(null)

const configForm = reactive({
  configKey: '',
  configValue: '',
  configGroup: '',
  description: ''
})

const rules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

// 分组颜色映射
const groupColors = {
  '连续专注奖励': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  '专注奖励': 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  '花园净化': 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  '充能系统': 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  '其他': 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)'
}

// 分组图标映射
const groupIcons = {
  '连续专注奖励': Trophy,
  '专注奖励': Timer,
  '花园净化': Sunrise,
  '充能系统': Lightning,
  '其他': More
}

const getGroupColor = (name) => groupColors[name] || groupColors['其他']
const getGroupIcon = (name) => groupIcons[name] || More

// 过滤掉AI相关配置，按分组整理
const groupedConfigList = computed(() => {
  // 过滤掉AI配置（configGroup为'ai'的配置在AI配置页面管理）
  const filteredList = configList.value.filter(item => item.configGroup !== 'ai')
  
  // 按分组整理
  const groups = {}
  filteredList.forEach(item => {
    const groupName = item.configGroup || '其他'
    if (!groups[groupName]) {
      groups[groupName] = []
    }
    groups[groupName].push(item)
  })
  
  // 转换为数组并排序
  const groupOrder = ['连续专注奖励', '专注奖励', '花园净化', '充能系统', '其他']
  return Object.entries(groups)
    .map(([name, items]) => ({ name, items }))
    .sort((a, b) => {
      const indexA = groupOrder.indexOf(a.name)
      const indexB = groupOrder.indexOf(b.name)
      return (indexA === -1 ? 999 : indexA) - (indexB === -1 ? 999 : indexB)
    })
})

const resetForm = () => {
  configForm.configKey = ''
  configForm.configValue = ''
  configForm.configGroup = ''
  configForm.description = ''
}

const fetchConfigList = async () => {
  loading.value = true
  try {
    configList.value = await getConfigList()
    changedItems.value.clear()
  } catch (e) {
    console.error('获取配置失败', e)
    ElMessage.error('获取配置列表失败')
  } finally {
    loading.value = false
  }
}

const markChanged = (item) => {
  changedItems.value.add(item.configKey)
}

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  
  submitting.value = true
  try {
    await addConfig(configForm)
    ElMessage.success('配置新增成功')
    dialogVisible.value = false
    fetchConfigList()
  } catch (e) {
    console.error('新增失败', e)
    ElMessage.error('新增配置失败')
  } finally {
    submitting.value = false
  }
}

const handleSaveAll = async () => {
  if (changedItems.value.size === 0) {
    ElMessage.info('没有需要保存的更改')
    return
  }
  
  saving.value = true
  try {
    const updates = configList.value
      .filter(item => changedItems.value.has(item.configKey))
      .map(item => ({
        configKey: item.configKey,
        configValue: item.configValue
      }))
    
    await batchUpdateConfig(updates)
    ElMessage.success(`成功保存 ${updates.length} 项配置`)
    changedItems.value.clear()
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存配置失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除配置「${item.configKey}」吗？`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteConfig(item.configId)
    ElMessage.success('删除成功')
    fetchConfigList()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
      ElMessage.error('删除配置失败')
    }
  }
}

onMounted(() => {
  fetchConfigList()
})
</script>

<style lang="scss" scoped>
@import '@/styles/index.scss';

.config-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;
    
    @media (max-width: 768px) {
      flex-direction: column;
      gap: 16px;
    }
    
    .header-left {
      h2 {
        color: $text-primary;
        font-size: 22px;
        margin: 0 0 8px 0;
      }
      
      .subtitle {
        color: $text-secondary;
        font-size: 14px;
        margin: 0;
      }
    }
    
    .header-actions {
      display: flex;
      gap: 12px;
      
      @media (max-width: 768px) {
        width: 100%;
        flex-wrap: wrap;
        
        .el-button {
          flex: 1;
        }
      }
    }
  }
  
  .group-card {
    background: $bg-card;
    border: 1px solid $border-color;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    
    :deep(.el-card__header) {
      border-bottom: 1px solid #E0E0E0;
      padding: 16px 20px;
    }
    
    .group-header {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .group-icon {
        width: 42px;
        height: 42px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        .el-icon {
          font-size: 20px;
          color: #fff;
        }
      }
      
      .group-info {
        display: flex;
        flex-direction: column;
        
        .group-name {
          color: $text-primary;
          font-size: 15px;
          font-weight: 500;
        }
        
        .group-count {
          color: $text-secondary;
          font-size: 12px;
        }
      }
    }
    
    .config-list {
      .config-item {
        display: flex;
        align-items: center;
        padding: 12px 20px;
        border-bottom: 1px solid #F5F5F5;
        transition: all 0.2s;
        
        &:last-child {
          border-bottom: none;
        }
        
        &:hover {
          background: #F5F9FD;
          
          .delete-btn {
            opacity: 1;
          }
        }
        
        &.changed {
          background: #E3F2FD;
          
          .config-name {
            color: $primary-color;
          }
          
          :deep(.el-input__wrapper) {
            box-shadow: 0 0 0 1px $primary-color inset;
          }
        }
        
        .config-label {
          flex: 1;
          min-width: 0;
          
          .config-name {
            display: block;
            color: $text-primary;
            font-size: 14px;
            margin-bottom: 2px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
          
          .config-key {
            display: block;
            color: $text-secondary;
            font-size: 11px;
            font-family: 'Consolas', monospace;
          }
        }
        
        .config-input {
          width: 100px;
          margin: 0 12px;
          
          :deep(.el-input__inner) {
            text-align: center;
          }
        }
        
        .delete-btn {
          opacity: 0;
          transition: opacity 0.2s;
          padding: 4px;
        }
      }
    }
  }
}
</style>
