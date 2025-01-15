CREATE TABLE `alert_event_handler_audit`
(
    `id`               bigint                                                                               NOT NULL COMMENT 'id',
    `parent_id`        bigint                                                                               NULL DEFAULT NULL COMMENT '父Id',
    `alert_id`         bigint                                                                               NULL DEFAULT NULL COMMENT '告警id',
    `event`            varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci                         NULL DEFAULT NULL COMMENT '事件',
    `handler`          varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci                         NULL DEFAULT NULL COMMENT '事件处理器唯一标识',
    `handler_name`     varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci                        NULL DEFAULT NULL COMMENT '事件处理器名称',
    `event_handler_id` bigint                                                                               NULL DEFAULT NULL COMMENT '事件处理器id',
    `start_time`       timestamp(3)                                                                         NULL DEFAULT NULL COMMENT '开始时间',
    `end_time`         timestamp(3)                                                                         NULL DEFAULT NULL COMMENT '结束时间',
    `status`           enum ('succeed','failed','running') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态',
    `error`            text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci                                NULL COMMENT '异常',
    `result`           text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci                                NULL COMMENT '结果',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_alert_id` (`alert_id` ASC) USING BTREE,
    INDEX `idx_start_time` (`start_time` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = Dynamic;