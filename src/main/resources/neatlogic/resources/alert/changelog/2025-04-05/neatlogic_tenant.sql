CREATE TABLE `alert_rule` (
                              `id` bigint NOT NULL COMMENT 'id',
                              `attr_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '属性唯一标识',
                              `name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
                              `label` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
                              `is_active` tinyint DEFAULT NULL COMMENT '是否激活',
                              `config` text COLLATE utf8mb4_general_ci COMMENT '配置',
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;