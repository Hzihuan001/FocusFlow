-- ============================================================
-- User1 APP数据看板专项数据
-- 目标：让日报/周报/总体三个视图都有丰富数据
--
-- user_id = 1（原有的第一个用户）
--
-- 日报：今天有多条不同任务的记录，饼图有多个扇区
-- 周报：近7天每天都有记录，柱状图7根柱子都有高度
-- 总体：历史累计大量记录，累计时长可观，饼图任务分布丰富
--
-- 所有时间戳用 UNIX_TIMESTAMP() 动态计算，永远有效
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 今日记录（日报视图）
--    多个不同任务，饼图有5个扇区
--    start_time 落在今天 08:00~22:00
-- ============================================================
INSERT INTO `biz_focus_record`
    (`record_id`,`user_id`,`task_name`,`duration_minutes`,`start_time`,`signature`,`sync_time`,`created_at`,`deleted`)
VALUES
-- 写代码：3次，共 52+52+45 = 149分钟
(UUID(), 1, '写代码', 52,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 28800) * 1000,
    'u1_today_1', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0),
(UUID(), 1, '写代码', 52,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 35000) * 1000,
    'u1_today_2', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0),
(UUID(), 1, '写代码', 45,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 50000) * 1000,
    'u1_today_3', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0),
-- 读论文：2次，共 25+25 = 50分钟
(UUID(), 1, '读论文', 25,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 32000) * 1000,
    'u1_today_4', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0),
(UUID(), 1, '读论文', 25,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 55000) * 1000,
    'u1_today_5', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0),
-- 刷算法题：2次，共 60+52 = 112分钟
(UUID(), 1, '刷算法题', 60,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 38000) * 1000,
    'u1_today_6', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0),
(UUID(), 1, '刷算法题', 52,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 58000) * 1000,
    'u1_today_7', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0),
-- 写毕业论文：2次，共 90+60 = 150分钟
(UUID(), 1, '写毕业论文', 90,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 43000) * 1000,
    'u1_today_8', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0),
(UUID(), 1, '写毕业论文', 60,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 62000) * 1000,
    'u1_today_9', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0),
-- 背单词：1次，25分钟
(UUID(), 1, '背单词', 25,
    (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) + 47000) * 1000,
    'u1_today_10', UNIX_TIMESTAMP(NOW())*1000, UNIX_TIMESTAMP(NOW())*1000, 0);

-- ============================================================
-- 2. 近7天记录（周报柱状图）
--    每天4-8条，时长各不相同，柱子高低有变化
--    包含今天的数据（与日报数据一致）
-- ============================================================

-- 今天（0天）：已在日报部分插入，共10条，约486分钟

-- 昨天（-1天）：6条，约280分钟
INSERT INTO `biz_focus_record`
    (`record_id`,`user_id`,`task_name`,`duration_minutes`,`start_time`,`signature`,`sync_time`,`created_at`,`deleted`)
VALUES
(UUID(),1,'写代码',52,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-86400+30000)*1000,'u1_d1_1',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'读论文',25,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-86400+34000)*1000,'u1_d1_2',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'刷算法题',60, (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-86400+39000)*1000,'u1_d1_3',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'写毕业论文',90,(UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-86400+44000)*1000,'u1_d1_4',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'背单词',25,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-86400+50000)*1000,'u1_d1_5',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'写代码',30,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-86400+56000)*1000,'u1_d1_6',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0);

-- 前天（-2天）：5条，约220分钟
INSERT INTO `biz_focus_record`
    (`record_id`,`user_id`,`task_name`,`duration_minutes`,`start_time`,`signature`,`sync_time`,`created_at`,`deleted`)
VALUES
(UUID(),1,'写代码',52,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-172800+31000)*1000,'u1_d2_1',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'刷算法题',45, (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-172800+37000)*1000,'u1_d2_2',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'读论文',25,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-172800+43000)*1000,'u1_d2_3',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'写毕业论文',60,(UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-172800+49000)*1000,'u1_d2_4',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'复习笔记',38, (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-172800+55000)*1000,'u1_d2_5',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0);

