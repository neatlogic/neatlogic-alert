ALTER TABLE `alert_event_handler_audit`
    ADD COLUMN `is_async` tinyint NULL COMMENT '是否异步' AFTER `server_id`;