package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertAuditVo;

import java.util.List;

public interface AlertAuditMapper {
    int selectAlertAuditCount(AlertAuditVo alertAudit);

    List<AlertAuditVo> selectAlertAudit(AlertAuditVo alertAuditVo);

    void insertAlertAudit(AlertAuditVo alertAuditVo);
}
