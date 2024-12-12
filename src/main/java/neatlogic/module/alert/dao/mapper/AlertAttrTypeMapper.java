package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertAttrTypeVo;

import java.util.List;

public interface AlertAttrTypeMapper {
    int checkAttrTypeNameIsExists(AlertAttrTypeVo alertAttrTypeVo);

    AlertAttrTypeVo getAttrTypeById(Long id);

    int searchAttrTypeCount(AlertAttrTypeVo alertAttrTypeVo);

    List<AlertAttrTypeVo> searchAttrType(AlertAttrTypeVo alertAttrTypeVo);

    List<AlertAttrTypeVo> listAttrType();

    void saveAlertAttrType(AlertAttrTypeVo alertAttrTypeVo);

    void deleteAttrTypeById(Long id);
}
