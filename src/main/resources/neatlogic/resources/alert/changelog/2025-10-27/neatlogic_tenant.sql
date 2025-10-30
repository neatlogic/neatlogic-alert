CREATE TABLE `alert_similar`
(
    `id`       bigint    NOT NULL COMMENT 'id',
    `alert_id` bigint                                                       DEFAULT NULL COMMENT '告警id',
    `count`    int                                                          DEFAULT NULL COMMENT '相似告警数',
    `color`    varchar(50) COLLATE utf8mb4_general_ci                       DEFAULT NULL COMMENT '颜色',
    `tag`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '提示标签',
    `time`     timestamp NULL                                               DEFAULT NULL COMMENT '告警时间',
    PRIMARY KEY (`id`),
    KEY `idx_alert_id` (`alert_id`) USING BTREE,
    KEY `idx_time` (`time`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;