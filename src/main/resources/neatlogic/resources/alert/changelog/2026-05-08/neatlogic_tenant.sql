ALTER TABLE `alert_breaker_state`
  MODIFY COLUMN `state` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态：CLOSED/OPEN/COLLECTING/FLUSHING';

CREATE TABLE `alert_breaker_collect_item` (
  `id` bigint NOT NULL COMMENT 'id',
  `state_id` bigint NOT NULL COMMENT '熔断状态id',
  `policy_id` bigint NOT NULL COMMENT '策略id',
  `alert_id` bigint NOT NULL COMMENT '告警id',
  `fcd` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_state_alert` (`state_id`,`alert_id`),
  KEY `idx_state_id` (`state_id`),
  KEY `idx_policy_id` (`policy_id`),
  KEY `idx_alert_id` (`alert_id`),
  KEY `idx_fcd` (`fcd`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警熔断收集明细';
