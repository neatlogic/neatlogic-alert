ALTER TABLE `alert_breaker_action_audit`
    ADD COLUMN `breaker_audit_id` bigint NULL COMMENT '熔断审计id' AFTER `id`;

ALTER TABLE `alert_breaker_action_audit`
    ADD INDEX `idx_breaker_audit_id` (`breaker_audit_id`) USING BTREE;
