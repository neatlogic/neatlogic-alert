package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertViewAuthVo;
import neatlogic.framework.alert.dto.AlertViewVo;

import java.util.List;

public interface AlertViewMapper {
    int checkAlertViewIsExists(AlertViewVo alertViewVo);

    AlertViewVo getAlertViewById(Long id);

    AlertViewVo getAlertViewByName(String name);


    List<AlertViewVo> searchAlertView(AlertViewVo alertViewVo);

    List<AlertViewVo> listAlertView(AlertViewVo alertViewVo);

    int searchAlertViewCount(AlertViewVo alertViewVo);

    void insertAlertViewAuth(AlertViewAuthVo alertViewAuthVo);

    void saveAlertView(AlertViewVo alertViewVo);

    void deleteAlertViewAuthByViewId(Long viewId);

    void updateAlertViewSort(AlertViewVo alertViewVo);

    void deleteAlertViewById(Long viewId);
}
