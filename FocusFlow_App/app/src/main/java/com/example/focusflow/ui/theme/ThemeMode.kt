package com.example.focusflow.ui.theme

/**
 * 主题模式枚举
 * 
 * 定义三种主题模式：
 * - FOLLOW_SYSTEM: 跟随系统主题设置
 * - FORCE_DARK: 强制使用深色主题
 * - FORCE_LIGHT: 强制使用浅色主题
 */
enum class ThemeMode {
    FOLLOW_SYSTEM,  // ordinal = 0
    FORCE_DARK,     // ordinal = 1
    FORCE_LIGHT;    // ordinal = 2
    
    companion object {
        /**
         * 从 ordinal 值反序列化为 ThemeMode
         * 
         * @param ordinal 枚举的序号值
         * @return 对应的 ThemeMode，如果 ordinal 无效则返回 FOLLOW_SYSTEM
         */
        fun fromOrdinal(ordinal: Int): ThemeMode = 
            values().getOrNull(ordinal) ?: FOLLOW_SYSTEM
    }
}
