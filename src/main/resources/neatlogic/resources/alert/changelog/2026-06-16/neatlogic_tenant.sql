ALTER TABLE `alert`
    ADD COLUMN `close_time` datetime NULL COMMENT '关闭时间' AFTER `alert_time`;

UPDATE `alert` a
    JOIN (
        SELECT aa.alert_id,
               aa.input_time
        FROM alert_audit aa
                 JOIN (
                     SELECT alert_id,
                            MAX(id) AS id
                     FROM alert_audit
                     WHERE attr_name = 'const_isClose'
                       AND new_value = '[1]'
                     GROUP BY alert_id
                 ) latest_close
                      ON aa.alert_id = latest_close.alert_id
                          AND aa.id = latest_close.id
    ) close_audit
    ON a.id = close_audit.alert_id
SET a.close_time = close_audit.input_time
WHERE a.is_close = 1;
