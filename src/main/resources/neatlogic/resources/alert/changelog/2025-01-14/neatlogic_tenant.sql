CREATE TABLE `alert_interval_job`
(
    `alert_id`               bigint    NOT NULL COMMENT '告警id',
    `alert_event_handler_id` bigint    NOT NULL COMMENT '事件组件id',
    `parent_audit_id`        bigint         DEFAULT NULL COMMENT '父组件执行记录id',
    `start_time`             timestamp NULL DEFAULT NULL COMMENT '下次开始时间',
    `interval_minute`        int            DEFAULT NULL COMMENT '间隔分钟',
    `repeat_count`           int            DEFAULT NULL COMMENT '剩余需要重复次数',
    `config`                 text COLLATE utf8mb4_general_ci COMMENT '配置',
    PRIMARY KEY (`alert_id`, `alert_event_handler_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;