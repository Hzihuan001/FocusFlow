package com.example.focusflow.utils

import java.security.MessageDigest

/**
 * 数据安全工具类 —— 防篡改签名引擎
 *
 * 【设计意图与论文价值】
 * 本模块是 FocusFlow "离线优先" 架构的安全基石。传统的时间管理应用依赖服务器实时验证，
 * 一旦断网便无法记录或容易被破解。本系统创新性地采用了"客户端签名 + 服务端验签"的
 * 双向校验机制，确保：
 *
 * ① 离线场景下用户可正常完成专注并落库，体验零中断
 * ② 即使用户使用 SQLite 编辑器篡改本地数据库，签名不匹配会导致云端拒绝同步
 * ③ 签名算法包含不可预测的 recordId(UUID)，防止重放攻击
 *
 * 【技术实现】
 * - 算法: SHA-256 (单向哈希，不可逆)
 * - 签名源: recordId + userId + durationMinutes + startTime + APP_SECRET_SALT
 * - 输出: 64位十六进制字符串
 *
 * 【重要】盐值必须与服务端 SecurityUtils.APP_SECRET_SALT 保持完全一致！
 */
object SecurityUtils {

    /**
     * 应用专属盐值
     *
     * 【安全说明】
     * 此盐值必须与服务端 com.focusflow.server.common.SecurityUtils.APP_SECRET_SALT 完全一致
     * 否则会导致验签失败，同步被拒绝
     */
    private const val APP_SECRET_SALT = "FOCUS_FLOW_2025_CYBER_SECURITY_SALT_V2"

    /**
     * 生成专注记录的防篡改数字签名
     *
     * 【签名公式】
     * signature = SHA-256(recordId + userId + durationMinutes + startTime + APP_SECRET_SALT)
     *
     * 注意：字段直接拼接，无分隔符
     *
     * @param recordId 记录唯一标识符 (UUID字符串)
     * @param userId 用户ID
     * @param durationMinutes 专注时长(分钟)
     * @param startTime 专注开始时间戳
     * @return 64位十六进制签名字符串
     */
    fun generateFocusSignature(
        recordId: String,
        userId: Long,
        durationMinutes: Int,
        startTime: Long
    ): String {
        // 字段直接拼接，顺序必须与服务端完全一致
        val rawInput = "$recordId$userId$durationMinutes$startTime$APP_SECRET_SALT"
        return sha256(rawInput)
    }

    /**
     * 验证签名有效性
     *
     * @param recordId 记录ID
     * @param userId 用户ID
     * @param durationMinutes 专注时长
     * @param startTime 开始时间
     * @param signature 待验证的签名
     * @return 签名是否有效
     */
    fun verifySignature(
        recordId: String,
        userId: Long,
        durationMinutes: Int,
        startTime: Long,
        signature: String
    ): Boolean {
        val expectedSignature = generateFocusSignature(recordId, userId, durationMinutes, startTime)
        return constantTimeEquals(expectedSignature, signature)
    }

    /**
     * 标准 SHA-256 单向哈希算法
     */
    private fun sha256(input: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 常量时间字符串比较（防止时序攻击）
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}