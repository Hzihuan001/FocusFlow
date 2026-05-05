package com.example.focusflow.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.util.Log

/**
 * 设备管理员接收器 - Kiosk 模式核心组件
 *
 * 【实现原理】
 * 1. 当应用被设置为"设备所有者"后，startLockTask() 会进入真正的 Kiosk 模式
 * 2. 用户无法通过任何手势或按键退出（包括返回+最近任务组合键）
 * 3. 只有调用 stopLockTask() 或管理员强制解除才能退出
 *
 * 【设置方法】
 * 需要通过 ADB 命令设置设备所有者：
 * adb shell dpm set-device-owner com.example.focusflow/.receiver.FocusFlowDeviceAdminReceiver
 *
 * 【解除方法】
 * adb shell dpm remove-active-admin com.example.focusflow/.receiver.FocusFlowDeviceAdminReceiver
 */
class FocusFlowDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "✅ 设备管理员已启用")
        Toast.makeText(context, "FocusFlow 设备管理员已启用", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "❌ 设备管理员已禁用")
        Toast.makeText(context, "FocusFlow 设备管理员已禁用", Toast.LENGTH_SHORT).show()
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.d(TAG, "🔒 进入 Lock Task 模式 (Kiosk)")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.d(TAG, "🔓 退出 Lock Task 模式")
    }
}
