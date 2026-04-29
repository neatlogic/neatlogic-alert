CREATE TABLE `alert_breaker_policy` (
  `id` bigint NOT NULL COMMENT 'id',
  `name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `handler` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '熔断策略插件',
  `config` text COLLATE utf8mb4_general_ci COMMENT '插件配置',
  `is_active` tinyint DEFAULT 1 COMMENT '是否激活',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '说明',
  `fcu` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `fcd` timestamp NULL DEFAULT NULL,
  `lcu` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `lcd` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_handler` (`handler`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警熔断策略';

CREATE TABLE `alert_event_handler_breaker_policy_rel` (
  `id` bigint NOT NULL COMMENT 'id',
  `event_handler_id` bigint NOT NULL COMMENT '事件插件实例id',
  `policy_id` bigint NOT NULL COMMENT '熔断策略id',
  `sort` int DEFAULT 0 COMMENT '执行顺序',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_handler_policy` (`event_handler_id`,`policy_id`),
  KEY `idx_event_handler_id` (`event_handler_id`),
  KEY `idx_policy_id` (`policy_id`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='事件插件熔断策略关联';

CREATE TABLE `alert_breaker_state` (
  `id` bigint NOT NULL COMMENT 'id',
  `policy_id` bigint NOT NULL COMMENT '策略id',
  `unique_key` char(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '熔断对象唯一标识，由handler生成MD5',
  `state` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态：CLOSED/OPEN',
  `window_start` timestamp NULL DEFAULT NULL COMMENT '当前统计窗口开始时间',
  `window_end` timestamp NULL DEFAULT NULL COMMENT '当前统计窗口结束时间',
  `trigger_count` int NOT NULL DEFAULT 0 COMMENT '当前窗口触发次数',
  `open_time` timestamp NULL DEFAULT NULL COMMENT '熔断开始时间',
  `open_until` timestamp NULL DEFAULT NULL COMMENT '熔断截止时间',
  `last_trigger_time` timestamp NULL DEFAULT NULL COMMENT '最后触发时间',
  `skip_count` int NOT NULL DEFAULT 0 COMMENT '熔断期间跳过次数',
  `data` text COLLATE utf8mb4_general_ci COMMENT '插件自定义状态数据',
  `fcd` timestamp NULL DEFAULT NULL,
  `lcd` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_unique_key` (`policy_id`,`unique_key`),
  KEY `idx_policy_id` (`policy_id`),
  KEY `idx_open_until` (`open_until`),
  KEY `idx_window_end` (`window_end`),
  KEY `idx_last_trigger_time` (`last_trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警熔断状态';

CREATE TABLE `alert_breaker_audit` (
  `id` bigint NOT NULL COMMENT 'id',
  `policy_id` bigint NOT NULL COMMENT '策略id',
  `state_id` bigint DEFAULT NULL COMMENT '熔断状态id',
  `alert_id` bigint DEFAULT NULL COMMENT '触发审计的告警id',
  `event_handler_audit_id` bigint DEFAULT NULL COMMENT '事件插件审计id',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '结束时间',
  `status` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '结果：PASS/OPEN/FAILED',
  `error` text COLLATE utf8mb4_general_ci COMMENT '异常',
  PRIMARY KEY (`id`),
  KEY `idx_policy_id` (`policy_id`),
  KEY `idx_state_id` (`state_id`),
  KEY `idx_alert_id` (`alert_id`),
  KEY `idx_event_handler_audit_id` (`event_handler_audit_id`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警熔断执行审计';
