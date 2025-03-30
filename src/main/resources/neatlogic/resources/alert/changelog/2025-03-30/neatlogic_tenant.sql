CREATE TABLE `alert_enum`
(
    `id`        bigint NOT NULL COMMENT 'id',
    `attr_type` bigint                                                        DEFAULT NULL COMMENT '属性类型',
    `value`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '值',
    `text`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '显示名',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk` (`attr_type`, `value`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;