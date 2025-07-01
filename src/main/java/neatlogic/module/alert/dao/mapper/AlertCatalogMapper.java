package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertCatalogAuthVo;
import neatlogic.framework.alert.dto.AlertCatalogVo;

import java.util.List;

public interface AlertCatalogMapper {
    AlertCatalogVo getAlertCatalogById(Long id);

    int checkAlertCatalogIsExists(AlertCatalogVo alertCatalog);

    int checkAlertCatalogIsInUsed(Long catalogId);

    void saveAlertCatalog(AlertCatalogVo alertCatalogVo);

    void insertAlertCatalogAuth(AlertCatalogAuthVo alertCatalogAuthVo);

    void deleteAlertCatalogAuthByCatalogId(Long catalogId);

    List<AlertCatalogVo> searchAlertCatalog(AlertCatalogVo alertCatalogVo);

    int searchAlertCatalogCount(AlertCatalogVo AlertCatalogVo);

    List<AlertCatalogVo> listAlertCatalog(AlertCatalogVo alertCatalogVo);

    void updateAlertCatalogSort(AlertCatalogVo alertCatalogVo);

    void deleteAlertCatalogById(Long id);
}