-- -3天：7条，约350分钟（本周最高峰）
INSERT INTO `biz_focus_record`
    (`record_id`,`user_id`,`task_name`,`duration_minutes`,`start_time`,`signature`,`sync_time`,`created_at`,`deleted`)
VALUES
(UUID(),1,'写代码',52,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-259200+29000)*1000,'u1_d3_1',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'写代码',52,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-259200+35000)*1000,'u1_d3_2',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'刷算法题',90, (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-259200+41000)*1000,'u1_d3_3',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'读论文',25,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-259200+47000)*1000,'u1_d3_4',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'写毕业论文',60,(UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-259200+53000)*1000,'u1_d3_5',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'背单词',25,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-259200+59000)*1000,'u1_d3_6',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'复习笔记',45, (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-259200+65000)*1000,'u1_d3_7',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0);

-- -4天：4条，约160分钟（较少的一天）
INSERT INTO `biz_focus_record`
    (`record_id`,`user_id`,`task_name`,`duration_minutes`,`start_time`,`signature`,`sync_time`,`created_at`,`deleted`)
VALUES
(UUID(),1,'写代码',52,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-345600+32000)*1000,'u1_d4_1',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'读论文',25,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-345600+40000)*1000,'u1_d4_2',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'刷算法题',45, (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-345600+48000)*1000,'u1_d4_3',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'背单词',38,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-345600+56000)*1000,'u1_d4_4',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0);

-- -5天：6条，约260分钟
INSERT INTO `biz_focus_record`
    (`record_id`,`user_id`,`task_name`,`duration_minutes`,`start_time`,`signature`,`sync_time`,`created_at`,`deleted`)
VALUES
(UUID(),1,'写毕业论文',90,(UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-432000+30000)*1000,'u1_d5_1',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'写代码',52,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-432000+37000)*1000,'u1_d5_2',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'读论文',25,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-432000+44000)*1000,'u1_d5_3',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'刷算法题',52, (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-432000+51000)*1000,'u1_d5_4',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'复习笔记',25, (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-432000+58000)*1000,'u1_d5_5',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'背单词',16,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-432000+64000)*1000,'u1_d5_6',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0);

-- -6天：5条，约200分钟
INSERT INTO `biz_focus_record`
    (`record_id`,`user_id`,`task_name`,`duration_minutes`,`start_time`,`signature`,`sync_time`,`created_at`,`deleted`)
