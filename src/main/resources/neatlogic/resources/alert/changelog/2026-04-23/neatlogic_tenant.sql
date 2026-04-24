ALTER TABLE `alert_catalog`
    ADD COLUMN `parent_id` bigint NULL COMMENT '父目录id' AFTER `name`,
    ADD INDEX `idx_parent_id` (`parent_id`),
    ADD INDEX `idx_parent_id_sort` (`parent_id`, `sort`);

CREATE TABLE `alert_allalert_config` (
    `name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置名称',
    `config` text COLLATE utf8mb4_general_ci COMMENT '配置',
    `fcu` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
    `fcd` datetime DEFAULT NULL,
    `lcu` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
    `lcd` datetime DEFAULT NULL,
    PRIMARY KEY (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
