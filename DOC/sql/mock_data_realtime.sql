-- ============================================================
-- FocusFlow 实时数据补充脚本（解决今日/近7日全为0的问题）
-- 使用 UNIX_TIMESTAMP() 动态计算时间戳，确保数据永远落在"今天"
-- 执行时间：任意时间执行均有效
-- 最后更新：2026-04-29（浅色主题上线后数据更新）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 辅助：计算今天0点的毫秒时间戳（东八区）
-- UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) * 1000
-- ============================================================

-- ============================================================
-- 1. 今日新增用户（3人）
--    created_at 落在今天0点~现在之间
-- ============================================================
INSERT INTO `biz_user` (`account`,`password`,`nickname`,`avatar_id`,`time_flux`,`streak_days`,`last_focus_date`,`last_focus_time`,`status`,`created_at`,`updated_at`,`deleted`)
SELECT
    CONCAT('today_user_', seq) AS account,
    '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92' AS password,
    CONCAT('今日新人', seq) AS nickname,
    (seq % 4) + 1 AS avatar_id,
    0 AS time_flux,
    0 AS streak_days,
    DATE_FORMAT(CONVERT_TZ(NOW(),'+00:00','+08:00'), '%Y-%m-%d') AS last_focus_date,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + seq * 1800) * 1000 AS last_focus_time,
    0 AS status,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + seq * 600) * 1000 AS created_at,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + seq * 600) * 1000 AS updated_at,
    0 AS deleted
FROM (
    SELECT 1 AS seq UNION ALL SELECT 2 UNION ALL SELECT 3
) t
WHERE NOT EXISTS (
    SELECT 1 FROM biz_user WHERE account = CONCAT('today_user_', seq)
);

-- ============================================================
-- 2. 今日专注记录（20条，覆盖15个不同用户）
--    start_time 落在今天各个时段
-- ============================================================
INSERT INTO `biz_focus_record` (`record_id`,`user_id`,`task_name`,`duration_minutes`,`start_time`,`signature`,`sync_time`,`created_at`,`deleted`)
SELECT
    UUID() AS record_id,
    u.user_id,
    ELT(FLOOR(RAND()*6)+1,'写代码','读论文','刷算法题','背单词','写毕业论文','复习笔记') AS task_name,
    ELT(FLOOR(RAND()*5)+1, 25, 30, 45, 52, 60) AS duration_minutes,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + FLOOR(RAND()*57600) + 28800) * 1000 AS start_time,
    CONCAT('realtime_sig_', u.user_id) AS signature,
    (UNIX_TIMESTAMP(NOW())) * 1000 AS sync_time,
    (UNIX_TIMESTAMP(NOW())) * 1000 AS created_at,
    0 AS deleted
FROM (
    SELECT user_id FROM biz_user
    WHERE deleted = 0
    ORDER BY RAND()
    LIMIT 20
) u;

-- ============================================================
-- 3. 近7日专注记录（每天15-25条，覆盖10-20个不同用户）
--    用存储过程批量插入，start_time 精确落在各天
-- ============================================================
DROP PROCEDURE IF EXISTS insert_week_focus;

DELIMITER $$
CREATE PROCEDURE insert_week_focus()
BEGIN
    DECLARE i INT DEFAULT 1;  -- 天数偏移（1=昨天，2=前天...）
    DECLARE j INT DEFAULT 1;  -- 每天的记录序号
    DECLARE day_start BIGINT;
    DECLARE rec_count INT;
    DECLARE uid BIGINT;
    DECLARE tasks_arr VARCHAR(200);
    DECLARE dur INT;

    -- 近7天（不含今天，今天已在上面插入）
    WHILE i <= 6 DO
        SET day_start = (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) - i * 86400) * 1000;
        -- 每天插入 15~22 条
        SET rec_count = 15 + FLOOR(RAND() * 8);
        SET j = 1;

        WHILE j <= rec_count DO
            -- 随机选一个用户
            SELECT user_id INTO uid
            FROM biz_user
            WHERE deleted = 0
            ORDER BY RAND()
            LIMIT 1;

            -- 随机时长
            SET dur = ELT(FLOOR(RAND()*5)+1, 25, 30, 45, 52, 60);

            -- 插入记录，start_time 落在当天 08:00~22:00 之间
            INSERT IGNORE INTO biz_focus_record
                (record_id, user_id, task_name, duration_minutes, start_time, signature, sync_time, created_at, deleted)
            VALUES (
                UUID(),
                uid,
                ELT(FLOOR(RAND()*6)+1,'写代码','读论文','刷算法题','背单词','写毕业论文','复习笔记'),
                dur,
                day_start + (28800 + FLOOR(RAND() * 50400)) * 1000,
                CONCAT('week_sig_', uid, '_', i, '_', j),
                day_start + 86400000,
                day_start + 86400000,
                0
            );

            SET j = j + 1;
        END WHILE;

        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL insert_week_focus();
DROP PROCEDURE IF EXISTS insert_week_focus;

-- ============================================================
-- 4. 近7日新增用户（每天2-4人注册，用于DAU趋势的新增用户柱）
-- ============================================================
DROP PROCEDURE IF EXISTS insert_week_users;

