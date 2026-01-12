CREATE TABLE `alert_action_rel`
(
    `alert_id`    bigint                                 NOT NULL,
    `action_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
    PRIMARY KEY (`alert_id`, `action_name`),
    KEY `name_idx` (`action_name`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE `alert_action`
(
    `id`          bigint NOT NULL,
    `name`        varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
    `icon`        varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
    `label`       varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
    `is_active`   tinyint                                DEFAULT NULL COMMENT '是否激活',
    `script`      longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
    `description` text COLLATE utf8mb4_general_ci COMMENT '说明',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='告警自定义动作';