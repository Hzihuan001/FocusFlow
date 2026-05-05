<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-header">
        <div class="logo">
          <span class="logo-icon">⚡</span>
          <span v-show="!isCollapsed" class="logo-text">FocusFlow</span>
        </div>
        <p v-show="!isCollapsed" class="logo-subtitle">后台管理系统</p>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        :collapse="isCollapsed"
        background-color="transparent"
        text-color="#757575"
        active-text-color="#2196F3"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        
        <el-menu-item index="/bag">
          <el-icon><Box /></el-icon>
          <span>背包管理</span>
        </el-menu-item>
        
        <el-menu-item index="/plant">
          <el-icon><Cherry /></el-icon>
          <span>植物图鉴</span>
        </el-menu-item>
        
        <el-menu-item index="/focus">
          <el-icon><Timer /></el-icon>
          <span>专注记录</span>
        </el-menu-item>
        
        <el-menu-item index="/config">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
        
        <el-menu-item index="/ai">
          <el-icon><Cpu /></el-icon>
          <span>AI配置</span>
        </el-menu-item>
      </el-menu>
    </aside>
    
    <!-- 主内容区 -->
    <div class="main-container">
      <!-- 顶部栏 -->
      <header class="header">
        <div class="header-left">
          <el-icon 
            class="collapse-btn" 
            @click="isCollapsed = !isCollapsed"
          >
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        
        <div class="header-right">
          <span class="time">{{ currentTime }}</span>
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" class="admin-avatar">
                {{ adminUser?.nickname?.charAt(0) || 'A' }}
              </el-avatar>
              <span>{{ adminUser?.nickname || '管理员' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <span class="role-tag">{{ adminUser?.role === 'super_admin' ? '超级管理员' : '管理员' }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      
      <!-- 内容区 -->
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()

const isCollapsed = ref(false)
const currentTime = ref('')
const adminUser = ref(null)
let timer = null

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => route.meta?.title || 'FocusFlow')

const updateTime = () => {
  currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
}

// 检查登录状态
const checkAuth = async () => {
  const storedUser = localStorage.getItem('adminUser')
  
  if (!storedUser) {
    router.push('/login')
    return
  }
  
  try {
    adminUser.value = JSON.parse(storedUser)
    // 验证Session是否有效（响应拦截器成功时返回 res.data）
    const data = await request.get('/admin/auth/info')
    if (data && data.adminId) {
      adminUser.value = data
    } else {
      localStorage.removeItem('adminUser')
      router.push('/login')
    }
  } catch (e) {
    // Session无效，清除本地存储
    localStorage.removeItem('adminUser')
    router.push('/login')
  }
}

// 退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      type: 'warning'
    })
    
    await request.post('/admin/auth/logout')
    localStorage.removeItem('adminUser')
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (e) {
    if (e !== 'cancel') {
      localStorage.removeItem('adminUser')
      router.push('/login')
    }
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  checkAuth()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
@import '@/styles/index.scss';

.layout-container {
  display: flex;
  min-height: 100vh;
  background: $bg-light;
}

.sidebar {
  width: 220px;
  background: $bg-card;
  border-right: 1px solid $border-color;
  transition: width 0.3s;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.08);
  
  &.collapsed {
    width: 64px;
  }
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid $border-color;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  
  .logo-icon {
    font-size: 24px;
  }
  
  .logo-text {
    font-size: 18px;
    font-weight: 600;
    color: $primary-color;
  }
}

.logo-subtitle {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 5px;
}

.sidebar-menu {
  border-right: none;
  
  :deep(.el-menu-item) {
    margin: 4px 8px;
    border-radius: 8px;
    
    &:hover {
      background: rgba(33, 150, 243, 0.1);
    }
    
    &.is-active {
      background: rgba(33, 150, 243, 0.15);
      border-left: 3px solid $primary-color;
    }
  }
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: $bg-card;
  border-bottom: 1px solid $border-color;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
  
  .collapse-btn {
    font-size: 20px;
    color: $text-secondary;
    cursor: pointer;
    transition: color 0.3s;
    
    &:hover {
      color: $primary-color;
    }
  }
  
  .page-title {
    font-size: 18px;
    font-weight: 500;
    color: $text-primary;
    margin: 0;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
  
  .time {
    color: $text-secondary;
    font-size: 14px;
  }
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    color: $text-primary;
    
    &:hover {
      color: $primary-color;
    }
    
    .admin-avatar {
      background: linear-gradient(135deg, $primary-color, #1976D2);
      color: #FFFFFF;
      font-weight: bold;
    }
  }
  
  .role-tag {
    color: $text-secondary;
    font-size: 12px;
  }
}

.content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>