VALUES
(UUID(),1,'写代码',52,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-518400+31000)*1000,'u1_d6_1',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'刷算法题',60, (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-518400+38000)*1000,'u1_d6_2',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'读论文',25,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-518400+45000)*1000,'u1_d6_3',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'写毕业论文',45,(UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-518400+52000)*1000,'u1_d6_4',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0),
(UUID(),1,'背单词',18,   (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00')))-518400+59000)*1000,'u1_d6_5',UNIX_TIMESTAMP(NOW())*1000,UNIX_TIMESTAMP(NOW())*1000,0);

-- ============================================================
-- 3. 历史记录（总体视图）
--    近30天每天3-5条，任务分布丰富，累计时长可观
-- ============================================================
DROP PROCEDURE IF EXISTS insert_user1_history;

DELIMITER $$
CREATE PROCEDURE insert_user1_history()
BEGIN
    DECLARE i INT DEFAULT 7;   -- 从第7天前开始
    DECLARE j INT DEFAULT 1;
    DECLARE day_start BIGINT;
    DECLARE rec_count INT;
    DECLARE dur INT;
    DECLARE task_name VARCHAR(50);

    WHILE i <= 90 DO
        SET day_start = (UNIX_TIMESTAMP(DATE(CONVERT_TZ(NOW(),'+00:00','+08:00'))) - i * 86400) * 1000;
        -- 每天3-6条
        SET rec_count = 3 + FLOOR(RAND() * 4);
        SET j = 1;

        WHILE j <= rec_count DO
            -- 随机时长：25/30/45/52/60/90
            SET dur = ELT(FLOOR(RAND()*6)+1, 25, 30, 45, 52, 60, 90);
            -- 随机任务（6种，分布均匀）
            SET task_name = ELT(FLOOR(RAND()*6)+1,
                '写代码','读论文','刷算法题','写毕业论文','背单词','复习笔记');

            INSERT IGNORE INTO biz_focus_record
                (record_id, user_id, task_name, duration_minutes, start_time,
                 signature, sync_time, created_at, deleted)
            VALUES (
                UUID(),
                1,
                task_name,
                dur,
                day_start + (28800 + FLOOR(RAND() * 50400)) * 1000,
                CONCAT('u1_hist_', i, '_', j),
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

CALL insert_user1_history();
DROP PROCEDURE IF EXISTS insert_user1_history;

-- ============================================================
-- 4. 更新 User1 的统计字段
-- ============================================================
UPDATE `biz_user` SET
    `streak_days`     = 7,
    `last_focus_date` = DATE_FORMAT(CONVERT_TZ(NOW(),'+00:00','+08:00'), '%Y-%m-%d'),
    `last_focus_time` = UNIX_TIMESTAMP(NOW()) * 1000,
    `time_flux`       = (
        SELECT COALESCE(SUM(duration_minutes), 0)
        FROM biz_focus_record
        WHERE user_id = 1 AND deleted = 0
    ),
    `updated_at`      = UNIX_TIMESTAMP(NOW()) * 1000
WHERE `user_id` = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 执行后 User1 APP数据看板预期效果：
--
-- 【日报】
--   今日专注时长：约 8.6小时（149+50+112+150+25 = 486分钟）
--   饼图5个扇区：写代码31%、刷算法题23%、写毕业论文31%、读论文10%、背单词5%
--
-- 【周报（本周）】
--   近7天柱状图（分钟）：
--   -6天: ~200  -5天: ~260  -4天: ~160  -3天: ~350（最高）
--   -2天: ~220  昨天: ~280  今天: ~486（最高）
--   → 柱子高低起伏，今天最高，-3天次高
--
-- 【总体】
--   历史90天 × 平均4条 × 平均47min ≈ 16920分钟 ≈ 282小时
--   加上近7天 ≈ 总计约 300小时
--   饼图6个任务均匀分布
-- ============================================================

-- ============================================================
-- 5. User1 好友关系（与已有用户建立双向好友）
--    user_id=1 与 user_id 2,3,4,6,8,10,13,16,19,22,24,29 互为好友
--    另有2条申请中（单向）
-- ============================================================
INSERT IGNORE INTO `biz_friendship`
    (`user_id`,`friend_id`,`status`,`created_at`,`updated_at`,`deleted`)
VALUES
-- 双向已同意
(1, 2,  1, (UNIX_TIMESTAMP(NOW())-86400*30)*1000, (UNIX_TIMESTAMP(NOW())-86400*30)*1000, 0),
(2, 1,  1, (UNIX_TIMESTAMP(NOW())-86400*30)*1000, (UNIX_TIMESTAMP(NOW())-86400*30)*1000, 0),
(1, 3,  1, (UNIX_TIMESTAMP(NOW())-86400*28)*1000, (UNIX_TIMESTAMP(NOW())-86400*28)*1000, 0),
(3, 1,  1, (UNIX_TIMESTAMP(NOW())-86400*28)*1000, (UNIX_TIMESTAMP(NOW())-86400*28)*1000, 0),
(1, 4,  1, (UNIX_TIMESTAMP(NOW())-86400*25)*1000, (UNIX_TIMESTAMP(NOW())-86400*25)*1000, 0),
(4, 1,  1, (UNIX_TIMESTAMP(NOW())-86400*25)*1000, (UNIX_TIMESTAMP(NOW())-86400*25)*1000, 0),
(1, 6,  1, (UNIX_TIMESTAMP(NOW())-86400*22)*1000, (UNIX_TIMESTAMP(NOW())-86400*22)*1000, 0),
(6, 1,  1, (UNIX_TIMESTAMP(NOW())-86400*22)*1000, (UNIX_TIMESTAMP(NOW())-86400*22)*1000, 0),
(1, 8,  1, (UNIX_TIMESTAMP(NOW())-86400*20)*1000, (UNIX_TIMESTAMP(NOW())-86400*20)*1000, 0),
(8, 1,  1, (UNIX_TIMESTAMP(NOW())-86400*20)*1000, (UNIX_TIMESTAMP(NOW())-86400*20)*1000, 0),
(1, 10, 1, (UNIX_TIMESTAMP(NOW())-86400*18)*1000, (UNIX_TIMESTAMP(NOW())-86400*18)*1000, 0),
(10,1,  1, (UNIX_TIMESTAMP(NOW())-86400*18)*1000, (UNIX_TIMESTAMP(NOW())-86400*18)*1000, 0),
(1, 13, 1, (UNIX_TIMESTAMP(NOW())-86400*15)*1000, (UNIX_TIMESTAMP(NOW())-86400*15)*1000, 0),
(13,1,  1, (UNIX_TIMESTAMP(NOW())-86400*15)*1000, (UNIX_TIMESTAMP(NOW())-86400*15)*1000, 0),
(1, 16, 1, (UNIX_TIMESTAMP(NOW())-86400*12)*1000, (UNIX_TIMESTAMP(NOW())-86400*12)*1000, 0),
(16,1,  1, (UNIX_TIMESTAMP(NOW())-86400*12)*1000, (UNIX_TIMESTAMP(NOW())-86400*12)*1000, 0),
(1, 19, 1, (UNIX_TIMESTAMP(NOW())-86400*10)*1000, (UNIX_TIMESTAMP(NOW())-86400*10)*1000, 0),
(19,1,  1, (UNIX_TIMESTAMP(NOW())-86400*10)*1000, (UNIX_TIMESTAMP(NOW())-86400*10)*1000, 0),
(1, 22, 1, (UNIX_TIMESTAMP(NOW())-86400*7)*1000,  (UNIX_TIMESTAMP(NOW())-86400*7)*1000,  0),
(22,1,  1, (UNIX_TIMESTAMP(NOW())-86400*7)*1000,  (UNIX_TIMESTAMP(NOW())-86400*7)*1000,  0),
(1, 24, 1, (UNIX_TIMESTAMP(NOW())-86400*5)*1000,  (UNIX_TIMESTAMP(NOW())-86400*5)*1000,  0),
(24,1,  1, (UNIX_TIMESTAMP(NOW())-86400*5)*1000,  (UNIX_TIMESTAMP(NOW())-86400*5)*1000,  0),
(1, 29, 1, (UNIX_TIMESTAMP(NOW())-86400*3)*1000,  (UNIX_TIMESTAMP(NOW())-86400*3)*1000,  0),
(29,1,  1, (UNIX_TIMESTAMP(NOW())-86400*3)*1000,  (UNIX_TIMESTAMP(NOW())-86400*3)*1000,  0),
-- 申请中（user1 发出，对方未同意）
(1, 33, 0, (UNIX_TIMESTAMP(NOW())-86400*1)*1000,  (UNIX_TIMESTAMP(NOW())-86400*1)*1000,  0),
(1, 35, 0, (UNIX_TIMESTAMP(NOW())-3600)*1000,     (UNIX_TIMESTAMP(NOW())-3600)*1000,     0);

-- ============================================================
-- 6. 互访日志（充能 + 留言，双向互动）
--    User1 去充能好友 / 好友来充能 User1
--    User1 收到留言 / User1 给好友留言
-- ============================================================
INSERT INTO `biz_visit_log`
    (`visitor_id`,`host_id`,`action_type`,`content`,`is_read`,`created_at`,`deleted`)
VALUES
-- ── 好友来给 User1 充能（action_type=1）──
(2,  1, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-3600)*1000,       0),
(3,  1, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-7200)*1000,       0),
(6,  1, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-10800)*1000,      0),
(10, 1, 1, NULL, 0, (UNIX_TIMESTAMP(NOW())-14400)*1000,      0),
(16, 1, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400)*1000,      0),
(19, 1, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*2)*1000,    0),
(22, 1, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*2+3600)*1000,0),
(24, 1, 1, NULL, 0, (UNIX_TIMESTAMP(NOW())-86400*3)*1000,    0),
(4,  1, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*4)*1000,    0),
(8,  1, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*5)*1000,    0),
(13, 1, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*6)*1000,    0),
(29, 1, 1, NULL, 0, (UNIX_TIMESTAMP(NOW())-86400*7)*1000,    0),

