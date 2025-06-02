ALTER TABLE `alert`
    ADD COLUMN `delete_batch` bigint NULL COMMENT '删除批次' AFTER `is_delete`,
    ADD INDEX `idx_delete_batch`(`delete_batch`) USING BTREE;