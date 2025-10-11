package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertMarkVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AlertMarkMapper {
    List<AlertMarkVo> listAlertMarkByNameList(@Param("nameList") List<String> nameList);

    List<AlertMarkVo> searchAlertMark(AlertMarkVo vo);

    List<String> getAlertMarkNameByAlertId(Long alertId);

    List<AlertMarkVo> getAlertMarkByAlertId(Long alertId);

    void updateAlertMark(AlertMarkVo vo);

    void insertAlertMark(AlertMarkVo vo);

    void insertAlertAlertMark(@Param("alertId") Long alertId, @Param("markUuid") String markUuid);

    void deleteAlertAlertMark(@Param("alertId") Long alertId, @Param("markUuid") String markUuid);

    void deleteAlertAlertMarkByAlertId(Long alertId);
}
