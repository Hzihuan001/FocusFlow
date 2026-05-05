-- ═══════════════════════════════════════════════════════════════════════════════
-- FocusFlow 服务器部署 SQL 脚本
-- ═══════════════════════════════════════════════════════════════════════════════
-- 适用环境：Ubuntu 24.04 LTS + MySQL 8.0
-- 执行方式：mysql -u root -p < deploy_server.sql
-- 
-- 执行顺序：
-- 1. 创建数据库 focus_flow
-- 2. 创建业务表（用户、专注记录、植物、背包、花园、好友）
-- 3. 创建系统表（管理员、配置、日志）
-- 4. 初始化基础数据（植物图鉴、系统配置、管理员账号）
-- ═══════════════════════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════════════════════
-- 第一步：创建数据库
-- ═══════════════════════════════════════════════════════════════════════════════
CREATE DATABASE IF NOT EXISTS `focus_flow` 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `focus_flow`;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 第二步：创建业务表
-- ═══════════════════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────────────────
-- 表1：biz_user（移动端用户主表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `biz_user` (
    `user_id`           BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID（主键）',
    `account`           VARCHAR(32)     NOT NULL                 COMMENT '登录账号（唯一）',
    `password`          VARCHAR(64)     NOT NULL                 COMMENT '密码（SHA-256加盐哈希值）',
    `nickname`          VARCHAR(64)     NOT NULL DEFAULT ''      COMMENT '用户昵称',
    `avatar_id`         INT             NOT NULL DEFAULT 1       COMMENT '预设头像ID',
    `time_flux`         INT             NOT NULL DEFAULT 0       COMMENT '光流余额（核心货币资产）',
    `streak_days`       INT             NOT NULL DEFAULT 0       COMMENT '连续专注天数',
    `last_focus_date`   VARCHAR(10)     NULL                     COMMENT '最后专注日期（YYYY-MM-DD）',
    `last_focus_time`   BIGINT          DEFAULT 0                COMMENT '最后专注时间戳（毫秒）',
    `status`            TINYINT         NOT NULL DEFAULT 0       COMMENT '账号状态（0=正常，1=封禁）',
    `created_at`        BIGINT          NOT NULL                 COMMENT '注册时间戳（毫秒）',
    `updated_at`        BIGINT          NOT NULL                 COMMENT '最后更新时间戳（毫秒）',
    `deleted`           TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_account` (`account`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
    COMMENT='移动端用户主表';

-- ─────────────────────────────────────────────────────────────────────────────
-- 表2：biz_focus_record（专注记录表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `biz_focus_record` (
    `record_id`         CHAR(36)        NOT NULL                 COMMENT '专注记录ID（UUID）',
    `user_id`           BIGINT          NOT NULL                 COMMENT '用户ID',
    `task_name`         VARCHAR(100)    NOT NULL DEFAULT ''      COMMENT '任务名称',
    `duration_minutes`  INT             NOT NULL                 COMMENT '专注时长（分钟）',
    `start_time`        BIGINT          NOT NULL                 COMMENT '专注开始时间戳（毫秒）',
    `signature`         VARCHAR(64)     NOT NULL DEFAULT ''      COMMENT '防篡改签名（SHA-256）',
    `sync_time`         BIGINT          NULL                     COMMENT '同步到云端的时间戳（毫秒）',
    `created_at`        BIGINT          NOT NULL                 COMMENT '记录创建时间戳',
    `deleted`           TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    PRIMARY KEY (`record_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_sync_time` (`sync_time`),
    KEY `idx_user_start` (`user_id`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
    COMMENT='专注记录表';

-- ─────────────────────────────────────────────────────────────────────────────
-- 表3：biz_plant_dict（赛博植物图鉴配置表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `biz_plant_dict` (
    `plant_id`          INT             NOT NULL AUTO_INCREMENT  COMMENT '植物ID',
    `plant_name`        VARCHAR(50)     NOT NULL                 COMMENT '植物名称',
    `description`       VARCHAR(200)    NOT NULL DEFAULT ''      COMMENT '植物描述',
    `drop_weight`       INT             NOT NULL DEFAULT 1       COMMENT '盲盒掉落权重',
    `resource_code`     VARCHAR(100)    NOT NULL                 COMMENT '资源编码',
    `image_url`         VARCHAR(500)    NULL                     COMMENT '植物图片URL',
    `color_hex`         VARCHAR(7)      NOT NULL DEFAULT '#00FF00' COMMENT '植物主色调',
    `purify_range`      INT             NOT NULL DEFAULT 1       COMMENT '净化辐射半径',
    `width`             INT             NOT NULL DEFAULT 1       COMMENT '植物占地尺寸',
    `scale`             INT             NOT NULL DEFAULT 100     COMMENT '显示缩放比例(百分比:100=原大小,50=缩小一半,200=放大两倍)',
    `status`            TINYINT         NOT NULL DEFAULT 1       COMMENT '状态（0=下架，1=上架）',
    `created_at`        BIGINT          NOT NULL                 COMMENT '创建时间戳',
    `updated_at`        BIGINT          NOT NULL                 COMMENT '更新时间戳',
    `deleted`           TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    PRIMARY KEY (`plant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
    COMMENT='赛博植物图鉴配置表';

-- ─────────────────────────────────────────────────────────────────────────────
-- 表4：biz_user_bag（用户背包表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `biz_user_bag` (
    `bag_id`            CHAR(36)        NOT NULL                 COMMENT '背包物品ID（UUID）',
    `user_id`           BIGINT          NOT NULL                 COMMENT '用户ID',
    `plant_id`          INT             NOT NULL                 COMMENT '植物字典ID',
    `status`            TINYINT         NOT NULL DEFAULT 0       COMMENT '状态（0=种子，1=成熟，2=已种植）',
    `obtained_at`       BIGINT          NOT NULL                 COMMENT '获取时间戳',
    `created_at`        BIGINT          NOT NULL                 COMMENT '创建时间戳',
    `deleted`           TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    PRIMARY KEY (`bag_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
    COMMENT='用户背包表';

-- ─────────────────────────────────────────────────────────────────────────────
-- 表5：biz_garden_tile（花园地块表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `biz_garden_tile` (
    `tile_id`           BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '地块ID',
    `user_id`           BIGINT          NOT NULL                 COMMENT '用户ID',
    `x`                 INT             NOT NULL                 COMMENT '网格X坐标',
    `y`                 INT             NOT NULL                 COMMENT '网格Y坐标',
    `plant_id`          INT             NULL                     COMMENT '植物ID',
    `bag_record_id`     CHAR(36)        NULL                     COMMENT '关联的背包记录ID',
    `deploy_time`       BIGINT          NULL                     COMMENT '部署时间戳',
    `last_charge_time`  BIGINT          NULL                     COMMENT '最后一次充能时间戳',
    `is_purified`       TINYINT         NOT NULL DEFAULT 0       COMMENT '是否已净化',
    `created_at`        BIGINT          NOT NULL                 COMMENT '创建时间戳',
    `updated_at`        BIGINT          NOT NULL                 COMMENT '更新时间戳',
    `deleted`           TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    PRIMARY KEY (`tile_id`),
    UNIQUE KEY `uk_user_xy` (`user_id`, `x`, `y`),
    KEY `idx_plant_id` (`plant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
    COMMENT='花园地块表';

-- ─────────────────────────────────────────────────────────────────────────────
-- 表6：biz_friendship（好友关系表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `biz_friendship` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '关系ID',
    `user_id`           BIGINT          NOT NULL                 COMMENT '发起用户ID',
    `friend_id`         BIGINT          NOT NULL                 COMMENT '接收用户ID',
    `status`            TINYINT         NOT NULL DEFAULT 0       COMMENT '状态（0=申请中，1=已同意）',
    `created_at`        BIGINT          NOT NULL                 COMMENT '创建时间戳',
    `updated_at`        BIGINT          NOT NULL                 COMMENT '更新时间戳',
    `deleted`           TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    KEY `idx_friend_id` (`friend_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
    COMMENT='好友关系表';

-- ─────────────────────────────────────────────────────────────────────────────
-- 表7：biz_visit_log（互访日志表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `biz_visit_log` (
    `log_id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
    `visitor_id`        BIGINT          NOT NULL                 COMMENT '访客ID',
    `host_id`           BIGINT          NOT NULL                 COMMENT '花园主人ID',
    `action_type`       TINYINT         NOT NULL                 COMMENT '互动类型（1=充能，2=留言）',
    `content`           VARCHAR(200)    NULL                     COMMENT '留言内容',
    `is_read`           TINYINT         NOT NULL DEFAULT 0       COMMENT '是否已读',
    `created_at`        BIGINT          NOT NULL                 COMMENT '创建时间戳',
    `deleted`           TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    PRIMARY KEY (`log_id`),
    KEY `idx_host_id` (`host_id`),
    KEY `idx_visitor_id` (`visitor_id`),
    KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
    COMMENT='互访日志表';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 第三步：创建系统表
-- ═══════════════════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────────────────
-- 表8：sys_admin（后台管理员表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `sys_admin` (
    `admin_id`      BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '管理员ID',
    `username`      VARCHAR(50)     NOT NULL                 COMMENT '登录用户名',
    `password`      VARCHAR(64)     NOT NULL                 COMMENT '密码（SHA-256哈希）',
    `nickname`      VARCHAR(50)     NOT NULL                 COMMENT '显示名称',
    `role`          VARCHAR(20)     NOT NULL DEFAULT 'admin' COMMENT '角色',
    `status`        TINYINT         NOT NULL DEFAULT 0       COMMENT '状态（0=正常，1=禁用）',
    `last_login_at` BIGINT          NULL                     COMMENT '最后登录时间戳',
    `last_login_ip` VARCHAR(50)     NULL                     COMMENT '最后登录IP',
    `created_at`    BIGINT          NOT NULL                 COMMENT '创建时间戳',
    `updated_at`    BIGINT          NOT NULL                 COMMENT '更新时间戳',
    `deleted`       TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    PRIMARY KEY (`admin_id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
    COMMENT='后台管理员表';

-- ─────────────────────────────────────────────────────────────────────────────
-- 表9：sys_admin_login_log（管理员登录日志表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `sys_admin_login_log` (
    `log_id`        BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '日志ID',
    `admin_id`      BIGINT          NOT NULL                 COMMENT '管理员ID',
    `username`      VARCHAR(50)     NOT NULL                 COMMENT '登录用户名',
    `login_ip`      VARCHAR(50)     NULL                     COMMENT '登录IP',
    `user_agent`    VARCHAR(255)    NULL                     COMMENT '浏览器UA',
    `login_result`  TINYINT         NOT NULL                 COMMENT '登录结果（0=成功）',
    `login_at`      BIGINT          NOT NULL                 COMMENT '登录时间戳',
    PRIMARY KEY (`log_id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_login_at` (`login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
    COMMENT='管理员登录日志表';

-- ─────────────────────────────────────────────────────────────────────────────
-- 表10：sys_config（系统配置表）
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `sys_config` (
    `config_id`     BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '配置ID',
    `config_key`    VARCHAR(100)    NOT NULL                 COMMENT '配置键',
    `config_value`  TEXT            NOT NULL                 COMMENT '配置值',
    `description`   VARCHAR(200)    NULL                     COMMENT '配置描述',
    `config_group`  VARCHAR(50)     NULL                     COMMENT '配置分组',
    `created_at`    BIGINT          NOT NULL                 COMMENT '创建时间戳',
    `updated_at`    BIGINT          NOT NULL                 COMMENT '更新时间戳',
    `deleted`       TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除标记',
    PRIMARY KEY (`config_id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 第四步：初始化基础数据
-- ═══════════════════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────────────────
-- 初始化植物图鉴（6种赛博植物）
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO `biz_plant_dict` 
(`plant_id`, `plant_name`, `description`, `drop_weight`, `resource_code`, `color_hex`, `purify_range`, `width`, `scale`, `status`, `created_at`, `updated_at`) 
VALUES
-- N级（普通）：drop_weight >= 30
(1, '比特幼苗', '从数据废墟中萌发的初级生命体，叶脉中流淌着二进制能量', 40, 'plant_bit_seedling', '#32CD32', 1, 1, 100, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
(2, '数据蘑菇', '成簇生长在数据腐殖层上的真菌，发光强度随网络流量波动', 35, 'plant_data_shrooms', '#9370DB', 1, 1, 100, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
-- R级（稀有）：drop_weight >= 15
(3, '电路垂柳', '枝条如电路板般精密排列，风中摇曳时发出微弱的电流声', 25, 'plant_circuit_willow', '#00CED1', 2, 2, 150, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
(4, '霓虹棕榈', '热带数据风暴中进化出的耐旱植物，叶片闪烁着温暖的橙光', 20, 'plant_neon_palm', '#FF8C00', 2, 2, 150, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
-- SR级（史诗）：drop_weight >= 5
(5, '霓虹水晶', '罕见的晶体植物，内部封存着古老的代码片段，发出梦幻粉光', 10, 'plant_neon_crystal', '#FF69B4', 3, 1, 120, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
-- SSR级（传说）：drop_weight < 5
(6, '量子仙人掌', '传说级变异仙人掌，据说能感知量子涨落，浑身散发着金色光芒', 3, 'plant_quantum_cactus', '#FFD700', 4, 1, 100, 1, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000)
ON DUPLICATE KEY UPDATE `updated_at` = UNIX_TIMESTAMP()*1000;

-- ─────────────────────────────────────────────────────────────────────────────
-- 初始化系统配置
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`, `config_group`, `created_at`, `updated_at`) VALUES
-- 连续专注奖励
('streak.bonus.3', '5', '连续专注3天奖励光流', '连续专注奖励', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('streak.bonus.5', '10', '连续专注5天奖励光流', '连续专注奖励', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('streak.bonus.7', '20', '连续专注7天奖励光流', '连续专注奖励', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('streak.bonus.14', '30', '连续专注14天奖励光流', '连续专注奖励', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('streak.bonus.21', '50', '连续专注21天奖励光流', '连续专注奖励', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('streak.bonus.30', '100', '连续专注30天奖励光流', '连续专注奖励', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
-- 专注奖励
('focus.reward.per.minute', '1', '每专注1分钟获得光流', '专注奖励', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('focus.drop.min.minutes', '25', '盲盒掉落最低专注时长', '专注奖励', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('focus.drop.base.rate', '0.5', '盲盒掉落基础概率', '专注奖励', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
-- 花园净化
('purify.tiles.per.10min', '1', '每10分钟净化地块数', '花园净化', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('purify.max.tiles', '5', '单次专注最大净化地块数', '花园净化', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
-- 充能系统
('charge.cost', '5', '充能消耗光流', '充能系统', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('charge.restore', '10', '充能恢复活力值', '充能系统', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
-- AI配置（需要后台配置API Key）
('ai.api.provider', 'zhipu', 'API服务商', 'ai', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('ai.model.name', 'glm-4-flash', '模型名称', 'ai', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('ai.api.endpoint', 'https://open.bigmodel.cn/api/paas/v4/', 'API端点URL', 'ai', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('ai.api.key', '', 'AI服务API密钥（后台配置）', 'ai', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('ai.system.prompt', '你是一个友善的学习助手，帮助用户解答学习问题，鼓励他们保持专注。', 'AI助手系统提示词', 'ai', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('ai.max.tokens', '2048', '最大Token数', 'ai', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('ai.temperature', '0.7', '温度参数', 'ai', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('ai.timeout', '60', '超时时间(秒)', 'ai', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000),
('ai.kill.switch', 'false', 'AI熔断开关', 'ai', UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000)
ON DUPLICATE KEY UPDATE `updated_at` = UNIX_TIMESTAMP()*1000;

-- ─────────────────────────────────────────────────────────────────────────────
-- 初始化管理员账号
-- 默认密码：admin123
-- 哈希算法：SHA-256(password + "FocusFlow_Admin_2024")
-- 请在首次登录后立即修改密码！
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO `sys_admin` (`username`, `password`, `nickname`, `role`, `status`, `created_at`, `updated_at`)
VALUES ('admin', '252ae4b107135304abf3b076fd57bb61f234f160e32ac52cf6df51d2826d7e6e', '超级管理员', 'super_admin', 0, UNIX_TIMESTAMP()*1000, UNIX_TIMESTAMP()*1000)
ON DUPLICATE KEY UPDATE `password` = '252ae4b107135304abf3b076fd57bb61f234f160e32ac52cf6df51d2826d7e6e';

-- ═══════════════════════════════════════════════════════════════════════════════
-- 验证部署结果
-- ═══════════════════════════════════════════════════════════════════════════════
SELECT '═════════════════════════════════════════════════════════════' AS '部署结果';
SELECT '数据库表创建完成' AS '状态';

SELECT COUNT(*) AS '植物图鉴数量' FROM biz_plant_dict;
SELECT COUNT(*) AS '系统配置数量' FROM sys_config;
SELECT COUNT(*) AS '管理员账号数量' FROM sys_admin;

SELECT '═════════════════════════════════════════════════════════════' AS '重要提示';
SELECT '管理员账号: admin' AS '用户名';
SELECT '默认密码: admin123' AS '密码';
SELECT '请登录后立即修改密码！' AS '安全提醒';
SELECT '═════════════════════════════════════════════════════════════' AS '完成';
