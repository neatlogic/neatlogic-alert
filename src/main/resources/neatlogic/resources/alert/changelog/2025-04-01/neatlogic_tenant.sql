ALTER TABLE `alert_attrtype`
    ADD COLUMN `sort`   int     NULL COMMENT '排序' AFTER `is_normal`,
    ADD COLUMN `is_top` tinyint NULL COMMENT '是否置顶' AFTER `sort`;