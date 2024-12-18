package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertRelVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.OriginalAlertVo;

import java.util.List;

public interface AlertMapper {
    List<AlertVo> searchAlert(AlertVo alertVo);

    List<AlertVo> selectAlertById(AlertVo alertVo);

    AlertVo getAlertById(Long id);

    AlertVo getAlertByUniqueKey(String uniqueKey);

    void updateAlert(AlertVo alertVo);

    void saveAlertRel(AlertRelVo alertRelVo);

    void insertAlert(AlertVo alertVo);

    void saveAlertAttr(AlertVo alertVo);

    void insertAlertOrigin(OriginalAlertVo originalAlertVo);

    void deleteAlertAttr(Long alertId);

}
