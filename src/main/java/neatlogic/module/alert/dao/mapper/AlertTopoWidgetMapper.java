package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertTopoWidgetVo;

import java.util.List;

public interface AlertTopoWidgetMapper {
    AlertTopoWidgetVo getWidgetByName(String name);

    int checkWidgetNameIsExists(AlertTopoWidgetVo alertTopoWidgetVo);

    AlertTopoWidgetVo getWidgetById(Long id);

    List<AlertTopoWidgetVo> listWidget(AlertTopoWidgetVo alertTopoWidgetVo);

    List<AlertTopoWidgetVo> searchWidget(AlertTopoWidgetVo alertTopoWidgetVo);

    int searchWidgetCount(AlertTopoWidgetVo vo);

    void insertWidget(AlertTopoWidgetVo vo);

    void updateWidgetActive(AlertTopoWidgetVo vo);

    void updateWidget(AlertTopoWidgetVo vo);

    void deleteWidgetById(Long id);
}
