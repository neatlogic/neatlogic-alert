package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertLevelVo;

import java.util.List;

public interface AlertLevelMapper {
    int checkAlertLevelIsExists(AlertLevelVo alertLevel);

    int checkAlertLevelNameIsExists(AlertLevelVo alertLevel);

    AlertLevelVo getAlertLevelById(Long id);

    AlertLevelVo getAlertLevelByLevel(Integer level);

    List<AlertLevelVo> listAlertLevel();

    void saveAlertLevel(AlertLevelVo alertLevel);

    void deleteAlertLevel(Long id);
}
