ALTER TABLE `alert_origin`
    ADD COLUMN `adaptor` varchar(100) NULL COMMENT '适配器' AFTER `content`;