CREATE TABLE `alert_team`
(
    `alert_id`  bigint                                                    NOT NULL COMMENT '告警id',
    `team_uuid` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分组uuid',
    PRIMARY KEY (`alert_id`, `team_uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE `alert_user`
(
    `alert_id` bigint                                                    NOT NULL COMMENT '告警id',
    `user_id`  char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户uuid',
    PRIMARY KEY (`alert_id`, `user_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;