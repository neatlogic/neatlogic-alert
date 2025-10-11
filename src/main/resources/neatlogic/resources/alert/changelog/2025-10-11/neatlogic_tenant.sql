CREATE TABLE `alert_mark`
(
    `uuid`  char(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'md5',
    `name`  varchar(50) COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '名称',
    `style` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '风格',
    PRIMARY KEY (`uuid`),
    KEY `idx_name` (`name`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE `alert_alert_mark`
(
    `alert_id`  bigint                              NOT NULL COMMENT '告警id',
    `mark_uuid` char(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签uuid',
    `mark_time` timestamp                           NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`alert_id`, `mark_uuid`),
    KEY `idx_index` (`mark_uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;