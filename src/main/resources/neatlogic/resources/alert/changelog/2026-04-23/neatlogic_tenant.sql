ALTER TABLE `alert_catalog`
    ADD COLUMN `parent_id` bigint NULL COMMENT '父目录id' AFTER `name`,
    ADD INDEX `idx_parent_id` (`parent_id`),
    ADD INDEX `idx_parent_id_sort` (`parent_id`, `sort`);
