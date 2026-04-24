package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertTrashVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AlertTrashMapper {
    AlertTrashVo getAlertTrashById(Long id);

    List<AlertTrashVo> searchAlertTrash(AlertTrashVo vo);

    List<AlertTrashVo> getAlertTrashByIdList(AlertTrashVo alertTrashVo);

    void insertAlertTrash(AlertTrashVo alertTrashVo);

    void saveAlertTrashAttr(AlertTrashVo alertTrashVo);

    List<Long> getAlertTrashIdListByDayBefore(int dayBefore);

    void deleteAlertTrashAttrByIdList(@Param("idList") List<Long> idList);

    void deleteAlertTrashByIdList(@Param("idList") List<Long> idList);
}
