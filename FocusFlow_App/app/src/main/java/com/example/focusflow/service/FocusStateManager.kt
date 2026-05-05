package com.example.focusflow.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 单例状态管理器，用于解耦 FocusService(负责时间与状态机) 和 LockOverlayService(负责悬浮窗锁屏)
 *
 * 【架构解耦设计】：
 * 服务之间不直接持有引用，防止内存泄漏或服务互相影响。
 * 它们通过这个基于 StateFlow 的中介者进行单向响应式数据流通信。
 */
object FocusStateManager {
    private val _isFocusing = MutableStateFlow(false)
    val isFocusing: StateFlow<Boolean> = _isFocusing.asStateFlow()

    // 当前专注参数（用于弹回时导航到锁屏界面）
    var currentTaskName: String = ""
    var currentTotalMinutes: Int = 0
    var currentFocusMinutes: Int = 0
    var currentBreakMinutes: Int = 0
    var currentTag: String = ""

    fun setFocusingState(isFocusing: Boolean) {
        _isFocusing.value = isFocusing
    }
    
    fun setFocusParams(taskName: String, totalMinutes: Int, focusMinutes: Int, breakMinutes: Int, tag: String) {
        currentTaskName = taskName
        currentTotalMinutes = totalMinutes
        currentFocusMinutes = focusMinutes
        currentBreakMinutes = breakMinutes
        currentTag = tag
    }
    
    fun clearFocusParams() {
        currentTaskName = ""
        currentTotalMinutes = 0
        currentFocusMinutes = 0
        currentBreakMinutes = 0
        currentTag = ""
    }
}
