ALTER TABLE `alert`
    MODIFY COLUMN `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态' AFTER `type`;

CREATE TABLE `alert_status`
(
    `name`  varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一标识',
    `label` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
    `color` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '颜色',
    `sort`  int                                    DEFAULT NULL COMMENT '排序',
    PRIMARY KEY (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;