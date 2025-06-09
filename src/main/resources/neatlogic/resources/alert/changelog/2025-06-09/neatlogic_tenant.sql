ALTER TABLE `alert_origin`
    MODIFY COLUMN `status` enum ('succeed','failed','ignored') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否上报成功' AFTER `source`;