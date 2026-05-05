package com.example.focusflow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 打印友好颜色对象
 * 
 * 为数据可视化组件提供固定的彩色方案，不随主题变化
 * 确保数据可视化的一致性和可读性
 */
object PrintFriendlyColors {
    
    /**
     * 图表颜色：固定彩色系（不随主题变化）
     * 适用于所有主题，保持数据可视化的一致性
     */
    val chartColors = listOf(
        Color(0xFF00FF9D),  // 霓虹绿
        Color(0xFF00E5FF),  // 霓虹蓝
        Color(0xFFFF2E93),  // 霓虹粉
        Color(0xFFFFD700),  // 金黄
        Color(0xFFBC00FF),  // 紫色
        Color(0xFFFF4B4B)   // 红色
    )
    
    /**
     * 获取图表颜色数组（固定彩色，不随主题变化）
     * 
     * @return 固定的彩色列表（6 种颜色）
     */
    @Composable
    fun getChartColors(): List<Color> {
        return chartColors
    }
}
