package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.*;

import java.util.List;

public interface AlertMapper {
    List<Long> listToAlertIdByFromAlertId(Long fromAlertId);

    List<Long> listAllToAlertIdByFromAlertId(Long fromAlertId);

    int checkAlertIsExists(Long id);

    List<AlertVo> getAlertByParentId(Long parentId);

    AlertOriginVo getAlertOriginById(Long id);

    List<AlertVo> searchAlert(AlertVo alertVo);

    List<AlertVo> getAlertByIdList(AlertVo alertVo);

    AlertVo getAlertById(Long id);

    AlertVo getAlertByUniqueKey(String uniqueKey);

    void updateAlertUpdateTime(AlertVo alertVo);

    void updateAlertStatus(AlertVo alertVo);

    void saveAlertRel(AlertRelVo alertRelVo);

    void insertAlert(AlertVo alertVo);

    void saveAlertAttr(AlertVo alertVo);

    void insertAlertOrigin(OriginalAlertVo originalAlertVo);

    void insertAlertUser(AlertUserVo alertUserVo);

    void insertAlertTeam(AlertTeamVo alertTeamVo);

    void deleteAlertById(Long alertId);

    void deleteAlertAttr(Long alertId);

}