-- ── User1 去给好友充能 ──
(1, 2,  1, NULL, 1, (UNIX_TIMESTAMP(NOW())-5400)*1000,       0),
(1, 6,  1, NULL, 1, (UNIX_TIMESTAMP(NOW())-9000)*1000,       0),
(1, 10, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400+1800)*1000, 0),
(1, 16, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*2+7200)*1000,0),
(1, 19, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*3+3600)*1000,0),
(1, 22, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*4+5400)*1000,0),
(1, 24, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*5+7200)*1000,0),
(1, 29, 1, NULL, 1, (UNIX_TIMESTAMP(NOW())-86400*6+3600)*1000,0),

-- ── 好友给 User1 留言（action_type=2）──
(2,  1, 2, '加油！你今天的专注时长真的很厉害！',          1, (UNIX_TIMESTAMP(NOW())-3800)*1000,       0),
(3,  1, 2, '一起努力，共同进步！坚持就是胜利！',          1, (UNIX_TIMESTAMP(NOW())-7500)*1000,       0),
(6,  1, 2, '你的花园好漂亮，继续保持专注习惯！',          0, (UNIX_TIMESTAMP(NOW())-11000)*1000,      0),
(10, 1, 2, '今天也要好好专注哦，我们一起冲！',            0, (UNIX_TIMESTAMP(NOW())-86400+2000)*1000, 0),
(16, 1, 2, '量子脉冲已发送，花园已点亮，加油！',          1, (UNIX_TIMESTAMP(NOW())-86400*2+1000)*1000,0),
(19, 1, 2, '看到你连续专注7天了，太厉害了！',             1, (UNIX_TIMESTAMP(NOW())-86400*3+4000)*1000,0),
(22, 1, 2, '你的专注记录给了我很大动力，感谢！',          0, (UNIX_TIMESTAMP(NOW())-86400*4+2000)*1000,0),
(4,  1, 2, '毕业论文加油！你一定可以的！',                1, (UNIX_TIMESTAMP(NOW())-86400*5+3000)*1000,0),
(8,  1, 2, '今天刷了多少算法题？一起交流！',              1, (UNIX_TIMESTAMP(NOW())-86400*6+5000)*1000,0),
(13, 1, 2, '你的花园净化面积好大，植物好多！',            1, (UNIX_TIMESTAMP(NOW())-86400*7+2000)*1000,0),

