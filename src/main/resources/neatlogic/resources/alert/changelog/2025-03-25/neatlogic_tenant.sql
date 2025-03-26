CREATE TABLE `alert_notify_template`
(
    `id`        bigint NOT NULL COMMENT 'id',
    `name`      varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
    `label`     varbinary(255)                          DEFAULT NULL COMMENT '名称',
    `is_active` tinyint                                 DEFAULT NULL COMMENT '是否激活',
    `title`     text COLLATE utf8mb4_general_ci COMMENT '标题',
    `content`   text COLLATE utf8mb4_general_ci COMMENT '内容',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;