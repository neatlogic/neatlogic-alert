package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertTopoVo;

import java.util.List;

public interface AlertTopoMapper {
    AlertTopoVo getAlertTopoById(Long id);

    int checkTopoNameIsExists(AlertTopoVo alertTopo);

    List<AlertTopoVo> searchAlertTopo(AlertTopoVo alertTopoVo);

    List<AlertTopoVo> listAlertTopo(AlertTopoVo alertTopoVo);

    int searchAlertTopoCount(AlertTopoVo alertTopoVo);

    void updateAlertTopo(AlertTopoVo alertTopoVo);

    void insertAlertTopo(AlertTopoVo alertTopoVo);
}
