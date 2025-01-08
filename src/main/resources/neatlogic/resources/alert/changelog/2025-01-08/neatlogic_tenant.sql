CREATE TABLE `alert_type_attrtype`
(
    `alerttype_id` bigint NOT NULL,
    `attrtype_id`  bigint NOT NULL,
    `sort`         int DEFAULT NULL,
    PRIMARY KEY (`alerttype_id`, `attrtype_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;