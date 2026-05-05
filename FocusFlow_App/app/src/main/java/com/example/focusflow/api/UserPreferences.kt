package com.example.focusflow.api

import android.content.Context

object UserPreferences {
    private const val PREF_NAME = "user_settings"

    // 键名定义
    private const val KEY_NICKNAME = "user_nickname"
    private const val KEY_ACCOUNT = "user_account"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_TOTAL_FOCUS_MINUTES = "total_focus_minutes" // 简单的本地统计

    // --- 昵称 ---
    fun getNickname(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NICKNAME, "Focus Runner") ?: "Focus Runner" // 默认叫 Focus Runner
    }

    fun setNickname(context: Context, name: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_NICKNAME, name).apply()
    }

    // --- 账号 ---
    fun getAccount(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCOUNT, "") ?: ""
    }

    fun setAccount(context: Context, account: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACCOUNT, account).apply()
    }

    // --- 音效开关 ---
    fun isSoundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    // --- 震动开关 ---
    fun isVibrationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    // --- 专注时长统计 (简易版) ---
    fun addFocusMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_TOTAL_FOCUS_MINUTES, 0)
        prefs.edit().putInt(KEY_TOTAL_FOCUS_MINUTES, current + minutes).apply()
    }

    fun getTotalFocusMinutes(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TOTAL_FOCUS_MINUTES, 0)
    }
}