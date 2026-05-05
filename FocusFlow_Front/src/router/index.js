import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'DataAnalysis' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'bag',
        name: 'Bag',
        component: () => import('@/views/bag/index.vue'),
        meta: { title: '背包管理', icon: 'Box' }
      },
      {
        path: 'plant',
        name: 'Plant',
        component: () => import('@/views/plant/index.vue'),
        meta: { title: '植物图鉴', icon: 'Cherry' }
      },
      {
        path: 'focus',
        name: 'Focus',
        component: () => import('@/views/focus/index.vue'),
        meta: { title: '专注记录', icon: 'Timer' }
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('@/views/config/index.vue'),
        meta: { title: '系统配置', icon: 'Setting' }
      },
      {
        path: 'ai',
        name: 'AI',
        component: () => import('@/views/ai/index.vue'),
        meta: { title: 'AI配置', icon: 'Cpu' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta?.title || 'FocusFlow'} - 后台管理`
  
  // 登录页面直接放行
  if (to.path === '/login') {
    next()
    return
  }
  
  // 检查登录状态
  const adminUser = localStorage.getItem('adminUser')
  if (!adminUser) {
    next('/login')
    return
  }
  
  next()
})

export default router