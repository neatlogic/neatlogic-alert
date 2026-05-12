ALTER TABLE `alert_audit`
  ADD KEY `idx_alert_id` (`alert_id`);

ALTER TABLE `alert_comment`
  ADD KEY `idx_alert_id` (`alert_id`);

CREATE TABLE IF NOT EXISTS `alert_breaker_action_audit` (
  `id` bigint NOT NULL COMMENT 'id',
  `policy_id` bigint DEFAULT NULL COMMENT '熔断策略id',
  `state_id` bigint DEFAULT NULL COMMENT '熔断状态id',
  `trigger` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '触发点：OPEN/AGGREGATE/RECOVER',
  `action_uuid` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '动作配置uuid',
  `action_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '动作名称',
  `action_handler` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '动作插件',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '结束时间',
  `status` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '执行状态',
  `error` text COLLATE utf8mb4_general_ci COMMENT '异常',
  PRIMARY KEY (`id`),
  KEY `idx_policy_id` (`policy_id`),
  KEY `idx_state_id` (`state_id`),
  KEY `idx_trigger` (`trigger`),
  KEY `idx_action_handler` (`action_handler`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警熔断动作执行审计';
