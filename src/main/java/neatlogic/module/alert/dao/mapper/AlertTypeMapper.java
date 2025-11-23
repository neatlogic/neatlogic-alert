package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.crossover.IAlertTypeCrossoverMapper;
import neatlogic.framework.alert.dto.AlertTypeAdaptorVo;
import neatlogic.framework.alert.dto.AlertTypeVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AlertTypeMapper extends IAlertTypeCrossoverMapper {
    int checkAlertTypeNameIsExists(AlertTypeVo alertTypeVo);

    AlertTypeVo getAlertTypeById(Long id);

    AlertTypeVo getAlertTypeByName(String name);

    List<AlertTypeVo> searchAlertType(AlertTypeVo alertTypeVo);

    List<AlertTypeAdaptorVo> getAlertAdaptorNameList();

    int searchAlertTypeCount(AlertTypeVo alertTypeVo);

    void insertAlertType(AlertTypeVo alertTypeVo);

    void insertAlertTypeAttrType(@Param("alertTypeId") Long alertTypeId, @Param("attrTypeId") Long attrTypeId, @Param("sort") int sort);

    void insertAlertTypeAdaptor(AlertTypeAdaptorVo alertTypeAdaptorVo);

    void updateAlertType(AlertTypeVo alertTypeVo);

    void deleteAlertTypeById(Long id);

    void deleteAlertTypeAdaptorByAlertTypeId(Long id);

    void deleteAlertTypeAttrTypeByAlertTypeId(Long id);
}
