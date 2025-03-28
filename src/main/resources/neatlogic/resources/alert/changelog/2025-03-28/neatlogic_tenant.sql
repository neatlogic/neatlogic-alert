ALTER TABLE `alert_event_handler_audit`
    ADD COLUMN `config` text NULL COMMENT '事件配置' AFTER `error`;