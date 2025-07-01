CREATE TABLE IF NOT EXISTS `alert_catalog`
(
    `id`        bigint    NOT NULL,
    `name`      varchar(255) COLLATE utf8mb4_general_ci                   DEFAULT NULL COMMENT '名称',
    `is_active` tinyint                                                   DEFAULT NULL COMMENT '是否激活',
    `sort`      int                                                       DEFAULT NULL COMMENT '排序',
    `fcd`       timestamp NULL                                            DEFAULT NULL,
    `fcu`       char(32) COLLATE utf8mb4_general_ci                       DEFAULT NULL,
    `lcd`       timestamp NULL                                            DEFAULT NULL,
    `lcu`       char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `alert_catalog_auth`
(
    `catalog_id` bigint                                                                                NOT NULL,
    `auth_type`  enum ('user','team','role','common') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    `auth_uuid`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci                         NOT NULL,
    PRIMARY KEY (`catalog_id`, `auth_type`, `auth_uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

ALTER TABLE `alert_view`
    ADD COLUMN `sort` int NULL COMMENT '排序' AFTER `lcd`;

ALTER TABLE `alert_view`
    ADD COLUMN `catalog_id` bigint NULL COMMENT '目录id' AFTER `lcd`;