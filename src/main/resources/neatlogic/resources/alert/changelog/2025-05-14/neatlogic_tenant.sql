ALTER TABLE `alert`
    ADD COLUMN `is_delete` tinyint NULL COMMENT '是否删除中' AFTER `port`;