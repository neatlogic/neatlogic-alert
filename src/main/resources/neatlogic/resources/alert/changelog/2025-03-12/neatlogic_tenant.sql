CREATE TABLE `alert_adaptor`
(
    `id`           bigint NOT NULL COMMENT 'id',
    `alerttype_id` bigint                                  DEFAULT NULL COMMENT '告警类型id',
    `name`         varchar(50) COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '唯一标识',
    `label`        varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
    `file_id`      bigint                                  DEFAULT NULL COMMENT '附件id',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;