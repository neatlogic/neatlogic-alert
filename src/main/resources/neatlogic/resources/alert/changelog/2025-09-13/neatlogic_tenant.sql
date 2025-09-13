ALTER TABLE `alert_attrtype`
    ADD COLUMN `is_row` tinyint NULL COMMENT '是否占一行' AFTER `is_top`;
ALTER TABLE `alert_attrtype`
    ADD COLUMN `is_tab` tinyint NULL COMMENT '是否单独页签' AFTER `is_row`;