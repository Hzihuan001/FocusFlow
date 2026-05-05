<template>
  <div class="login-page" ref="loginPageRef" @mousemove="handleMouseMove">
    <!-- 动态粒子背景 -->
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
    
    <!-- 鼠标跟随光效 -->
    <div 
      class="mouse-glow" 
      :style="{ left: mousePos.x + 'px', top: mousePos.y + 'px' }"
    ></div>
    
    <!-- 登录卡片 -->
    <div class="login-container">
      <div class="login-header">
        <div class="logo">
          <span class="logo-icon">⚡</span>
          <span class="logo-ring"></span>
        </div>
        <h1>FocusFlow</h1>
        <p>后台管理系统</p>
      </div>
      
      <el-form :model="loginForm" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        
        <el-form-item prop="captcha" class="captcha-item">
          <el-input 
            v-model="loginForm.captcha" 
            placeholder="验证码"
            prefix-icon="Key"
            size="large"
            @keyup.enter="handleLogin"
            class="captcha-input"
          />
          <div class="captcha-img" @click="refreshCaptcha">
            <img v-if="captchaImg" :src="captchaImg" alt="验证码" />
            <span v-else class="captcha-loading">加载中...</span>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            <span class="btn-text">登 录</span>
            <span class="btn-glow"></span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 底部版权 -->
    <div class="footer">
      <span>FocusFlow Admin © 2026</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const router = useRouter()
const loading = ref(false)
const formRef = ref(null)
const canvasRef = ref(null)
const loginPageRef = ref(null)

// 鼠标位置
const mousePos = reactive({ x: 0, y: 0 })

// 验证码
const captchaImg = ref('')

// 登录表单
const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 粒子系统
let animationId = null
let particles = []

class Particle {
  constructor(canvas) {
    this.canvas = canvas
    this.reset()
  }
  
  reset() {
    this.x = Math.random() * this.canvas.width
    this.y = Math.random() * this.canvas.height
    this.size = Math.random() * 2 + 1
    this.speedX = (Math.random() - 0.5) * 0.5
    this.speedY = (Math.random() - 0.5) * 0.5
    this.opacity = Math.random() * 0.5 + 0.2
    // 浅色主题蓝色调
    this.hue = 200 + Math.random() * 20
  }
  
  update() {
    this.x += this.speedX
    this.y += this.speedY
    
    if (this.x < 0 || this.x > this.canvas.width) this.speedX *= -1
    if (this.y < 0 || this.y > this.canvas.height) this.speedY *= -1
  }
  
  draw(ctx) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fillStyle = `hsla(${this.hue}, 70%, 60%, ${this.opacity})`
    ctx.fill()
  }
}

// 初始化粒子
const initParticles = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  
  particles = []
  const particleCount = Math.min(100, Math.floor((canvas.width * canvas.height) / 15000))
  
  for (let i = 0; i < particleCount; i++) {
    particles.push(new Particle(canvas))
  }
}

// 动画循环
const animate = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  
  // 更新和绘制粒子
  particles.forEach(particle => {
    particle.update()
    particle.draw(ctx)
  })
  
  // 绘制连线
  particles.forEach((p1, i) => {
    particles.slice(i + 1).forEach(p2 => {
      const dx = p1.x - p2.x
      const dy = p1.y - p2.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      
      if (dist < 150) {
        ctx.beginPath()
        ctx.strokeStyle = `rgba(33, 150, 243, ${0.15 * (1 - dist / 150)})`
        ctx.lineWidth = 0.5
        ctx.moveTo(p1.x, p1.y)
        ctx.lineTo(p2.x, p2.y)
        ctx.stroke()
      }
    })
  })
  
  animationId = requestAnimationFrame(animate)
}

// 鼠标移动处理
const handleMouseMove = (e) => {
  const rect = loginPageRef.value?.getBoundingClientRect()
  if (rect) {
    mousePos.x = e.clientX - rect.left
    mousePos.y = e.clientY - rect.top
  }
}

// 获取验证码
const refreshCaptcha = async () => {
  try {
    const data = await request.get('/admin/captcha/image')
    captchaImg.value = data.img
  } catch (e) {
    console.error('获取验证码失败', e)
  }
}

