ALTER TABLE `alert_event_handler_audit`
    MODIFY COLUMN `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态' AFTER `end_time`;