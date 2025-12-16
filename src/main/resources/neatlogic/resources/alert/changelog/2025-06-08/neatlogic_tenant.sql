ALTER TABLE `alert_tag`
    MODIFY COLUMN `alert_id` bigint NOT NULL FIRST;
ALTER TABLE `alert_tag`
    MODIFY COLUMN `tag_hash` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签做散列' AFTER `alert_id`;
ALTER TABLE `alert_tag`
    ADD PRIMARY KEY (`tag_hash`, `alert_id`);