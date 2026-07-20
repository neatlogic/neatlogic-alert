ALTER TABLE `alert_event_handler_audit`
    ADD INDEX `idx_parent_id_id` (`parent_id`, `id`) USING BTREE;
