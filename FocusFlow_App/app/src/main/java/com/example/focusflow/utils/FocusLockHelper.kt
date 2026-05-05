package com.example.focusflow.utils

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log

/**
 * 专注锁屏增强工具
 *
 * 检测和引导用户开启专注锁屏的增强功能：
 * 1. 悬浮窗权限 - 用于显示全屏锁屏覆盖层
 * 2. 使用情况访问权限 - 用于防逃逸检测
 */
object FocusLockHelper {
    
    private const val TAG = "FocusLockHelper"

    /**
     * 检测悬浮窗权限是否已开启
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context).also {
                Log.d(TAG, "悬浮窗权限状态: $it")
            }
        } else {
            true // Android 6.0 以下默认有权限
        }
    }

    /**
     * 检测使用情况访问权限是否已开启
     * 用于 UsageStatsManager 查询前台应用
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true // Android 5.0 以下不需要此权限
        }
        
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        
        val granted = mode == AppOpsManager.MODE_ALLOWED
        Log.d(TAG, "使用情况访问权限状态: $granted (mode=$mode)")
        return granted
    }

    /**
     * 打开悬浮窗权限设置页面
     */
    fun openOverlaySettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

    /**
     * 打开使用情况访问权限设置页面
     */
    fun openUsageStatsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "无法打开使用情况设置页面", e)
            // 备选方案：打开应用详情页
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    /**
     * 获取锁屏增强状态（双权限检测）
     */
    fun getLockEnhancementStatus(context: Context): LockEnhancementStatus {
        val overlayEnabled = canDrawOverlays(context)
        val usageStatsEnabled = hasUsageStatsPermission(context)
        
        return LockEnhancementStatus(
            overlayEnabled = overlayEnabled,
            usageStatsEnabled = usageStatsEnabled,
            isFullyProtected = overlayEnabled && usageStatsEnabled
        )
    }
}

/**
 * 锁屏增强状态
 */
data class LockEnhancementStatus(
    val overlayEnabled: Boolean,      // 悬浮窗权限
    val usageStatsEnabled: Boolean,   // 使用情况访问权限
    val isFullyProtected: Boolean     // 双权限齐全
) {
    val statusText: String
        get() = when {
            isFullyProtected -> "🛡️ 已开启增强保护"
            overlayEnabled && !usageStatsEnabled -> "⚠️ 缺少防逃逸权限"
            !overlayEnabled && usageStatsEnabled -> "⚠️ 缺少锁屏权限"
            else -> "❌ 未开启"
        }
    
    val description: String
        get() = when {
            isFullyProtected -> "专注时无法切出应用"
            overlayEnabled && !usageStatsEnabled -> "需开启使用情况访问权限"
            !overlayEnabled && usageStatsEnabled -> "需开启悬浮窗权限"
            else -> "点击开启专注锁屏增强"
        }
}
