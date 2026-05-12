ALTER TABLE `alert_breaker_state`
  ADD COLUMN `event_handler_id` bigint DEFAULT NULL COMMENT '事件插件id' AFTER `policy_id`;

ALTER TABLE `alert_breaker_state`
  ADD KEY `idx_event_handler_id` (`event_handler_id`);
