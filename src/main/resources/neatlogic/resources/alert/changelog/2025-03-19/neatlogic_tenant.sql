ALTER TABLE `alert_event_handler`
    ADD INDEX `idx_parent_id` (`parent_id`) USING BTREE;
ALTER TABLE `alert_origin`
    ADD COLUMN `alert_data` longtext NULL COMMENT '转换后的告警数据' AFTER `status`;