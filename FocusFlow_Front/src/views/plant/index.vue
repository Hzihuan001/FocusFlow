<template>
  <div class="plant-page">
    <!-- 操作栏 -->
    <el-card class="action-card">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增植物
      </el-button>
      <el-button @click="fetchPlantList">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </el-card>
    
    <!-- 植物列表 -->
    <el-card class="table-card">
      <el-table :data="plantList" v-loading="loading" class="cyber-table" stripe empty-text="暂无植物数据">
        <el-table-column prop="plantId" label="ID" width="80" />
        <el-table-column prop="imageUrl" label="图片" width="100">
          <template #default="{ row }">
            <el-image 
              v-if="row.imageUrl"
              :src="row.imageUrl" 
              :preview-src-list="[row.imageUrl]"
              fit="cover"
              lazy
              class="plant-image"
            />
            <span v-else class="no-image">暂无</span>
          </template>
        </el-table-column>
        <el-table-column prop="plantName" label="植物名称" width="150">
          <template #default="{ row }">
            <span :style="{ color: row.colorHex }">{{ row.plantName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="250">
          <template #default="{ row }">
            <span class="desc-text">{{ row.description }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dropWeight" label="掉落权重" width="100" />
        <el-table-column prop="purifyRange" label="净化范围" width="100">
          <template #default="{ row }">
            <el-tag size="small">Lv.{{ row.purifyRange }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="width" label="占地" width="80" />
        <el-table-column prop="scale" label="缩放" width="80">
          <template #default="{ row }">
            <span>{{ row.scale || 100 }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              v-if="row.status === 1"
              size="small" 
              type="warning" 
              @click="handleStatus(row, 0)"
            >下架</el-button>
            <el-button 
              v-else
              size="small" 
              type="success" 
              @click="handleStatus(row, 1)"
            >上架</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑植物' : '新增植物'" 
      width="600px"
    >
      <el-form :model="plantForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="植物名称" prop="plantName">
          <el-input v-model="plantForm.plantName" placeholder="请输入植物名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资源编码" prop="resourceCode">
              <el-input v-model="plantForm.resourceCode" placeholder="如: plant_pixel_bamboo" />
              <div class="field-tip">上传图片前填写，图片将以此命名</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="颜色" prop="colorHex">
              <el-color-picker v-model="plantForm.colorHex" />
              <div class="field-tip">用于列表中植物名称的显示颜色</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="植物图片">
          <el-upload
            class="plant-uploader"
            :action="uploadUrl"
            :data="{ resourceCode: plantForm.resourceCode || '' }"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :with-credentials="true"
            accept="image/*"
          >
            <!-- 上传中状态 -->
            <div v-if="uploading" class="upload-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>正在上传...</span>
            </div>
            <!-- 已上传图片 -->
            <el-image v-else-if="plantForm.imageUrl" :src="plantForm.imageUrl" class="uploaded-image" />
            <!-- 未上传状态 -->
            <el-icon v-else class="upload-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">点击上传植物图片（文件将以资源编码命名）</div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="plantForm.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入植物描述"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="掉落权重" prop="dropWeight">
              <el-input-number v-model="plantForm.dropWeight" :min="1" :max="1000" :step="1" style="width: 100%" />
              <div class="field-tip">数值越大越容易掉落（1-1000）</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="净化范围" prop="purifyRange">
              <el-select v-model="plantForm.purifyRange" style="width: 100%">
                <el-option label="Lv.1 - 初级" :value="1" />
                <el-option label="Lv.2 - 中级" :value="2" />
                <el-option label="Lv.3 - 高级" :value="3" />
                <el-option label="Lv.4 - 传说" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="占地" prop="width">
              <el-input-number 
                v-model="plantForm.width" 
                :min="1" 
                :max="4" 
                :step="1"
                controls-position="right"
              />
              <div class="field-tip">占地格数（1-4格）</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="缩放(%)" prop="scale">
              <el-input-number 
                v-model="plantForm.scale" 
                :min="10" 
                :max="500" 
                :step="10"
                controls-position="right"
              />
              <div class="field-tip">100%=原大小，50%=缩小一半，200%=放大两倍</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-switch 
                v-model="plantForm.status" 
                :active-value="1" 
                :inactive-value="0"
                active-text="上架"
                inactive-text="下架"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Loading } from '@element-plus/icons-vue'
import { getPlantList, addPlant, updatePlant, deletePlant, updatePlantStatus } from '@/api/plant'

const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const plantList = ref([])
const formRef = ref(null)

// 上传URL：统一使用相对路径，通过Nginx代理到后端
// 这样Cookie可以正常传递，避免跨域问题
const uploadUrl = '/api/admin/plant/upload'

const plantForm = reactive({
  plantId: null,
  plantName: '',
  description: '',
  dropWeight: 40,
  resourceCode: '',
  imageUrl: '',
  colorHex: '#4CAF50',
  purifyRange: 1,
  width: 1,
  scale: 100,
  status: 1
})

const rules = {
  plantName: [{ required: true, message: '请输入植物名称', trigger: 'blur' }],
  dropWeight: [{ required: true, message: '请输入掉落权重', trigger: 'blur' }]
}

const resetForm = () => {
  plantForm.plantId = null
  plantForm.plantName = ''
  plantForm.description = ''
  plantForm.dropWeight = 40
  plantForm.resourceCode = ''
  plantForm.imageUrl = ''
  plantForm.colorHex = '#4CAF50'
  plantForm.purifyRange = 1
  plantForm.width = 1
  plantForm.scale = 100
  plantForm.status = 1
}

const fetchPlantList = async () => {
  loading.value = true
  try {
    plantList.value = await getPlantList()
  } catch (e) {
    console.error('获取植物列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(plantForm, row)
  dialogVisible.value = true
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  uploading.value = true
  return true
}

const handleUploadSuccess = (response) => {
  uploading.value = false
  if (response.code === 200) {
    plantForm.imageUrl = response.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const handleUploadError = () => {
  uploading.value = false
  ElMessage.error('图片上传失败，请重试')
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  
  submitting.value = true
  try {
    if (isEdit.value) {
      await updatePlant(plantForm.plantId, plantForm)
      ElMessage.success('植物更新成功')
    } else {
      await addPlant(plantForm)
      ElMessage.success('植物新增成功')
    }
    dialogVisible.value = false
    fetchPlantList()
  } catch (e) {
    console.error('提交失败', e)
  } finally {
    submitting.value = false
  }
}

const handleStatus = async (row, status) => {
  try {
    await updatePlantStatus(row.plantId, status)
    ElMessage.success(status === 1 ? '已上架' : '已下架')
    row.status = status
  } catch (e) {
    console.error('操作失败', e)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该植物吗？', '提示', { type: 'warning' })
    await deletePlant(row.plantId)
    ElMessage.success('删除成功')
    fetchPlantList()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
    }
  }
}

onMounted(() => {
  fetchPlantList()
})
</script>

<style lang="scss" scoped>
.plant-page {
  .action-card {
    margin-bottom: 20px;
  }
  
  .plant-image {
    width: 60px;
    height: 60px;
    border-radius: 4px;
  }
  
  .no-image {
    color: var(--el-text-color-placeholder);
    font-size: 12px;
  }
  
  .desc-text {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
  
  .plant-uploader {
    :deep(.el-upload) {
      border: 1px dashed var(--el-border-color);
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: all 0.3s;
      
      &:hover {
        border-color: var(--el-color-primary);
      }
    }
    
    .upload-icon {
      font-size: 28px;
      color: var(--el-color-primary);
      width: 100px;
      height: 100px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    .uploaded-image {
      width: 100px;
      height: 100px;
      display: block;
    }
    
    .upload-loading {
      width: 100px;
      height: 100px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: var(--el-color-primary);
      
      .el-icon {
        font-size: 24px;
        margin-bottom: 8px;
      }
      
      span {
        font-size: 12px;
      }
    }
  }
  
  .upload-tip {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    margin-top: 8px;
  }
  
  .field-tip {
    color: var(--el-text-color-placeholder);
    font-size: 11px;
    margin-top: 4px;
  }
}
</style>
