ALTER TABLE `alert`
    CHANGE COLUMN `is_delete` `is_close` tinyint NULL DEFAULT NULL COMMENT '是否关闭' AFTER `source`;