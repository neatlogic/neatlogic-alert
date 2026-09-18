ALTER TABLE `alert_interval_job`
    ADD COLUMN `interval_second` int DEFAULT NULL COMMENT '间隔秒数' AFTER `interval_minute`;
