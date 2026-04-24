package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertAllAlertConfigVo;

public interface AlertAllAlertConfigMapper {
    AlertAllAlertConfigVo getAlertAllAlertConfigByName(String name);

    void saveAlertAllAlertConfig(AlertAllAlertConfigVo alertAllAlertConfigVo);
}
