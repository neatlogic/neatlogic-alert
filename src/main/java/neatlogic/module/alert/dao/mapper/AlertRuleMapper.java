package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertRuleVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AlertRuleMapper {
    int checkAlertRuleIsExists(AlertRuleVo alertRuleVo);

    AlertRuleVo getAlertRuleById(Long id);

    List<AlertRuleVo> getAlertRuleByIdList(@Param("idList") List<Long> idList);

    List<AlertRuleVo> listAllAlertRule(AlertRuleVo alertRuleVo);

    void updateAlertRule(AlertRuleVo alertRuleVo);

    void insertAlertRule(AlertRuleVo alertRuleVo);

    void deleteAlertRuleById(Long id);
}
