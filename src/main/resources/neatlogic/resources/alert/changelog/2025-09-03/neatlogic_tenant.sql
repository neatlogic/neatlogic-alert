CREATE TABLE `alert_trash`
(
    `id`          bigint   NOT NULL COMMENT 'id',
    `level`       int                                                           DEFAULT NULL COMMENT '级别',
    `title`       text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '标题',
    `update_time` datetime                                                      DEFAULT NULL COMMENT '更新时间',
    `delete_time` datetime                                                      DEFAULT NULL COMMENT '删除时间',
    `alert_time`  datetime NOT NULL COMMENT '告警时间',
    `delete_user` char(32) COLLATE utf8mb4_general_ci                           DEFAULT NULL COMMENT '删除用户',
    `type`        bigint                                                        DEFAULT NULL COMMENT '类型',
    `status`      varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  DEFAULT NULL COMMENT '状态',
    `source`      varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
    `is_close`    tinyint                                                       DEFAULT NULL COMMENT '是否关闭',
    `unique_key`  char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     DEFAULT NULL COMMENT '唯一键',
    PRIMARY KEY (`id`),
    KEY `idx_unique_key` (`unique_key`) USING BTREE,
    KEY `idx_delete_time` (`delete_time`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='告警内容';

CREATE TABLE `alert_trash_attr`
(
    `alert_id` bigint NOT NULL,
    `content`  longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
    PRIMARY KEY (`alert_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='告警额外属性';