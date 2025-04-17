ALTER TABLE `alert`
    MODIFY COLUMN `title` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '标题' AFTER `level`;