// 登录
const handleLogin = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  
  loading.value = true
  
  try {
    const data = await request.post('/admin/auth/login', {
      username: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha
    })
    
    if (data && data.adminId) {
      localStorage.setItem('adminUser', JSON.stringify(data))
      ElMessage.success('登录成功')
      router.push('/dashboard')
    }
  } catch (e) {
    // 登录失败刷新验证码
    refreshCaptcha()
    loginForm.captcha = ''
  } finally {
    loading.value = false
  }
}

// 窗口大小变化
const handleResize = () => {
  initParticles()
}

onMounted(() => {
  initParticles()
  animate()
  refreshCaptcha()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #E3F2FD 0%, #FAFAFA 50%, #F5F9FD 100%);
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.mouse-glow {
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(33, 150, 243, 0.08) 0%, transparent 70%);
  pointer-events: none;
  transform: translate(-50%, -50%);
  transition: left 0.1s ease-out, top 0.1s ease-out;
}

.login-container {
  width: 420px;
  padding: 45px 40px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(33, 150, 243, 0.2);
  border-radius: 20px;
  box-shadow: 
    0 0 60px rgba(33, 150, 243, 0.08),
    0 25px 50px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  position: relative;
  z-index: 10;
  
  &::before {
    content: '';
    position: absolute;
    top: -1px;
    left: 20%;
    right: 20%;
    height: 2px;
    background: linear-gradient(90deg, transparent, #2196F3, transparent);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 35px;
  
  .logo {
    position: relative;
    display: inline-block;
    margin-bottom: 15px;
    
    .logo-icon {
      font-size: 52px;
      display: block;
      animation: pulse 2s ease-in-out infinite;
    }
    
    .logo-ring {
      position: absolute;
      top: 50%;
      left: 50%;
      width: 80px;
      height: 80px;
      transform: translate(-50%, -50%);
      border: 2px solid rgba(33, 150, 243, 0.3);
      border-radius: 50%;
      animation: ring-pulse 3s ease-in-out infinite;
    }
  }
  
  h1 {
    font-size: 32px;
    font-weight: 600;
    background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    margin: 0 0 8px;
    letter-spacing: 2px;
  }
  
  p {
    color: rgba(0, 0, 0, 0.5);
    margin: 0;
    font-size: 14px;
    letter-spacing: 1px;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

@keyframes ring-pulse {
  0%, 100% { 
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.3;
  }
  50% { 
    transform: translate(-50%, -50%) scale(1.1);
    opacity: 0.6;
  }
}

.login-form {
  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.8);
    border: 1px solid rgba(33, 150, 243, 0.2);
    border-radius: 10px;
    box-shadow: none;
    padding: 4px 15px;
    transition: all 0.3s ease;
    
    &:hover {
      border-color: rgba(33, 150, 243, 0.5);
      background: rgba(255, 255, 255, 0.95);
    }
    
    &.is-focus {
      border-color: #2196F3;
      box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
    }
  }
  
  :deep(.el-input__inner) {
    color: #1A1A1A;
    font-size: 15px;
    
    &::placeholder {
      color: rgba(0, 0, 0, 0.35);
    }
  }
  
  :deep(.el-input__prefix-inner) {
    color: rgba(33, 150, 243, 0.7);
    font-size: 18px;
  }
  
  :deep(.el-form-item) {
    margin-bottom: 24px;
  }
}

.captcha-item {
  :deep(.el-form-item__content) {
    display: flex;
    gap: 12px;
  }
  
  .captcha-input {
    flex: 1;
  }
  
  .captcha-img {
    width: 120px;
    height: 40px;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    background: rgba(255, 255, 255, 0.8);
    border: 1px solid rgba(33, 150, 243, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
    
    &:hover {
      border-color: rgba(33, 150, 243, 0.5);
      transform: scale(1.02);
    }
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .captcha-loading {
      color: rgba(0, 0, 0, 0.4);
      font-size: 12px;
    }
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  
  .btn-text {
    position: relative;
    z-index: 2;
  }
  
  .btn-glow {
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
    transition: left 0.5s ease;
  }
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 30px rgba(33, 150, 243, 0.3);
    
    .btn-glow {
      left: 100%;
    }
  }
  
  &:active {
    transform: translateY(0);
  }
}

.footer {
  position: absolute;
  bottom: 30px;
  color: rgba(0, 0, 0, 0.35);
  font-size: 12px;
  letter-spacing: 1px;
}
</style>
