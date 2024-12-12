package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertViewAuthVo;
import neatlogic.framework.alert.dto.AlertViewVo;

import java.util.List;

public interface AlertViewMapper {

    AlertViewVo getAlertViewById(Long id);

    AlertViewVo getAlertViewByName(String name);

    List<AlertViewVo> listAlertView(AlertViewVo alertViewVo);

    void insertAlertViewAuth(AlertViewAuthVo alertViewAuthVo);

    void saveAlertView(AlertViewVo alertViewVo);

    void deleteAlertViewAuthByViewId(Long viewId);
}
