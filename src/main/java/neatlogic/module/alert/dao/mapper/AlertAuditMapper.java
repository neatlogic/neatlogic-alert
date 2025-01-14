package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertAuditVo;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;

import java.util.List;

public interface AlertAuditMapper {
    AlertEventHandlerAuditVo getAlertEventAuditById(Long id);

    int selectAlertAuditCount(AlertAuditVo alertAudit);

    List<AlertAuditVo> selectAlertAudit(AlertAuditVo alertAuditVo);

    void insertAlertAudit(AlertAuditVo alertAuditVo);

    List<AlertEventHandlerAuditVo> searchAlertEventAudit(AlertEventHandlerAuditVo alertEventHandlerAuditVo);
}
