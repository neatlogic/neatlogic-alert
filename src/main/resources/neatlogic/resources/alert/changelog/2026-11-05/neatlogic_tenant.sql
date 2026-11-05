-- 统一告警时间字段类型，保留原有空值、默认值和注释，不增加自动更新时间。
-- 执行前确认存量时间均在 TIMESTAMP 支持范围内，且迁移连接与应用连接使用相同的时区。
ALTER TABLE `alert`
    MODIFY COLUMN `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间';

ALTER TABLE `alert`
    MODIFY COLUMN `alert_time` timestamp NOT NULL COMMENT '告警时间';

ALTER TABLE `alert`
    MODIFY COLUMN `close_time` timestamp NULL DEFAULT NULL COMMENT '关闭时间';

ALTER TABLE `alert_trash`
    MODIFY COLUMN `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间';

ALTER TABLE `alert_trash`
    MODIFY COLUMN `delete_time` timestamp NULL DEFAULT NULL COMMENT '删除时间';

ALTER TABLE `alert_trash`
    MODIFY COLUMN `alert_time` timestamp NOT NULL COMMENT '告警时间';

ALTER TABLE `alert_view`
    MODIFY COLUMN `fcd` timestamp NULL DEFAULT NULL;

ALTER TABLE `alert_view`
    MODIFY COLUMN `lcd` timestamp NULL DEFAULT NULL;

ALTER TABLE `alert_allalert_config`
    MODIFY COLUMN `fcd` timestamp NULL DEFAULT NULL;

ALTER TABLE `alert_allalert_config`
    MODIFY COLUMN `lcd` timestamp NULL DEFAULT NULL;
