CREATE TABLE `alert_parent_uniquekey`
(
    `alert_id`   bigint                                                    NOT NULL,
    `unique_key` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    PRIMARY KEY (`unique_key`) USING BTREE,
    KEY `idx_alert_id` (`alert_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

ALTER TABLE `alert_attrtype`
    ADD COLUMN `is_index` tinyint NULL COMMENT '是否写入ES' AFTER `is_tab`;

insert ignore into alert_parent_uniquekey (alert_id, unique_key)
SELECT distinct a.from_alert_id,
                b.unique_key
FROM alert_rel a
         JOIN alert b ON a.from_alert_id = b.id AND b.is_close = 0
order by a.from_alert_id;


update `alert_attrtype`
set is_index = 1
where is_index is null;