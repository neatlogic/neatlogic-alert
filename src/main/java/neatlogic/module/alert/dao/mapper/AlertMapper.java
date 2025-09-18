package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AlertMapper {
    List<Long> getIsDeleteAlertIdList(@Param("id") Long id, @Param("deleteBatch") Long deleteBatch, @Param("limit") Integer limit);

    List<AlertTeamVo> getAlertTeamByAlertId(Long alertId);

    List<AlertUserVo> getAlertUserByAlertId(Long alertId);

    List<AlertVo> getOpenAlertByParentId(Long parentId);

    List<AlertVo> getCloseAlertByParentId(Long parentId);

    List<AlertVo> getOpenAlertByUniqueKey(String uniqueKey);

    List<AlertVo> getCloseAlertByUniqueKey(String uniqueKey);

    AlertIntervalJobVo getAlertIntervalJob(@Param("alertId") Long alertId, @Param("alertEventHandlerId") Long alertEventHandlerId);

    List<OriginalAlertVo> getAlertOriginByIdList(OriginalAlertVo originalAlertVo);

    List<OriginalAlertVo> searchAlertOrigin(OriginalAlertVo alertOriginVo);

    int searchAlertOriginCount(OriginalAlertVo alertOriginVo);

    List<Long> listToAlertIdByFromAlertId(Long fromAlertId);

    List<Long> listAllFromAlertIdByToAlertId(Long toAlertId);

    List<Long> listAllToAlertIdByFromAlertId(Long fromAlertId);

    int checkAlertIsExists(Long id);

    List<AlertVo> getAlertByParentId(Long parentId);

    OriginalAlertVo getAlertOriginById(Long id);

    List<AlertVo> searchAlert(AlertVo alertVo);

    List<AlertVo> getAlertByIdList(AlertVo alertVo);

    AlertVo getParentAlertByAlertId(Long alertId);

    AlertVo getAlertById(Long id);

    Long getFirstOpenAlertIdByUniqueKey(String uniqueKey);

    List<AlertIntervalJobVo> searchAlertIntervalJob(AlertIntervalJobVo alertIntervalJobVo);

    List<Long> getNotUsedAlertOriginIdByDayBefore(int day);


    void updateAlertIsDeleteByIdList(AlertVo alertVo);

    void updateAlert(AlertVo alertVo);

    void updateAlertUpdateTime(AlertVo alertVo);

    int updateAlertIsClose(@Param("alertId") Long alertId, @Param("isClose") Integer isClose);

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

    void deleteAlertRel(@Param("fromAlertId") Long fromAlertId, @Param("toAlertId") Long toAlertId);

    void deleteAlertIntervalJob(@Param("alertId") Long alertId, @Param("alertEventHandlerId") Long alertEventHandlerId);

    void deleteAlertOriginByIdList(@Param("idList") List<Long> idList);

}
