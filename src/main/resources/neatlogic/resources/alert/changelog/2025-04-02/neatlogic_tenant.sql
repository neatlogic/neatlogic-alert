ALTER TABLE `alert_event_handler_audit`
    ADD COLUMN `unique_key` char(32) NULL COMMENT '唯一键' AFTER `parent_id`;