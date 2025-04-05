ALTER TABLE `alert_event_handler_audit`
    ADD COLUMN `server_id` int NULL COMMENT '服务器id' AFTER `result`;
ALTER TABLE `alert_event_handler_audit`
    ADD INDEX `idx_server_id_status` (`server_id`, `status`) USING BTREE;