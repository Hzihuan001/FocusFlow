/*
 Navicat MySQL Dump SQL

 Source Server         : Focus
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45-0ubuntu0.24.04.1)
 Source Host           : 101.34.249.20:3306
 Source Schema         : focus_flow

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45-0ubuntu0.24.04.1)
 File Encoding         : 65001

 Date: 14/04/2026 20:36:19
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for biz_focus_record
-- ----------------------------
DROP TABLE IF EXISTS `biz_focus_record`;
CREATE TABLE `biz_focus_record`  (
  `record_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专注记录ID（UUID）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `duration_minutes` int NOT NULL COMMENT '专注时长（分钟）',
  `start_time` bigint NOT NULL COMMENT '专注开始时间戳（毫秒）',
  `signature` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '防篡改签名（SHA-256）',
  `sync_time` bigint NULL DEFAULT NULL COMMENT '同步到云端的时间戳（毫秒）',
  `created_at` bigint NOT NULL COMMENT '记录创建时间戳',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_start_time`(`start_time` ASC) USING BTREE,
  INDEX `idx_sync_time`(`sync_time` ASC) USING BTREE,
  INDEX `idx_user_start`(`user_id` ASC, `start_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '专注记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for biz_friendship
-- ----------------------------
DROP TABLE IF EXISTS `biz_friendship`;
CREATE TABLE `biz_friendship`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `user_id` bigint NOT NULL COMMENT '发起用户ID',
  `friend_id` bigint NOT NULL COMMENT '接收用户ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0=申请中，1=已同意）',
  `created_at` bigint NOT NULL COMMENT '创建时间戳',
  `updated_at` bigint NOT NULL COMMENT '更新时间戳',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_friend`(`user_id` ASC, `friend_id` ASC) USING BTREE,
  INDEX `idx_friend_id`(`friend_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '好友关系表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for biz_garden_tile
-- ----------------------------
DROP TABLE IF EXISTS `biz_garden_tile`;
CREATE TABLE `biz_garden_tile`  (
  `tile_id` bigint NOT NULL AUTO_INCREMENT COMMENT '地块ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `x` int NOT NULL COMMENT '网格X坐标',
  `y` int NOT NULL COMMENT '网格Y坐标',
  `plant_id` int NULL DEFAULT NULL COMMENT '植物ID',
  `bag_record_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联的背包记录ID',
  `deploy_time` bigint NULL DEFAULT NULL COMMENT '部署时间戳',
  `last_charge_time` bigint NULL DEFAULT NULL COMMENT '最后一次充能时间戳',
  `is_purified` tinyint NOT NULL DEFAULT 0 COMMENT '是否已净化',
  `created_at` bigint NOT NULL COMMENT '创建时间戳',
  `updated_at` bigint NOT NULL COMMENT '更新时间戳',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`tile_id`) USING BTREE,
  UNIQUE INDEX `uk_user_xy`(`user_id` ASC, `x` ASC, `y` ASC) USING BTREE,
  INDEX `idx_plant_id`(`plant_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 129 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '花园地块表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for biz_plant_dict
-- ----------------------------
DROP TABLE IF EXISTS `biz_plant_dict`;
CREATE TABLE `biz_plant_dict`  (
  `plant_id` int NOT NULL AUTO_INCREMENT COMMENT '植物ID',
  `plant_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '植物名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '植物描述',
  `drop_weight` int NOT NULL DEFAULT 1 COMMENT '盲盒掉落权重',
  `resource_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '资源编码',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '植物图片URL',
  `color_hex` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#00FF00' COMMENT '植物主色调',
  `purify_range` int NOT NULL DEFAULT 1 COMMENT '净化辐射半径',
  `width` int NOT NULL DEFAULT 1 COMMENT '植物占地尺寸',
  `scale` int NOT NULL DEFAULT 10 COMMENT '显示缩放比例(10=原大小,20=放大一倍,5=缩小一半)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0=下架，1=上架）',
  `created_at` bigint NOT NULL COMMENT '创建时间戳',
  `updated_at` bigint NOT NULL COMMENT '更新时间戳',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`plant_id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '赛博植物图鉴配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for biz_user
-- ----------------------------
DROP TABLE IF EXISTS `biz_user`;
CREATE TABLE `biz_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）',
  `account` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录账号（唯一）',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（SHA-256加盐哈希值）',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '用户昵称',
  `avatar_id` int NOT NULL DEFAULT 1 COMMENT '预设头像ID',
  `time_flux` int NOT NULL DEFAULT 0 COMMENT '光流余额（核心货币资产）',
  `streak_days` int NOT NULL DEFAULT 0 COMMENT '连续专注天数',
  `last_focus_date` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后专注日期（YYYY-MM-DD）',
  `last_focus_time` bigint NULL DEFAULT 0 COMMENT '最后专注时间戳（毫秒）',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '账号状态（0=正常，1=封禁）',
  `created_at` bigint NOT NULL COMMENT '注册时间戳（毫秒）',
  `updated_at` bigint NOT NULL COMMENT '最后更新时间戳（毫秒）',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_account`(`account` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '移动端用户主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for biz_user_bag
-- ----------------------------
DROP TABLE IF EXISTS `biz_user_bag`;
CREATE TABLE `biz_user_bag`  (
  `bag_id` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '背包物品ID（UUID）',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plant_id` int NOT NULL COMMENT '植物字典ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0=种子，1=成熟，2=已种植）',
  `obtained_at` bigint NOT NULL COMMENT '获取时间戳',
  `created_at` bigint NOT NULL COMMENT '创建时间戳',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`bag_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_status`(`user_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户背包表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for biz_visit_log
-- ----------------------------
DROP TABLE IF EXISTS `biz_visit_log`;
CREATE TABLE `biz_visit_log`  (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `visitor_id` bigint NOT NULL COMMENT '访客ID',
  `host_id` bigint NOT NULL COMMENT '花园主人ID',
  `action_type` tinyint NOT NULL COMMENT '互动类型（1=充能，2=留言）',
  `content` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '留言内容',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读',
  `created_at` bigint NOT NULL COMMENT '创建时间戳',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`log_id`) USING BTREE,
  INDEX `idx_host_id`(`host_id` ASC) USING BTREE,
  INDEX `idx_visitor_id`(`visitor_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '互访日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_admin
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin`;
CREATE TABLE `sys_admin`  (
  `admin_id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录用户名',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（SHA-256哈希）',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '显示名称',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'admin' COMMENT '角色',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0=正常，1=禁用）',
  `last_login_at` bigint NULL DEFAULT NULL COMMENT '最后登录时间戳',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `created_at` bigint NOT NULL COMMENT '创建时间戳',
  `updated_at` bigint NOT NULL COMMENT '更新时间戳',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`admin_id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '后台管理员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_admin_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin_login_log`;
CREATE TABLE `sys_admin_login_log`  (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `admin_id` bigint NOT NULL COMMENT '管理员ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录用户名',
  `login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '登录IP',
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '浏览器UA',
  `login_result` tinyint NOT NULL COMMENT '登录结果（0=成功）',
  `login_at` bigint NOT NULL COMMENT '登录时间戳',
  PRIMARY KEY (`log_id`) USING BTREE,
  INDEX `idx_admin_id`(`admin_id` ASC) USING BTREE,
  INDEX `idx_login_at`(`login_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员登录日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置值',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置描述',
  `config_group` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置分组',
  `created_at` bigint NOT NULL COMMENT '创建时间戳',
  `updated_at` bigint NOT NULL COMMENT '更新时间戳',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`config_id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
