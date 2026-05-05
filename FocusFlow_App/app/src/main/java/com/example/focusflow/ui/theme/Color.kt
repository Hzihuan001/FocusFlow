package com.example.focusflow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ── 固定颜色（不随主题变化）──
val CyberSecondary = Color(0xFF00E5FF)    // 辅助蓝
val CyberTertiary  = Color(0xFFFF2E93)   // 强调粉

// ── 主题自适应颜色（通过 LocalAppColors 读取）──
val CyberPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.primary

val CyberPrimaryDim: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.primaryDim

val CyberBgDeep: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.bgDeep

val CyberCardBg: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.cardBg

val CyberTextMain: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.textMain

val CyberTextSub: Color
    @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSub

// 莫兰迪色系 (Morandi Palette - 用于建筑美学重构)
val MorandiGrayBlue = Color(0xFFB0C4DE)    // 淡灰蓝
val MorandiGreen = Color(0xFF9DBEAF)       // 莫兰迪绿
val MorandiSage = Color(0xFF8DA399)        // 鼠尾草绿
val MorandiDustyRose = Color(0xFFC19A6B)   // 灰粉/土褐

// 纪念碑谷：水之阁 (Water Pavilion) 经典配色
val WaterPavilionDeep = Color(0xFF1A2A3A)     // 背景顶部 (深藏青)
val WaterPavilionDark = Color(0xFF0D151D)     // 背景底部 (极暗夜色)

val SandstoneWhite = Color(0xFFE8DAB2)        // 温暖沙石白 (未种植顶面)
val SandstoneShadow = Color(0xFFC0B291)       // 暗沙石色 (未种植侧面)

val MonumentMint = Color(0xFF4AAB88)          // 薄荷绿 (已种植顶面)
val MonumentDeepMint = Color(0xFF2B7A5F)      // 深薄荷绿 (已种植侧面)

// 预设高亮色
val NeonRed = Color(0xFFFF4B4B)
val CyberPurple = Color(0xFFBC00FF)