package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertNotifyTemplateVo;

import java.util.List;

public interface AlertNotifyTemplateMapper {
    int checkNotifyTemplateNameIsExists(AlertNotifyTemplateVo alertNotifyTemplateVo);

    AlertNotifyTemplateVo getNotifyTemplateById(Long id);

    List<AlertNotifyTemplateVo> searchNotifyTemplate(AlertNotifyTemplateVo alertNotifyTemplateVo);

    int searchNotifyTemplateCount(AlertNotifyTemplateVo alertNotifyTemplateVo);

    void updateNotifyTemplate(AlertNotifyTemplateVo alertNotifyTemplateVo);

    void insertNotifyTemplate(AlertNotifyTemplateVo alertNotifyTemplateVo);

    void deleteNotifyTemplateById(Long id);
}
