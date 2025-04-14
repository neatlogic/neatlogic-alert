CREATE TABLE `alert_event_plugin`
(
    `name`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一标识',
    `config`    text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置',
    `is_active` tinyint DEFAULT NULL COMMENT '是否激活',
    PRIMARY KEY (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;