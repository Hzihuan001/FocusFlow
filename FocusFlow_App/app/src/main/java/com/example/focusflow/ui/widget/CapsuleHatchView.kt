package com.example.focusflow.ui.widget

import android.content.Context
import android.graphics.*
import android.view.Choreographer
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 基因孵化舱视图 - 商业级视觉引擎
 * 
 * 【核心特性】
 * 1. 多层叠加深度 - 赛博金属外壳 + 玻璃内胆 + 能量填充
 * 2. 零分配粒子系统 - 所有对象预分配，onDraw零GC
 * 3. Choreographer驱动 - 60fps丝滑渲染
 * 4. 全息网格覆盖 - 顶层赛博网格
 */
class CapsuleHatchView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ==================== 颜色常量 ====================
    companion object {
        // 粒子系统参数（减少数量避免密集恐惧）
        private const val PARTICLE_POOL_SIZE = 25
        private const val PARTICLE_SPAWN_RATE = 1
        
        // 涟漪系统参数
        private const val RIPPLE_POOL_SIZE = 5
        
        // 默认颜色（深色主题）
        private const val DEFAULT_BG_DEEP = 0xFF0D0D1A.toInt()      // 极夜黑
        private const val DEFAULT_DARK_GRAY = 0xFF2A2A35.toInt()    // 深灰色（金属）
        private const val DEFAULT_NEON_PURPLE = 0xFFB026FF.toInt()  // 霓虹紫
        private const val DEFAULT_NEON_GREEN = 0xFF00FF9F.toInt()   // 霓虹绿
        private const val DEFAULT_NEON_CYAN = 0xFF00FFFF.toInt()    // 霓虹青
        private const val DEFAULT_NEON_BLUE = 0xFF0088FF.toInt()    // 霓虹蓝（紧迫感）
    }
    
    // ==================== 主题颜色（可动态设置） ====================
    private var COLOR_BG_DEEP = DEFAULT_BG_DEEP
    private var COLOR_DARK_GRAY = DEFAULT_DARK_GRAY
    private var COLOR_NEON_PURPLE = DEFAULT_NEON_PURPLE
    private var COLOR_NEON_GREEN = DEFAULT_NEON_GREEN
    private var COLOR_NEON_CYAN = DEFAULT_NEON_CYAN
    private var COLOR_NEON_BLUE = DEFAULT_NEON_BLUE

    // ==================== 状态变量 ====================
    private var currentSeconds: Long = 0      // 当前阶段剩余时间
    private var totalSeconds: Long = 0        // 总剩余时间
    private var initialTotalSeconds: Long = 0 // 初始总时长（用于计算进度）
    private var progress: Float = 0f
    private var targetProgress: Float = 0f  // 目标进度（用于平滑过渡）
    
    // 动画状态
    private var animationTime: Float = 0f
    private var breathePhase: Float = 0f
    private var glowPulse: Float = 0f
    
    // ==================== 预分配画笔（零分配核心） ====================
    // 金属外壳层
    private val metalShellPaint: Paint
    private val metalHighlightPaint: Paint
    private val metalShadowPaint: Paint
    
    // 玻璃内胆层
    private val glassInnerPaint: Paint
    private val glassGlowPaint: Paint
    private val glassReflectionPaint: Paint
    
    // 能量填充层
    private val energyFillPaint: Paint
    private val energySurfacePaint: Paint
    private val energyBubblePaint: Paint
    
    // 粒子系统
    private val particlePaint: Paint
    private val particleGlowPaint: Paint
    private val particleTrailPaint: Paint
    
    // 涟漪系统
    private val ripplePaint: Paint
    
    // 全息网格
    private val gridPaint: Paint
    private val gridGlowPaint: Paint
    
    // 文字
    private val timeTextPaint: Paint
    private val timeTextGlowPaint: Paint
    private val totalTimePaint: Paint
    
    // 内部光晕
    private val innerGlowPaint: Paint
    
    // 呼吸光效（胶囊外围）
    private val breatheGlowPaint: Paint
    
    // 波动线
    private val wavePaint: Paint
    
    // ==================== 预分配几何对象 ====================
    private val capsuleRect: RectF
    private val innerRect: RectF
    private val tempRect: RectF
    private val wavePath: Path
    private val gridPath: Path
    private val ripplePath: Path
    
    // ==================== 粒子对象池 ====================
    // 每个粒子10个属性：x, y, vx, vy, alpha, size, life, maxLife, hueShift, speedMult
    private val particlePool: FloatArray
    private var activeParticleCount: Int = 0
    
    // ==================== 涟漪对象池 ====================
    // 每个涟漪5个属性：x, y, radius, alpha, life
    private val ripplePool: FloatArray
    private var activeRippleCount: Int = 0
    
    // ==================== Choreographer 驱动 ====================
    private val choreographer: Choreographer = Choreographer.getInstance()
    private var isAnimating: Boolean = false
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isAnimating) return
            
            // 更新动画状态（约16.67ms per frame）
            animationTime += 0.016f
            breathePhase = (sin(animationTime * 3.5) * 0.5 + 0.5).toFloat()
            glowPulse = (sin(animationTime * 3.0) * 0.3 + 0.7).toFloat()
            
            // 平滑过渡 progress 到 targetProgress
            // 每帧移动约5%的距离，实现连贯的液体增长效果
            val lerpFactor = 0.05f
            progress = progress + (targetProgress - progress) * lerpFactor
            
            // 更新粒子和涟漪
            updateParticles()
            updateRipples()
            
            // 触发重绘
            invalidate()
            
            // 请求下一帧
            choreographer.postFrameCallback(this)
        }
    }

    init {
        val d = density
        
        // ==================== 金属外壳层画笔 ====================
        metalShellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        
        metalHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * d
        }
        
        metalShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(20f * d, BlurMaskFilter.Blur.NORMAL)
        }
        
        // ==================== 玻璃内胆层画笔 ====================
        glassInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(8f * d, BlurMaskFilter.Blur.NORMAL)
        }
        
        glassGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f * d
            maskFilter = BlurMaskFilter(15f * d, BlurMaskFilter.Blur.NORMAL)
        }
        
        glassReflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * d
        }
        
        // ==================== 能量填充层画笔 ====================
        energyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        
        energySurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(10f * d, BlurMaskFilter.Blur.NORMAL)
        }
        
        energyBubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        
        // ==================== 粒子系统画笔 ====================
        particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        
        particleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(6f * d, BlurMaskFilter.Blur.NORMAL)
        }
        
        particleTrailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        
        // ==================== 涟漪系统画笔 ====================
        ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * d
        }
        
        // ==================== 全息网格画笔 ====================
        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f * d
        }
        
        gridGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * d
            maskFilter = BlurMaskFilter(3f * d, BlurMaskFilter.Blur.NORMAL)
        }
        
        // ==================== 文字画笔 ====================
        timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 64f * d
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            isFakeBoldText = true
        }
        
        timeTextGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 64f * d
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            maskFilter = BlurMaskFilter(20f * d, BlurMaskFilter.Blur.NORMAL)
        }
        
        totalTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 18f * d
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            color = 0xFF888888.toInt()
        }
        
        // ==================== 内部光晕和波动 ====================
        innerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        
        // 呼吸光效（胶囊外围）
        breatheGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 25f * d
            maskFilter = BlurMaskFilter(40f * d, BlurMaskFilter.Blur.NORMAL)
        }
        
        wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * d
        }
        
        // ==================== 预分配几何对象 ====================
        capsuleRect = RectF()
        innerRect = RectF()
        tempRect = RectF()
        wavePath = Path()
        gridPath = Path()
        ripplePath = Path()
        
        // ==================== 初始化粒子池 ====================
        particlePool = FloatArray(PARTICLE_POOL_SIZE * 10)
        activeParticleCount = 0
        
        // ==================== 初始化涟漪池 ====================
        ripplePool = FloatArray(RIPPLE_POOL_SIZE * 5)
        activeRippleCount = 0
    }
    
    private val density: Float
        get() = resources.displayMetrics.density

    // ==================== 公共接口 ====================
    
    /**
     * 设置主题颜色
     * @param bgDeep 背景深色
     * @param primary 主色调（用于霓虹效果）
     * @param isDark 是否为深色主题
     */
    fun setThemeColors(bgDeep: Int, primary: Int, isDark: Boolean) {
        COLOR_BG_DEEP = bgDeep
        
        if (isDark) {
            // 深色主题：使用赛博朋克霓虹色
            COLOR_DARK_GRAY = DEFAULT_DARK_GRAY
            COLOR_NEON_PURPLE = DEFAULT_NEON_PURPLE
            COLOR_NEON_GREEN = DEFAULT_NEON_GREEN
            COLOR_NEON_CYAN = DEFAULT_NEON_CYAN
            COLOR_NEON_BLUE = DEFAULT_NEON_BLUE
        } else {
            // 浅色主题：使用主题色的变体
            COLOR_DARK_GRAY = adjustBrightness(bgDeep, 0.9f)  // 稍微深一点的背景色
            COLOR_NEON_PURPLE = adjustBrightness(primary, 1.2f)  // 稍微亮一点的主色
            COLOR_NEON_GREEN = primary  // 使用主题色
            COLOR_NEON_CYAN = adjustHue(primary, 0.1f)  // 色相偏移
            COLOR_NEON_BLUE = adjustHue(primary, -0.1f)  // 色相偏移
        }
        
        invalidate()  // 触发重绘
    }
    
    /**
     * 调整颜色亮度
     */
    private fun adjustBrightness(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
    
    /**
     * 调整颜色色相
     */
    private fun adjustHue(color: Int, shift: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + shift * 360) % 360
        return Color.HSVToColor(Color.alpha(color), hsv)
    }
    
    /**
     * 设置时间显示
     * @param phaseSeconds 当前阶段剩余时间（专注或休息的倒计时）
     * @param totalSeconds 总剩余时间
     */
    fun setTime(phaseSeconds: Long, totalSeconds: Long) {
        this.currentSeconds = phaseSeconds
        this.totalSeconds = totalSeconds
        
        // 如果初始总时长未设置，使用当前总剩余时间
        if (initialTotalSeconds <= 0 && totalSeconds > 0) {
            initialTotalSeconds = totalSeconds
        }
        
        // 目标进度 = 已用时间比例（液体随时间流逝变满）
        this.targetProgress = if (initialTotalSeconds > 0) {
            1f - (totalSeconds.toFloat() / initialTotalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f
        // 注意：不直接设置progress，让动画循环平滑过渡
    }
    
    /**
     * 设置初始总时长（用于计算进度）
     */
    fun setInitialTotal(seconds: Long) {
        this.initialTotalSeconds = seconds
    }

    // ==================== 生命周期 ====================
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }
    
    private fun startAnimation() {
        if (!isAnimating) {
            isAnimating = true
            choreographer.postFrameCallback(frameCallback)
        }
    }
    
    private fun stopAnimation() {
        isAnimating = false
        choreographer.removeFrameCallback(frameCallback)
    }

    // ==================== 零分配粒子系统 ====================
    
    private fun spawnParticle(startX: Float, startY: Float, targetX: Float, targetY: Float) {
        if (activeParticleCount >= PARTICLE_POOL_SIZE) return
        
        val idx = activeParticleCount * 10
        
        // 初始位置
        particlePool[idx] = startX + (Math.random() * 100 - 50).toFloat()
        particlePool[idx + 1] = startY
        
        // 速度方向
        val dx = targetX - particlePool[idx]
        val dy = targetY - particlePool[idx + 1]
        val dist = sqrt(dx * dx + dy * dy)
        val baseSpeed = 2f * density + (Math.random() * 2f * density).toFloat()
        
        // 速度随进度增加（紧迫感）
        val speedMult = 1f + progress * 0.5f
        
        particlePool[idx + 2] = (dx / dist) * baseSpeed * speedMult
        particlePool[idx + 3] = (dy / dist) * baseSpeed * speedMult - 0.5f * density
        
        // 视觉属性
        particlePool[idx + 4] = 1.0f  // alpha
        particlePool[idx + 5] = (3f + Math.random() * 3f).toFloat() * density  // size
        particlePool[idx + 6] = 0f  // life
        particlePool[idx + 7] = (80f + Math.random() * 40f).toFloat()  // maxLife
        
        // 颜色偏移：随时间从绿→蓝（紧迫感）
        particlePool[idx + 8] = progress * 0.4f  // hueShift (0=绿, 0.4=蓝)
        particlePool[idx + 9] = speedMult  // speedMult
        
        activeParticleCount++
    }
    
    private fun updateParticles() {
        var writeIdx = 0
        
        for (i in 0 until activeParticleCount) {
            val idx = i * 10
            
            // 更新位置
            particlePool[idx] += particlePool[idx + 2]
            particlePool[idx + 1] += particlePool[idx + 3]
            
            // 更新生命周期
            particlePool[idx + 6]++
            
            // 计算透明度
            val life = particlePool[idx + 6]
            val maxLife = particlePool[idx + 7]
            particlePool[idx + 4] = if (life > maxLife * 0.6f) {
                1.0f - (life - maxLife * 0.6f) / (maxLife * 0.4f)
            } else 1.0f
            
            // 更新颜色偏移（随时间变化）
            particlePool[idx + 8] = progress * 0.4f + breathePhase * 0.1f
            
            if (life < maxLife && particlePool[idx + 4] > 0) {
                if (writeIdx != idx) {
                    System.arraycopy(particlePool, idx, particlePool, writeIdx, 10)
                }
                writeIdx += 10
            }
        }
        
        activeParticleCount = writeIdx / 10
    }
    
    // ==================== 零分配涟漪系统 ====================
    
    private fun spawnRipple(x: Float, y: Float) {
        if (activeRippleCount >= RIPPLE_POOL_SIZE) return
        
        val idx = activeRippleCount * 5
        ripplePool[idx] = x
        ripplePool[idx + 1] = y
        ripplePool[idx + 2] = 0f  // radius
        ripplePool[idx + 3] = 1.0f  // alpha
        ripplePool[idx + 4] = 0f  // life
        
        activeRippleCount++
    }
    
    private fun updateRipples() {
        var writeIdx = 0
        
        for (i in 0 until activeRippleCount) {
            val idx = i * 5
            
            ripplePool[idx + 2] += 2f * density  // radius expand
            ripplePool[idx + 3] -= 0.02f  // alpha fade
            ripplePool[idx + 4]++  // life
            
            if (ripplePool[idx + 3] > 0) {
                if (writeIdx != idx) {
                    System.arraycopy(ripplePool, idx, ripplePool, writeIdx, 5)
                }
                writeIdx += 5
            }
        }
        
        activeRippleCount = writeIdx / 5
    }

    // ==================== 绘制引擎 ====================
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f - 60f * density
        val d = density
        
        // 胶囊舱尺寸
        val capsuleWidth = 200f * d
        val capsuleHeight = 280f * d
        val capsuleRadius = 100f * d
        val shellThickness = 8f * d
        
        capsuleRect.set(
            centerX - capsuleWidth / 2,
            centerY - capsuleHeight / 2,
            centerX + capsuleWidth / 2,
            centerY + capsuleHeight / 2
        )
        
        innerRect.set(
            capsuleRect.left + shellThickness,
            capsuleRect.top + shellThickness,
            capsuleRect.right - shellThickness,
            capsuleRect.bottom - shellThickness
        )
        
        // 1. 深色背景
        canvas.drawColor(COLOR_BG_DEEP)
        
        // 2. 全息网格（最底层）
        drawCyberGrid(canvas, width, height)
        
        // 3. 生成粒子
        val spawnRate = PARTICLE_SPAWN_RATE + (progress * 4).toInt()
        if (activeParticleCount < PARTICLE_POOL_SIZE - spawnRate) {
            for (i in 0 until spawnRate) {
                spawnParticle(
                    centerX + (Math.random() * width * 0.6 - width * 0.3).toFloat(),
                    height + 20f * d,
                    centerX + (Math.random() * capsuleWidth * 0.4 - capsuleWidth * 0.2).toFloat(),
                    centerY + capsuleHeight * 0.3f
                )
            }
        }
        
        // 4. 绘制粒子（光流效果）
        drawParticles(canvas)
        
        // 5. 胶囊外围呼吸光效
        drawBreatheGlow(canvas, capsuleRadius)
        
        // 6. 金属外壳阴影
        drawMetalShellShadow(canvas, capsuleRadius)
        
        // 6. 金属外壳
        drawMetalShell(canvas, capsuleRadius)
        
        // 7. 玻璃内胆
        drawGlassInner(canvas, capsuleRadius - shellThickness)
        
        // 8. 能量填充
        drawEnergyFill(canvas, capsuleRadius - shellThickness, capsuleHeight)
        
        // 9. 内部光晕
        drawInnerGlow(canvas, centerX, centerY)
        
        // 10. 涟漪效果
        drawRipples(canvas)
        
        // 11. 玻璃反光
        drawGlassReflection(canvas, capsuleRadius - shellThickness)
        
        // 12. 边框高光
        drawBorderHighlight(canvas, capsuleRadius)
        
        // 13. 全息倒计时
        drawHolographicTimer(canvas, centerX, centerY + capsuleHeight / 2 + 60f * d)
        
        // 14. 总时间
        drawTotalTime(canvas, centerX, centerY + capsuleHeight / 2 + 110f * d)
    }
    
    // ==================== 全息网格 ====================
    
    private fun drawCyberGrid(canvas: Canvas, width: Float, height: Float) {
        val d = density
        val hexSize = 30f * d
        val alpha = (0.08f * 255).toInt()
        
        gridPaint.color = (alpha shl 24) or (COLOR_NEON_PURPLE and 0x00FFFFFF)
        gridGlowPaint.color = (alpha shl 24) or (COLOR_NEON_PURPLE and 0x00FFFFFF)
        
        gridPath.reset()
        
        // 六边形网格
        var row = 0
        var y = -hexSize
        while (y < height + hexSize) {
            val offsetX = if (row % 2 == 0) 0f else hexSize * 0.866f
            var x = -hexSize + offsetX
            while (x < width + hexSize) {
                drawHexagon(gridPath, x, y, hexSize * 0.5f)
                x += hexSize * 1.732f
            }
            y += hexSize * 1.5f
            row++
        }
        
        canvas.drawPath(gridPath, gridPaint)
        
        // 动态脉冲线
        val pulseY = (animationTime * 50f * d) % height
        gridPaint.strokeWidth = 1f * d
        gridPaint.color = ((alpha * 2).coerceAtMost(255) shl 24) or (COLOR_NEON_GREEN and 0x00FFFFFF)
        canvas.drawLine(0f, pulseY, width, pulseY, gridPaint)
    }
    
    private fun drawHexagon(path: Path, cx: Float, cy: Float, radius: Float) {
        for (i in 0 until 6) {
            val angle = Math.PI / 6 + i * Math.PI / 3
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
    
    // ==================== 呼吸光效（胶囊外围） ====================
    
    private fun drawBreatheGlow(canvas: Canvas, radius: Float) {
        if (progress < 0.01f) return
        
        val d = density
        // 呼吸强度：0.3 - 1.0，基于 breathePhase
        val breatheIntensity = 0.3f + 0.7f * breathePhase
        
        // 颜色根据进度变化（绿→青→蓝）
        val urgencyShift = progress * 0.5f
        val glowColor = lerpColor(COLOR_NEON_GREEN, COLOR_NEON_BLUE, urgencyShift)
        
        // 计算透明度（带呼吸效果）
        val alpha = ((breatheIntensity * 0.6f * 255).toInt()).coerceIn(0, 255)
        
        breatheGlowPaint.color = (alpha shl 24) or (glowColor and 0x00FFFFFF)
        breatheGlowPaint.strokeWidth = (20f + 15f * breathePhase) * d
        
        // 绘制外围光晕
        canvas.drawRoundRect(capsuleRect, radius, radius, breatheGlowPaint)
    }
    
    // ==================== 金属外壳 ====================
    
    private fun drawMetalShellShadow(canvas: Canvas, radius: Float) {
        val d = density
        val shadowAlpha = (0.4f * 255).toInt()
        
        tempRect.set(
            capsuleRect.left - 5f * d,
            capsuleRect.top + 10f * d,
            capsuleRect.right + 5f * d,
            capsuleRect.bottom + 15f * d
        )
        
        metalShadowPaint.shader = LinearGradient(
            tempRect.left, tempRect.top,
            tempRect.left, tempRect.bottom,
            intArrayOf(Color.TRANSPARENT, (shadowAlpha shl 24) or 0x00000000),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        
        canvas.drawRoundRect(tempRect, radius, radius, metalShadowPaint)
    }
    
    private fun drawMetalShell(canvas: Canvas, radius: Float) {
        // 金属渐变外壳
        metalShellPaint.shader = LinearGradient(
            capsuleRect.left, capsuleRect.top,
            capsuleRect.right, capsuleRect.bottom,
            intArrayOf(
                COLOR_DARK_GRAY,       // 左上：深灰
                COLOR_BG_DEEP,         // 中间：极夜黑
                COLOR_DARK_GRAY,       // 右下：深灰
                COLOR_BG_DEEP          // 边缘：极夜黑
            ),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        
        canvas.drawRoundRect(capsuleRect, radius, radius, metalShellPaint)
        
        // 高光边缘
        metalHighlightPaint.shader = LinearGradient(
            capsuleRect.left, capsuleRect.top,
            capsuleRect.right, capsuleRect.top,
            intArrayOf(
                Color.TRANSPARENT,
                (0x40 shl 24) or (Color.WHITE and 0x00FFFFFF),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        canvas.drawRoundRect(capsuleRect, radius, radius, metalHighlightPaint)
    }
    
    // ==================== 玻璃内胆 ====================
    
    private fun drawGlassInner(canvas: Canvas, radius: Float) {
        val d = density
        
        // 玻璃内胆径向渐变
        glassInnerPaint.shader = RadialGradient(
            innerRect.centerX(), innerRect.centerY(),
            innerRect.width() / 2,
            intArrayOf(
                Color.TRANSPARENT,                                    // 中心：全透明
                (0x30 shl 24) or (COLOR_NEON_PURPLE and 0x00FFFFFF), // 中间：微紫
                (0x60 shl 24) or (COLOR_NEON_PURPLE and 0x00FFFFFF), // 边缘：霓虹紫
                (0x80 shl 24) or (COLOR_NEON_PURPLE and 0x00FFFFFF)  // 最外：深紫
            ),
            floatArrayOf(0f, 0.5f, 0.85f, 1f),
            Shader.TileMode.CLAMP
        )
        
        canvas.drawRoundRect(innerRect, radius, radius, glassInnerPaint)
        
        // 玻璃发光边缘
        val glowAlpha = (0.4f * glowPulse * 255).toInt()
        glassGlowPaint.color = (glowAlpha shl 24) or (COLOR_NEON_PURPLE and 0x00FFFFFF)
        canvas.drawRoundRect(innerRect, radius, radius, glassGlowPaint)
    }
    
    private fun drawGlassReflection(canvas: Canvas, radius: Float) {
        val d = density
        val reflectAlpha = (0.3f * 255).toInt()
        
        glassReflectionPaint.color = (reflectAlpha shl 24) or (Color.WHITE and 0x00FFFFFF)
        
        // 顶部弧形反光
        tempRect.set(
            innerRect.left + 20f * d,
            innerRect.top + 10f * d,
            innerRect.right - 20f * d,
            innerRect.top + 60f * d
        )
        
        canvas.drawArc(tempRect, 200f, 140f, false, glassReflectionPaint)
    }
    
    // ==================== 能量填充 ====================
    
    private fun drawEnergyFill(canvas: Canvas, radius: Float, capsuleHeight: Float) {
        if (progress <= 0.01f) return
        
        val d = density
        val fillHeight = capsuleHeight * progress * 0.85f  // 留出顶部空间
        val energyTop = innerRect.bottom - fillHeight
        
        // 裁剪到内胆区域
        canvas.save()
        ripplePath.reset()
        ripplePath.addRoundRect(innerRect, radius, radius, Path.Direction.CW)
        canvas.clipPath(ripplePath)
        
        // 能量填充区域
        tempRect.set(
            innerRect.left,
            energyTop,
            innerRect.right,
            innerRect.bottom
        )
        
        // 能量渐变（根据进度变色：绿→青→蓝）
        val urgencyShift = progress * 0.5f
        val baseColor = lerpColor(COLOR_NEON_GREEN, COLOR_NEON_BLUE, urgencyShift)
        val topColor = lerpColor(COLOR_NEON_CYAN, COLOR_NEON_PURPLE, urgencyShift)
        
        energyFillPaint.shader = LinearGradient(
            tempRect.left, tempRect.bottom,
            tempRect.left, tempRect.top,
            intArrayOf(
                baseColor,
                (0xDD shl 24) or (baseColor and 0x00FFFFFF),
                (0x88 shl 24) or (topColor and 0x00FFFFFF)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        canvas.drawRect(tempRect, energyFillPaint)
        
        // 液面发光
        val glowIntensity = (0.6f + 0.4f * glowPulse) * progress
        energySurfacePaint.shader = LinearGradient(
            tempRect.left, energyTop - 15f * d,
            tempRect.left, energyTop + 25f * d,
            intArrayOf(
                Color.TRANSPARENT,
                ((glowIntensity * 0.9f * 255).toInt() shl 24) or (baseColor and 0x00FFFFFF),
                ((glowIntensity * 0.5f * 255).toInt() shl 24) or (topColor and 0x00FFFFFF),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        
        canvas.drawRect(
            tempRect.left, energyTop - 15f * d,
            tempRect.right, energyTop + 25f * d,
            energySurfacePaint
        )
        
        // 液面波动
        drawEnergyWave(canvas, energyTop, baseColor)
        
        // 气泡效果
        drawEnergyBubbles(canvas, tempRect, baseColor)
        
        // 触发涟漪（每30帧）
        if ((animationTime * 60).toInt() % 30 == 0 && activeRippleCount < RIPPLE_POOL_SIZE) {
            spawnRipple(
                innerRect.centerX() + ((Math.random() - 0.5) * innerRect.width() * 0.5).toFloat(),
                energyTop
            )
        }
        
        canvas.restore()
    }
    
    private fun drawEnergyWave(canvas: Canvas, energyTop: Float, color: Int) {
        val d = density
        val glowIntensity = (0.6f + 0.4f * glowPulse) * progress
        
        wavePaint.strokeWidth = 2f * d
        wavePaint.color = ((glowIntensity * 255).toInt() shl 24) or (color and 0x00FFFFFF)
        
        wavePath.reset()
        // 更平缓的波浪参数
        val waveAmplitude = 3f * d * (1f + glowPulse * 0.2f)  // 减小振幅
        val waveFrequency = 1.5f  // 降低频率，波浪更平缓
        
        wavePath.moveTo(innerRect.left, energyTop)
        
        var x = innerRect.left.toInt()
        val rightX = innerRect.right.toInt()
        while (x <= rightX) {
            val waveY = energyTop + sin((x + animationTime * 60) * 0.03f * waveFrequency) * waveAmplitude
            wavePath.lineTo(x.toFloat(), waveY)
            x += 3  // 增加步长，减少计算点
        }
        
        canvas.drawPath(wavePath, wavePaint)
    }
    
    private fun drawEnergyBubbles(canvas: Canvas, rect: RectF, color: Int) {
        val d = density
        val bubbleCount = (progress * 8).toInt()
        
        energyBubblePaint.color = ((0.3f * 255).toInt() shl 24) or (color and 0x00FFFFFF)
        
        for (i in 0 until bubbleCount) {
            val phase = (animationTime * 2 + i * 0.5) % 1.0
            val bx = rect.left + rect.width() * (0.2f + 0.6f * ((i * 0.17) % 1.0).toFloat())
            val by = rect.bottom - rect.height() * phase.toFloat()
            val br = (2f + sin(animationTime * 3 + i.toFloat()) * 1f) * d
            
            canvas.drawCircle(bx, by, br, energyBubblePaint)
        }
    }
    
    // ==================== 涟漪效果 ====================
    
    private fun drawRipples(canvas: Canvas) {
        for (i in 0 until activeRippleCount) {
            val idx = i * 5
            val x = ripplePool[idx]
            val y = ripplePool[idx + 1]
            val radius = ripplePool[idx + 2]
            val alpha = ripplePool[idx + 3]
            
            val rippleAlpha = (alpha * 0.5f * 255).toInt()
            ripplePaint.color = (rippleAlpha shl 24) or (COLOR_NEON_GREEN and 0x00FFFFFF)
            ripplePaint.strokeWidth = 2f * density * alpha
            
            canvas.drawCircle(x, y, radius, ripplePaint)
        }
    }
    
    // ==================== 粒子绘制 ====================
    
    private fun drawParticles(canvas: Canvas) {
        val d = density
        
        for (i in 0 until activeParticleCount) {
            val idx = i * 10
            val x = particlePool[idx]
            val y = particlePool[idx + 1]
            val alpha = particlePool[idx + 4]
            val size = particlePool[idx + 5]
            val hueShift = particlePool[idx + 8]
            
            // 颜色偏移：绿→蓝（紧迫感）
            val particleColor = lerpColor(COLOR_NEON_GREEN, COLOR_NEON_BLUE, hueShift)
            val a = (alpha * 255).toInt().coerceIn(0, 255)
            
            // 发光层
            particleGlowPaint.color = ((a * 0.6f).toInt() shl 24) or (particleColor and 0x00FFFFFF)
            canvas.drawCircle(x, y, size * 1.5f, particleGlowPaint)
            
            // 主粒子
            particlePaint.color = (a shl 24) or (particleColor and 0x00FFFFFF)
            canvas.drawCircle(x, y, size, particlePaint)
            
            // 拖尾
            if (alpha > 0.3f) {
                val trailAlpha = (alpha * 0.4f * 255).toInt().coerceIn(0, 255)
                particleTrailPaint.color = (trailAlpha shl 24) or (particleColor and 0x00FFFFFF)
                
                val vx = particlePool[idx + 2]
                val vy = particlePool[idx + 3]
                canvas.drawCircle(x - vx * 3, y - vy * 3, size * 0.5f, particleTrailPaint)
                canvas.drawCircle(x - vx * 5, y - vy * 5, size * 0.3f, particleTrailPaint)
            }
        }
    }
    
    // ==================== 内部光晕 ====================
    
    private fun drawInnerGlow(canvas: Canvas, cx: Float, cy: Float) {
        val d = density
        val glowIntensity = progress * breathePhase * 0.8f
        val glowRadius = 90f * d * (0.7f + 0.3f * glowIntensity)
        
        innerGlowPaint.shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(
                ((glowIntensity * 0.6f * 255).toInt() shl 24) or (COLOR_NEON_GREEN and 0x00FFFFFF),
                ((glowIntensity * 0.3f * 255).toInt() shl 24) or (COLOR_NEON_CYAN and 0x00FFFFFF),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        canvas.drawCircle(cx, cy, glowRadius, innerGlowPaint)
    }
    
    // ==================== 边框高光 ====================
    
    private fun drawBorderHighlight(canvas: Canvas, radius: Float) {
        val highlightAlpha = (0.5f * glowPulse * 255).toInt()
        
        // 霓虹紫边框
        capsuleBorderPaint ?: return
        
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            color = (highlightAlpha shl 24) or (COLOR_NEON_PURPLE and 0x00FFFFFF)
        }
        
        canvas.drawRoundRect(capsuleRect, radius, radius, borderPaint)
    }
    
    // ==================== 全息倒计时 ====================
    
    private fun drawHolographicTimer(canvas: Canvas, x: Float, y: Float) {
        val timeText = formatTime(currentSeconds)
        val d = density
        
        // 颜色随进度变化（绿→蓝）
        val urgencyShift = progress * 0.3f
        val textColor = lerpColor(COLOR_NEON_GREEN, COLOR_NEON_CYAN, urgencyShift)
        
        // 呼吸发光
        val glowIntensity = 0.6f + 0.4f * breathePhase
        
        // 发光层
        timeTextGlowPaint.color = ((glowIntensity * 255).toInt() shl 24) or (textColor and 0x00FFFFFF)
        canvas.drawText(timeText, x, y, timeTextGlowPaint)
        
        // 主文字
        timeTextPaint.color = textColor
        canvas.drawText(timeText, x, y, timeTextPaint)
    }
    
    private fun drawTotalTime(canvas: Canvas, x: Float, y: Float) {
        val totalText = "总剩余 ${formatTime(totalSeconds)}"
        canvas.drawText(totalText, x, y, totalTimePaint)
    }
    
    // ==================== 工具方法 ====================
    
    private fun lerpColor(color1: Int, color2: Int, t: Float): Int {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        val r = (r1 + (r2 - r1) * t).toInt()
        val g = (g1 + (g2 - g1) * t).toInt()
        val b = (b1 + (b2 - b1) * t).toInt()
        
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    
    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
    
    // 延迟初始化边框画笔
    private val capsuleBorderPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f * density
            color = COLOR_NEON_PURPLE
        }
    }
}