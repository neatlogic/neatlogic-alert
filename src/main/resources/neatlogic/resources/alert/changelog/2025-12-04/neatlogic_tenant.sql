ALTER TABLE `alert_attrtype`
    ADD COLUMN `is_show` tinyint NULL DEFAULT 1 COMMENT '是否显示' AFTER `is_index`;