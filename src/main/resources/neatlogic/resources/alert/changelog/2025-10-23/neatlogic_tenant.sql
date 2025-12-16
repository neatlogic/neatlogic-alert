ALTER TABLE `alert_event_handler_audit`
    MODIFY COLUMN `error` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '异常' AFTER `status`;
ALTER TABLE `alert_event_handler_audit`
    MODIFY COLUMN `config` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '事件配置' AFTER `error`;
ALTER TABLE `alert_event_handler_audit`
    MODIFY COLUMN `result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '结果' AFTER `config`;