package com.focusflow.server.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * 安全工具类 - 防篡改签名 & 密码哈希引擎
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【核心功能】
 * 1. 专注记录防篡改签名（端云同步安全保障）
 * 2. 用户密码加盐哈希（账号安全基石）
 *
 * 【论文价值】
 * 本类展示了两种典型的密码学应用场景：
 * - SHA-256 用于数据完整性校验（防篡改）
 * - 加盐哈希用于密码安全存储（防泄露）
 */
public class SecurityUtils {

    // ═══════════════════════════════════════════════════════════════════════════
    // 盐值配置
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 专注记录签名盐值
     * 必须与 Android 端 SecurityUtils.APP_SECRET_SALT 完全一致
     */
    private static final String APP_SECRET_SALT = "FOCUS_FLOW_2025_CYBER_SECURITY_SALT_V2";

    /**
     * 密码哈希盐值
     *
     * 【安全原理】
     * 盐值的作用是防止彩虹表攻击：
     * - 无盐：攻击者可预计算常见密码的哈希值（彩虹表）
     * - 有盐：每个密码的哈希值都是唯一的，彩虹表失效
     *
     * 【生产环境建议】
     * 1. 每个用户使用独立的随机盐值（存储在 user 表中）
     * 2. 或使用 BCrypt 等专门设计的密码哈希算法
     */
    private static final String PASSWORD_SALT = "FOCUS_FLOW_PWD_SECURE_SALT_2025_X7kM";

    // ═══════════════════════════════════════════════════════════════════════════
    // 专注记录签名相关
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 生成专注记录的防篡改签名
     */
    public static String generateSignature(String recordId, Long userId, 
                                           Integer durationMinutes, Long startTime) {
        String rawInput = recordId + userId + durationMinutes + startTime + APP_SECRET_SALT;
        return sha256(rawInput);
    }

    /**
     * 验证签名是否有效
     */
    public static boolean verifySignature(String recordId, Long userId,
                                          Integer durationMinutes, Long startTime,
                                          String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        String expected = generateSignature(recordId, userId, durationMinutes, startTime);
        return constantTimeEquals(expected, signature);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 密码哈希相关
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 对密码进行加盐哈希
     *
     * 【安全设计】
     * 1. 密码拼接盐值：rawPassword + PASSWORD_SALT
     * 2. SHA-256 哈希：不可逆，单向加密
     * 3. 输出 64 位十六进制字符串
     *
     * 【为什么不用 MD5？】
     * - MD5 已被证明存在碰撞漏洞
     * - SHA-256 输出更长，暴力破解难度指数级增加
     *
     * @param rawPassword 用户输入的原始密码
     * @return 加盐后的 SHA-256 哈希值
     */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        String saltedPassword = rawPassword + PASSWORD_SALT;
        return sha256(saltedPassword);
    }

    /**
     * 验证密码是否正确
     *
     * 【安全说明】
     * 使用常量时间比较，防止时序攻击。
     * 即使密码错误，也比较完整字符串后才返回，消除时间侧信道。
     *
     * @param rawPassword 用户输入的原始密码
     * @param hashedPassword 数据库中存储的哈希密码
     * @return 密码是否正确
     */
    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        String computedHash = hashPassword(rawPassword);
        return constantTimeEquals(computedHash, hashedPassword);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 底层工具方法
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * SHA-256 哈希计算
     */
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * 常量时间字符串比较（防止时序攻击）
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}