package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AlertMapper {

    AlertIntervalJobVo getAlertIntervalJob(@Param("alertId") Long alertId, @Param("alertEventHandlerId") Long alertEventHandlerId);

    List<OriginalAlertVo> searchAlertOrigin(OriginalAlertVo alertOriginVo);

    int searchAlertOriginCount(OriginalAlertVo alertOriginVo);

    List<Long> listToAlertIdByFromAlertId(Long fromAlertId);

    List<Long> listAllToAlertIdByFromAlertId(Long fromAlertId);

    int checkAlertIsExists(Long id);

    List<AlertVo> getAlertByParentId(Long parentId);

    OriginalAlertVo getAlertOriginById(Long id);

    List<AlertVo> searchAlert(AlertVo alertVo);

    List<AlertVo> getAlertByIdList(AlertVo alertVo);

    AlertVo getAlertById(Long id);

    AlertVo getAlertByUniqueKey(String uniqueKey);

    List<AlertIntervalJobVo> searchAlertIntervalJob(AlertIntervalJobVo alertIntervalJobVo);

    void updateAlertUpdateTime(AlertVo alertVo);

    void updateAlertIntervalJob(AlertIntervalJobVo alertIntervalJobVo);

    void updateAlertStatus(AlertVo alertVo);

    void saveAlertRel(AlertRelVo alertRelVo);

    void insertAlert(AlertVo alertVo);

    void saveAlertAttr(AlertVo alertVo);

    void insertAlertOrigin(OriginalAlertVo originalAlertVo);

    void insertAlertUser(AlertUserVo alertUserVo);

    void insertAlertTeam(AlertTeamVo alertTeamVo);

    void insertAlertIntervalJob(AlertIntervalJobVo alertIntervalJobVo);

    void deleteAlertById(Long alertId);

    void deleteAlertAttr(Long alertId);

    void deleteAlertUserByAlertId(Long alertId);

    void deleteAlertTeamByAlertId(Long alertId);

    void deleteAlertIntervalJob(@Param("alertId") Long alertId, @Param("alertEventHandlerId") Long alertEventHandlerId);
}
