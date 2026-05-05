-- 添加植物缩放比例字段
-- 执行时间: 2026-04-09
-- 说明: scale字段用于控制植物显示大小（百分比）
--       100=原大小, 50=缩小一半, 200=放大两倍

ALTER TABLE `biz_plant_dict` 
ADD COLUMN `scale` INT NOT NULL DEFAULT 100 COMMENT '显示缩放比例(百分比: 100=原大小, 50=缩小一半, 200=放大两倍)' 
AFTER `width`;

-- 更新现有植物数据，设置默认缩放比例
UPDATE `biz_plant_dict` SET `scale` = 100 WHERE `scale` IS NULL OR `scale` = 0;
