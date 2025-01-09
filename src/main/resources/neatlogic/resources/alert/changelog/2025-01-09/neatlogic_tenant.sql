ALTER TABLE `alert_origin`
    ADD COLUMN `source` varchar(100) NULL COMMENT '来源' AFTER `status`;