package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertActionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AlertActionMapper {
    AlertActionVo getAlertActionById(Long id);

    AlertActionVo getAlertActionByName(String name);

    int checkAlertActionNameIsExists(AlertActionVo alertActionVo);

    List<AlertActionVo> searchAlertAction(AlertActionVo alertActionVo);

    void updateAlertAction(AlertActionVo alertActionVo);

    void insertAlertAction(AlertActionVo alertActionVo);

    void insertAlertActionRel(@Param("alertId") Long alertId, @Param("actionName") String actionName);

    void deleteAlertActionRel(@Param("alertId") Long alertId, @Param("actionName") String actionName);
}