DELIMITER $$
CREATE PROCEDURE insert_week_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE j INT DEFAULT 1;
    DECLARE day_start BIGINT;
    DECLARE reg_count INT;
    DECLARE acc VARCHAR(64);

    WHILE i <= 6 DO
        SET day_start = (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) - i * 86400) * 1000;
        SET reg_count = 2 + FLOOR(RAND() * 3);
        SET j = 1;

        WHILE j <= reg_count DO
            SET acc = CONCAT('week_u_', i, '_', j, '_', FLOOR(RAND()*9000+1000));

            INSERT IGNORE INTO biz_user
                (account, password, nickname, avatar_id, time_flux, streak_days,
                 last_focus_date, last_focus_time, status, created_at, updated_at, deleted)
            VALUES (
                acc,
                '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
                CONCAT('周用户', i, '-', j),
                (j % 4) + 1,
                FLOOR(RAND() * 2000),
                FLOOR(RAND() * 10),
                DATE_FORMAT(CONVERT_TZ(FROM_UNIXTIME(day_start/1000),'+00:00','+08:00'), '%Y-%m-%d'),
                day_start + (28800 + FLOOR(RAND()*50400)) * 1000,
                0,
                day_start + j * 3600000,
                day_start + j * 3600000,
                0
            );

            SET j = j + 1;
        END WHILE;

        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

CALL insert_week_users();
DROP PROCEDURE IF EXISTS insert_week_users;

-- ============================================================
-- 5. 留存分析数据
--    为近7天注册的用户，在注册后第1/3/7天插入专注记录
--    确保留存曲线有数据
-- ============================================================
DROP PROCEDURE IF EXISTS insert_retention_data;

DELIMITER $$
CREATE PROCEDURE insert_retention_data()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE uid BIGINT;
    DECLARE reg_ts BIGINT;
    DECLARE reg_day BIGINT;
    DECLARE day1_ts BIGINT;
    DECLARE day3_ts BIGINT;
    DECLARE day7_ts BIGINT;
    DECLARE today_start BIGINT;

    DECLARE cur CURSOR FOR
        SELECT user_id, created_at
        FROM biz_user
        WHERE deleted = 0
          AND created_at >= (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) - 7 * 86400) * 1000
          AND created_at < UNIX_TIMESTAMP(NOW()) * 1000
        ORDER BY created_at;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    SET today_start = UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) * 1000;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO uid, reg_ts;
        IF done THEN LEAVE read_loop; END IF;

        -- 注册当天0点
        SET reg_day = FLOOR(reg_ts / 86400000) * 86400000;

        -- 次日（+1天）
        SET day1_ts = reg_day + 86400000 + 36000000; -- +10小时，落在次日上午
        -- 第3日（+3天）
        SET day3_ts = reg_day + 3 * 86400000 + 36000000;
        -- 第7日（+7天）
        SET day7_ts = reg_day + 7 * 86400000 + 36000000;

        -- 次日记录（只插入已过去的时间）
        IF day1_ts < UNIX_TIMESTAMP(NOW()) * 1000 THEN
            INSERT IGNORE INTO biz_focus_record
                (record_id, user_id, task_name, duration_minutes, start_time, signature, sync_time, created_at, deleted)
            VALUES (
                UUID(), uid, '写代码', 25, day1_ts,
                CONCAT('ret1_', uid), day1_ts + 1800000, day1_ts + 1800000, 0
            );
        END IF;

        -- 第3日记录
        IF day3_ts < UNIX_TIMESTAMP(NOW()) * 1000 THEN
            INSERT IGNORE INTO biz_focus_record
                (record_id, user_id, task_name, duration_minutes, start_time, signature, sync_time, created_at, deleted)
            VALUES (
                UUID(), uid, '读论文', 52, day3_ts,
                CONCAT('ret3_', uid), day3_ts + 1800000, day3_ts + 1800000, 0
            );
        END IF;

        -- 第7日记录
        IF day7_ts < UNIX_TIMESTAMP(NOW()) * 1000 THEN
            INSERT IGNORE INTO biz_focus_record
                (record_id, user_id, task_name, duration_minutes, start_time, signature, sync_time, created_at, deleted)
            VALUES (
                UUID(), uid, '刷算法题', 60, day7_ts,
                CONCAT('ret7_', uid), day7_ts + 1800000, day7_ts + 1800000, 0
            );
        END IF;

    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL insert_retention_data();
DROP PROCEDURE IF EXISTS insert_retention_data;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 执行完毕后预期效果（2026-04-29 更新）：
-- 今日新增用户：3人
-- 今日活跃（DAU）：20人
-- 今日专注时长：约 20条 × 平均43min ≈ 860分钟
-- 近7日趋势：每天15-22条记录，时长600-1100分钟，折线图有数据
-- DAU趋势：每天10-20个活跃用户，每天2-4个新增用户
-- 留存分析：近7天注册用户在第1/3/7天均有专注记录，留存率有数据
-- 
-- 注意：此脚本使用动态时间计算，无论何时执行都会生成"今日"数据
-- 适用于浅色主题上线后的数据展示测试
-- ============================================================
