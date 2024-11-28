package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.OriginalAlertVo;

public interface AlertMapper {
    AlertVo getAlertByUniqueKey(String uniqueKey);

    void updateAlert(AlertVo alertVo);

    void insertAlert(AlertVo alertVo);

    void insertAlertError(OriginalAlertVo originalAlertVo);
}
