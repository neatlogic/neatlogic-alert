package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertAuditVo;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditSearchVo;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;

import java.util.List;

public interface AlertAuditMapper {
    AlertEventHandlerAuditVo getAlertEventAuditById(Long id);

    /**
     * 按父审计记录ID批量查询直接子记录。
     */
    List<AlertEventHandlerAuditVo> listAlertEventAuditByParentIdList(List<Long> parentIdList);

    /**
     * 统计满足条件的根审计记录数量。
     */
    int searchRootAlertEventAuditCount(AlertEventHandlerAuditSearchVo searchVo);

    /**
     * 分页查询满足条件的根审计记录。
     */
    List<AlertEventHandlerAuditVo> searchRootAlertEventAudit(AlertEventHandlerAuditSearchVo searchVo);

    int selectAlertAuditCount(AlertAuditVo alertAudit);

    List<AlertAuditVo> selectAlertAudit(AlertAuditVo alertAuditVo);

    void insertAlertAudit(AlertAuditVo alertAuditVo);

    List<AlertEventHandlerAuditVo> searchAlertEventAudit(AlertEventHandlerAuditVo alertEventHandlerAuditVo);
}
