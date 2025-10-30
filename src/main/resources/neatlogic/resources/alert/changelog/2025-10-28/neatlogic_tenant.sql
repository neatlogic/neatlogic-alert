ALTER TABLE `alert`
    ADD COLUMN `similar_count` int NULL COMMENT '相似告警数' AFTER `alert_count`;