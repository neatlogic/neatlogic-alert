package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertEventHandlerTypeVo;

import java.util.List;

public interface AlertEventHandlerTypeMapper {
    int checkAlertEventHandlerTypeIsInUsed(Long id);

    int checkAlertEventHandlerTypeNameIsExists(AlertEventHandlerTypeVo alertEventHandlerTypeVo);

    List<AlertEventHandlerTypeVo> searchAlertEventHandlerType(AlertEventHandlerTypeVo vo);

    void insertAlertEventHandlerType(AlertEventHandlerTypeVo vo);

    void updateAlertEventHandlerType(AlertEventHandlerTypeVo vo);

    void deleteAlertEventHandlerType(Long id);
}
