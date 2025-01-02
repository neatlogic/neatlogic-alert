CREATE TABLE `alert_team`
(
    `id`        bigint NOT NULL COMMENT 'id',
    `alert_id`  bigint                              DEFAULT NULL COMMENT '告警id',
    `team_uuid` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '分组uuid',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE `alert_user`
(
    `id`       bigint NOT NULL COMMENT 'id',
    `alert_id` bigint                                                    DEFAULT NULL COMMENT '告警id',
    `user_id`  char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户uuid',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;