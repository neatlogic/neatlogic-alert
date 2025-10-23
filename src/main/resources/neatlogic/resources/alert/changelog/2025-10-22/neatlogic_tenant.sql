CREATE TABLE `alert_event_handler_type`
(
    `id`    bigint NOT NULL,
    `name`  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
    `label` varchar(50) COLLATE utf8mb4_general_ci                       DEFAULT NULL COMMENT '名称',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

ALTER TABLE `alert_event_handler`
    ADD COLUMN `type_id` bigint NULL COMMENT '类型id' AFTER `is_active`;

ALTER TABLE `alert_event_handler`
    ADD COLUMN `is_async` tinyint NULL COMMENT '是否异步，为空则以插件默认值为准' AFTER `type_id`;