-- ── User1 给好友留言 ──
(1, 2,  2, '光流骑士，你今天也很努力！一起加油！',        1, (UNIX_TIMESTAMP(NOW())-6000)*1000,       0),
(1, 6,  2, '数据流，你的连续天数好厉害，向你学习！',      1, (UNIX_TIMESTAMP(NOW())-9500)*1000,       0),
(1, 10, 2, '弧光编织者，你的花园真的太美了！',            1, (UNIX_TIMESTAMP(NOW())-86400+3000)*1000, 0),
(1, 16, 2, '像素僧侣，一起冲刺毕业论文！加油！',          1, (UNIX_TIMESTAMP(NOW())-86400*2+8000)*1000,0),
(1, 19, 2, '波纹密码，你的专注时长每天都很稳定！',        1, (UNIX_TIMESTAMP(NOW())-86400*3+6000)*1000,0),
(1, 22, 2, '风暴编码者，今天的算法题刷得怎么样？',        1, (UNIX_TIMESTAMP(NOW())-86400*4+4000)*1000,0),
(1, 24, 2, '禅意黑客，你的花园净化范围好大！',            1, (UNIX_TIMESTAMP(NOW())-86400*5+5000)*1000,0),
(1, 29, 2, '论文苦行僧，毕业论文快写完了吗？加油！',      1, (UNIX_TIMESTAMP(NOW())-86400*6+7000)*1000,0);
