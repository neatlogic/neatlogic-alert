ALTER TABLE `alert_event_handler_audit`
    ADD COLUMN `result` text NULL COMMENT '结果' AFTER `error`;

ALTER TABLE `alert_event_handler_audit`
    ADD COLUMN `parent_id` bigint NULL COMMENT '父Id' AFTER `id`;