CREATE TABLE `alert_topo`
(
    `id`        bigint    NOT NULL COMMENT 'id',
    `name`      varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
    `is_active` tinyint                                 DEFAULT NULL COMMENT '是否激活',
    `config`    longtext COLLATE utf8mb4_general_ci COMMENT '配置',
    `fcd`       timestamp NULL                          DEFAULT NULL,
    `fcu`       char(32) COLLATE utf8mb4_general_ci     DEFAULT NULL,
    `lcd`       timestamp NULL                          DEFAULT NULL,
    `lcu`       char(32) COLLATE utf8mb4_general_ci     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE `alert_topo_widget`
(
    `id`        bigint NOT NULL COMMENT 'id',
    `name`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识，x6注册时用',
    `type`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '图元类型',
    `icon`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '图标',
    `label`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
    `shape`     varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '形状，和前端对应',
    `is_active` tinyint                                                       DEFAULT NULL COMMENT '是否激活',
    `config`    longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_name` (`name`) USING BTREE,
    KEY `idx_type_id` (`shape`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;