package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertAttrTypeEnumVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;

import java.util.List;

public interface AlertAttrTypeMapper {
    int checkAttrTypeNameIsExists(AlertAttrTypeVo alertAttrTypeVo);

    AlertAttrTypeVo getAttrTypeById(Long id);

    AlertAttrTypeVo getAttrTypeByName(String name);

    int searchAttrTypeCount(AlertAttrTypeVo alertAttrTypeVo);

    List<AlertAttrTypeEnumVo> searchAttrTypeEnum(AlertAttrTypeEnumVo alertAttrTypeEnumVo);

    List<AlertAttrTypeVo> searchAttrType(AlertAttrTypeVo alertAttrTypeVo);

    List<AlertAttrTypeVo> listAttrType();

    void saveAlertAttrTypeEnum(AlertAttrTypeEnumVo alertAttrTypeEnumVo);

    void saveAlertAttrType(AlertAttrTypeVo alertAttrTypeVo);

    void deleteAttrTypeById(Long id);
}
