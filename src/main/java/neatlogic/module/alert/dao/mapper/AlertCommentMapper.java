package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertCommentVo;

import java.util.List;

public interface AlertCommentMapper {
    int searchAlertCommentCount(AlertCommentVo alertCommentVo);

    List<AlertCommentVo> searchAlertComment(AlertCommentVo alertCommentVo);

    List<AlertCommentVo> getAlertCommentByAlertId(Long alertId);

    void insertAlertComment(AlertCommentVo alertCommentVo);
}
