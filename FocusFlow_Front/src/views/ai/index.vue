<template>
  <div class="ai-page">
    <!-- 状态概览卡片 -->
    <el-row :gutter="20" class="status-row">
      <el-col :xs="24" :sm="12" :lg="8">
        <div class="status-card" :class="{ warning: aiConfig.killSwitch }">
          <div class="status-icon">
            <el-icon v-if="aiConfig.killSwitch" class="danger"><Warning /></el-icon>
            <el-icon v-else class="success"><CircleCheck /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-title">AI 服务状态</div>
            <div class="status-value">{{ aiConfig.killSwitch ? '已熔断' : '正常运行' }}</div>
          </div>
          <el-tag :type="aiConfig.killSwitch ? 'danger' : 'success'" size="large">
            {{ aiConfig.killSwitch ? 'OFFLINE' : 'ONLINE' }}
          </el-tag>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="8">
        <div class="status-card">
          <div class="status-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-title">Prompt 长度</div>
            <div class="status-value">{{ aiConfig.systemPrompt?.length || 0 }} 字符</div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="8">
        <div class="status-card">
          <div class="status-icon">
            <el-icon><Key /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-title">API 配置</div>
            <div class="status-value">{{ aiConfig.apiKey ? '已配置' : '未配置' }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- API 配置区 -->
    <el-card class="api-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><Connection /></el-icon>
            API 接口配置
          </span>
          <div class="card-actions">
            <el-button type="primary" size="small" @click="saveApiConfig" :loading="apiLoading">
              <el-icon><Check /></el-icon>
              保存配置
            </el-button>
          </div>
        </div>
      </template>
      
      <el-form :model="aiConfig" label-width="120px" class="api-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="API 服务商">
              <el-select v-model="aiConfig.apiProvider" placeholder="选择API服务商" style="width: 100%">
                <el-option label="智谱AI (GLM-4)" value="zhipu" />
                <el-option label="OpenAI (GPT-4)" value="openai" />
                <el-option label="阿里云 (通义千问)" value="aliyun" />
                <el-option label="百度 (文心一言)" value="baidu" />
                <el-option label="自定义" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型名称">
              <el-input v-model="aiConfig.modelName" placeholder="如: glm-4-flash, gpt-4-turbo" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="API Key">
          <el-input 
            v-model="aiConfig.apiKey" 
            type="password" 
            placeholder="请输入API密钥" 
            show-password
          >
            <template #prefix>
              <el-icon><Key /></el-icon>
            </template>
          </el-input>
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>API Key 将加密存储，请妥善保管</span>
          </div>
        </el-form-item>
        
        <el-form-item label="API 端点">
          <el-input v-model="aiConfig.apiEndpoint" placeholder="自定义API端点（可选）">
            <template #prefix>
              <el-icon><Link /></el-icon>
            </template>
          </el-input>
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>留空则使用服务商默认端点</span>
          </div>
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="最大Tokens">
              <el-input-number v-model="aiConfig.maxTokens" :min="100" :max="8192" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="温度参数">
              <el-slider v-model="aiConfig.temperature" :min="0" :max="1" :step="0.1" show-input />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时时间(秒)">
              <el-input-number v-model="aiConfig.timeout" :min="5" :max="120" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- Prompt 配置区 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><EditPen /></el-icon>
            全局 Prompt 动态配置
          </span>
          <div class="card-actions">
            <el-button size="small" @click="resetPrompt">
              <el-icon><RefreshRight /></el-icon>
              重置默认
            </el-button>
            <el-button type="primary" size="small" @click="savePrompt" :loading="saveLoading" :disabled="aiConfig.killSwitch">
              <el-icon><Check /></el-icon>
              保存配置
            </el-button>
          </div>
        </div>
      </template>
      
      <div class="prompt-section">
        <div class="prompt-tips">
          <el-alert
            title="配置说明"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              Prompt 是 AI 助手的核心指令，决定了 AI 的行为风格和回答方式。
              修改后立即生效，APP端下次对话时将使用新的 Prompt。
            </template>
          </el-alert>
        </div>
        
        <div class="prompt-editor">
          <div class="editor-label">系统提示词 (System Prompt)</div>
          <el-input
            v-model="aiConfig.systemPrompt"
            type="textarea"
            :rows="10"
            placeholder="输入AI助手系统提示词，定义AI的角色、风格和行为准则..."
            :disabled="aiConfig.killSwitch"
            resize="none"
          />
          <div class="editor-footer">
            <div class="char-info">
              <span :class="{ warning: (aiConfig.systemPrompt?.length || 0) > 4000 }">
                {{ aiConfig.systemPrompt?.length || 0 }} / 4000 字符
              </span>
            </div>
            <div class="preset-actions">
              <el-dropdown @command="applyPreset" :disabled="aiConfig.killSwitch">
                <el-button size="small">
                  应用预设模板 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="friendly">友善学习助手</el-dropdown-item>
                    <el-dropdown-item command="strict">严厉导师</el-dropdown-item>
                    <el-dropdown-item command="gentle">温柔学姐</el-dropdown-item>
                    <el-dropdown-item command="coach">健身教练</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 熔断开关区 -->
    <el-card class="switch-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><Switch /></el-icon>
            AI 熔断开关 (Kill Switch)
          </span>
        </div>
      </template>
      
      <div class="switch-section">
        <div class="switch-info">
          <h3>紧急熔断机制</h3>
          <p>当大模型接口出现故障、额度耗尽或其他异常情况时，可一键关闭 AI 功能。</p>
          <p>熔断后，APP端拉起 AI 助手时将提示"系统维护中"。</p>
        </div>
        
        <div class="switch-control">
          <div class="switch-status">
            <span class="label">当前状态</span>
            <el-tag :type="aiConfig.killSwitch ? 'danger' : 'success'" size="large" effect="dark">
              {{ aiConfig.killSwitch ? '已熔断' : '正常运行' }}
            </el-tag>
          </div>
          
          <el-button
            :type="aiConfig.killSwitch ? 'success' : 'danger'"
            size="large"
            :loading="switchLoading"
            @click="toggleSwitch"
          >
            <el-icon><Switch /></el-icon>
            {{ aiConfig.killSwitch ? '恢复 AI 服务' : '紧急熔断' }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const aiConfig = ref({
  apiProvider: 'zhipu',
  apiKey: '',
  apiEndpoint: '',
  modelName: 'glm-4-flash',
  maxTokens: 2048,
  temperature: 0.7,
  timeout: 30,
  systemPrompt: '',
  killSwitch: false
})

const saveLoading = ref(false)
const apiLoading = ref(false)
const switchLoading = ref(false)

// 预设Prompt模板
const presetPrompts = {
  friendly: `你是一个友善的学习助手，名叫"小流"。你的任务是：
1. 帮助用户解答学习过程中遇到的问题
2. 鼓励用户保持专注，养成良好的学习习惯
3. 用轻松愉快的语气与用户交流
4. 当用户完成专注任务时，给予肯定和鼓励

请用简短的回复，避免冗长的说教。`,

  strict: `你是一位严厉但负责的学习导师。你的职责是：
1. 严格监督用户的学习进度
2. 对拖延行为提出批评
3. 要求用户制定明确的学习计划
4. 帮助用户分析学习效率低下的原因

语气要严肃、直接，不使用表情符号。`,

  gentle: `你是一位温柔的学姐，名叫"小云"。你的风格是：
1. 用温柔、鼓励的语气与用户交流
2. 在用户疲惫时给予关心和安慰
3. 分享一些轻松的学习小技巧
4. 用表情符号让对话更生动可爱`,

  coach: `你是一位充满激情的健身教练！你的任务是：
1. 用充满能量的语气激励用户
2. 把专注比作锻炼，鼓励用户"坚持就是胜利"
3. 使用运动相关的比喻和术语
4. 在用户想要放弃时给予强力鼓励`
}

// 获取AI配置
const fetchAIConfig = async () => {
  try {
    const res = await request.get('/admin/ai/config')
    if (res) {
      aiConfig.value = { ...aiConfig.value, ...res }
    }
  } catch (e) {
    console.error('获取AI配置失败', e)
  }
}

// 保存API配置
const saveApiConfig = async () => {
  apiLoading.value = true
  try {
    await request.put('/admin/ai/config', {
      apiProvider: aiConfig.value.apiProvider,
      apiKey: aiConfig.value.apiKey,
      apiEndpoint: aiConfig.value.apiEndpoint,
      modelName: aiConfig.value.modelName,
      maxTokens: aiConfig.value.maxTokens,
      temperature: aiConfig.value.temperature,
      timeout: aiConfig.value.timeout
    })
    ElMessage.success('API配置保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    apiLoading.value = false
  }
}

// 保存Prompt
const savePrompt = async () => {
  if (!aiConfig.value.systemPrompt?.trim()) {
    ElMessage.warning('Prompt不能为空')
    return
  }
  
  saveLoading.value = true
  try {
    await request.put('/admin/ai/prompt', { prompt: aiConfig.value.systemPrompt })
    ElMessage.success('Prompt更新成功')
  } catch (e) {
    ElMessage.error('更新失败')
  } finally {
    saveLoading.value = false
  }
}

// 重置默认Prompt
const resetPrompt = () => {
  aiConfig.value.systemPrompt = presetPrompts.friendly
  ElMessage.success('已重置为默认Prompt，请点击保存生效')
}

// 应用预设模板
const applyPreset = (key) => {
  aiConfig.value.systemPrompt = presetPrompts[key]
  ElMessage.success(`已应用"${{
    friendly: '友善学习助手',
    strict: '严厉导师',
    gentle: '温柔学姐',
    coach: '健身教练'
  }[key]}"模板`)
}

// 切换熔断开关
const toggleSwitch = async () => {
  const action = aiConfig.value.killSwitch ? '恢复' : '关闭'
  
  try {
    await ElMessageBox.confirm(
      `确定要${action}AI服务吗？此操作将立即生效。`,
      '确认操作',
      { type: 'warning' }
    )
    
    switchLoading.value = true
    await request.put('/admin/ai/switch', { enabled: !aiConfig.value.killSwitch })
    aiConfig.value.killSwitch = !aiConfig.value.killSwitch
    ElMessage.success(aiConfig.value.killSwitch ? 'AI服务已关闭' : 'AI服务已恢复')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  } finally {
    switchLoading.value = false
  }
}

onMounted(() => {
  fetchAIConfig()
})
</script>

<style lang="scss" scoped>
@import '@/styles/index.scss';

.ai-page {
  .status-row {
    margin-bottom: 20px;
  }
  
  .status-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
    background: $bg-card;
    border: 1px solid $border-color;
    border-radius: 12px;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    
    &.warning {
      border-color: rgba(244, 67, 54, 0.3);
      background: rgba(244, 67, 54, 0.05);
    }
    
    .status-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(33, 150, 243, 0.1);
      
      .el-icon {
        font-size: 28px;
        color: $primary-color;
        
        &.danger { color: #F44336; }
        &.success { color: #4CAF50; }
      }
    }
    
    .status-info {
      flex: 1;
      
      .status-title {
        font-size: 13px;
        color: $text-secondary;
        margin-bottom: 4px;
      }
      
      .status-value {
        font-size: 20px;
        font-weight: 600;
        color: $text-primary;
      }
    }
  }
  
  .api-card, .config-card, .switch-card {
    background: $bg-card;
    border: 1px solid $border-color;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    
    :deep(.el-card__header) {
      border-bottom: 1px solid #E0E0E0;
      padding: 16px 20px;
    }
    
    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      
      .card-title {
        display: flex;
        align-items: center;
        gap: 8px;
        color: $primary-color;
        font-weight: 500;
        font-size: 15px;
      }
    }
  }
  
  .api-form {
    .form-tip {
      display: flex;
      align-items: center;
      gap: 4px;
      margin-top: 6px;
      font-size: 12px;
      color: $text-secondary;
    }
  }
  
  .prompt-section {
    .prompt-editor {
      .editor-label {
        font-size: 14px;
        color: $text-primary;
        margin-bottom: 8px;
      }
      
      :deep(.el-textarea__inner) {
        background: $bg-card;
        border: 1px solid #E0E0E0;
        color: $text-primary;
        font-family: 'Consolas', monospace;
        
        &:focus { border-color: $primary-color; }
      }
      
      .editor-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 12px;
        
        .char-info {
          font-size: 13px;
          color: $text-secondary;
          
          .warning { color: #FF9800; }
        }
      }
    }
  }
  
  .switch-section {
    display: flex;
    gap: 40px;
    
    @media (max-width: 768px) {
      flex-direction: column;
      gap: 20px;
    }
    
    .switch-info {
      flex: 1;
      
      h3 {
        color: $text-primary;
        font-size: 16px;
        margin-bottom: 12px;
      }
      
      p {
        color: $text-secondary;
        font-size: 14px;
        line-height: 1.6;
        margin-bottom: 8px;
      }
    }
    
    .switch-control {
      width: 280px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 24px;
      background: #F5F5F5;
      border-radius: 12px;
      
      .switch-status {
        text-align: center;
        
        .label {
          display: block;
          font-size: 13px;
          color: $text-secondary;
          margin-bottom: 8px;
        }
      }
      
      .el-button {
        width: 100%;
        height: 48px;
        font-size: 16px;
      }
    }
  }
}
</style>