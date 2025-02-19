ALTER TABLE `alert_attrtype`
    ADD COLUMN `is_normal` tinyint NULL COMMENT '是否作为通用属性展示' AFTER `config`;