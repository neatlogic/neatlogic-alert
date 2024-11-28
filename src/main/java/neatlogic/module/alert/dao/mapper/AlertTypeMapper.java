package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertTypeVo;

import java.util.List;

public interface AlertTypeMapper {
    int checkAlertTypeNameIsExists(AlertTypeVo alertTypeVo);

    AlertTypeVo getAlertTypeById(Long id);

    AlertTypeVo getAlertTypeByName(String name);

    List<AlertTypeVo> searchAlertType(AlertTypeVo alertTypeVo);

    int searchAlertTypeCount(AlertTypeVo alertTypeVo);

    void insertAlertType(AlertTypeVo alertTypeVo);

    void updateAlertType(AlertTypeVo alertTypeVo);

    void deleteAlertTypeById(Long id);
}
