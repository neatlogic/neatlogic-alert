CREATE TABLE `alert` (
                         `id` bigint NOT NULL COMMENT 'id',
                         `level` int DEFAULT NULL COMMENT '级别',
                         `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                         `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                         `alert_time` datetime NOT NULL COMMENT '告警时间',
                         `type` bigint DEFAULT NULL COMMENT '类型',
                         `status` enum('new','confirmed','processing','resolved','closed') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '状态',
                         `source` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
                         `is_delete` tinyint DEFAULT NULL COMMENT '是否删除',
                         `unique_key` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一键',
                         `alert_count` int DEFAULT '1' COMMENT '告警次数',
                         `entity_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实体类型，例如OS，DB',
                         `entity_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '实体名称，例如：HOST1234',
                         `ip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IP地址',
                         `port` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '端口',
                         PRIMARY KEY (`id`),
                         KEY `idx_unique_key` (`unique_key`) USING BTREE,
                         KEY `idx_time` (`alert_time`) USING BTREE,
                         KEY `idx_updatetime` (`update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警内容';

CREATE TABLE `alert_attr` (
                              `alert_id` bigint NOT NULL,
                              `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
                              PRIMARY KEY (`alert_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警额外属性';

CREATE TABLE `alert_attrtype` (
                                  `id` bigint NOT NULL,
                                  `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
                                  `label` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
                                  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型',
                                  `is_active` tinyint DEFAULT NULL COMMENT '是否激活',
                                  `config` text COLLATE utf8mb4_general_ci COMMENT '配置',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警自定义属性类型';

CREATE TABLE `alert_audit` (
                               `id` bigint NOT NULL,
                               `alert_id` bigint DEFAULT NULL COMMENT '告警id',
                               `input_user` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户Id',
                               `input_from` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源',
                               `input_time` timestamp NULL DEFAULT NULL COMMENT '时间',
                               `attr_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '属性',
                               `old_value` longtext COLLATE utf8mb4_general_ci COMMENT '旧值',
                               `new_value` longtext COLLATE utf8mb4_general_ci COMMENT '新值',
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警操作审计';

CREATE TABLE `alert_comment` (
                                 `id` bigint NOT NULL COMMENT 'id',
                                 `alert_id` bigint DEFAULT NULL COMMENT '告警id',
                                 `comment` longtext COLLATE utf8mb4_general_ci COMMENT '评论',
                                 `comment_time` timestamp NULL DEFAULT NULL COMMENT '评论时间',
                                 `comment_user` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '评论用户',
                                 PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `alert_event_handler` (
                                       `id` bigint NOT NULL COMMENT 'id',
                                       `uuid` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'uuid',
                                       `parent_id` bigint DEFAULT NULL COMMENT '父处理器id',
                                       `alert_type` bigint DEFAULT NULL COMMENT '告警类型',
                                       `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
                                       `handler` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '处理器',
                                       `event` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '事件',
                                       `sort` int DEFAULT NULL COMMENT '排序，如果排序相同代表并行',
                                       `config` longtext COLLATE utf8mb4_general_ci COMMENT '配置',
                                       `is_active` tinyint DEFAULT NULL COMMENT '是否激活',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_uuid` (`uuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `alert_event_handler_data` (
                                            `alert_id` bigint NOT NULL COMMENT '告警id',
                                            `alert_event_handler_id` bigint NOT NULL COMMENT '事件id',
                                            `handler` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '子组件类型',
                                            `data` text COLLATE utf8mb4_general_ci COMMENT '数据',
                                            PRIMARY KEY (`alert_id`,`alert_event_handler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `alert_level` (
                               `level` int NOT NULL COMMENT '级别',
                               `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
                               `label` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
                               `color` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '颜色',
                               `id` bigint NOT NULL COMMENT 'id',
                               PRIMARY KEY (`id`) USING BTREE,
                               UNIQUE KEY `uk` (`level`) USING BTREE,
                               UNIQUE KEY `ukname` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `alert_origin` (
                                `id` bigint NOT NULL,
                                `content` longtext COLLATE utf8mb4_general_ci,
                                `type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '类型',
                                `time` timestamp NULL DEFAULT NULL,
                                `error` longtext COLLATE utf8mb4_general_ci,
                                `status` enum('succeed','failed') COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否上报成功',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `alert_rel` (
                             `from_alert_id` bigint NOT NULL,
                             `to_alert_id` bigint NOT NULL,
                             PRIMARY KEY (`from_alert_id`,`to_alert_id`),
                             KEY `idx_to_alert_id` (`to_alert_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `alert_tag` (
                             `alert_id` bigint DEFAULT NULL,
                             `tag_hash` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签做散列',
                             `tag` text COLLATE utf8mb4_general_ci COMMENT '标签值',
                             KEY `idx_alert_id` (`alert_id`) USING BTREE,
                             KEY `idx_alert_tag` (`tag_hash`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `alert_type` (
                              `id` bigint NOT NULL,
                              `name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
                              `label` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
                              `is_active` tinyint DEFAULT NULL COMMENT '是否激活',
                              `file_id` bigint DEFAULT NULL COMMENT '插件id',
                              `fcd` timestamp NULL DEFAULT NULL,
                              `fcu` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
                              `lcd` timestamp NULL DEFAULT NULL,
                              `lcu` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='告警类型';

CREATE TABLE `alert_view` (
                              `id` bigint NOT NULL COMMENT 'id',
                              `name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
                              `label` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
                              `config` text COLLATE utf8mb4_general_ci COMMENT '配置',
                              `is_active` tinyint DEFAULT NULL COMMENT '是否激活',
                              `fcu` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
                              `fcd` datetime DEFAULT NULL,
                              `lcu` char(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
                              `lcd` datetime DEFAULT NULL,
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `alert_view_auth` (
                                   `view_id` bigint NOT NULL,
                                   `auth_type` enum('user','team','role','common') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                   `auth_uuid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                                   PRIMARY KEY (`view_id`,`auth_type`,`auth_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
