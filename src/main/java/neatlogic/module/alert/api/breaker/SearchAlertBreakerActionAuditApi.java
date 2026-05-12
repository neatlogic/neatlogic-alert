package neatlogic.module.alert.api.breaker;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dto.breaker.AlertBreakerActionAuditVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertBreakerActionAuditApi extends PrivateApiComponentBase {
    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    @Override
    public String getToken() {
        return "/alert/breaker/action/audit/search";
    }

    @Override
    public String getName() {
        return "搜索告警熔断动作执行审计";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "policyId", desc = "策略id", type = ApiParamType.LONG),
            @Param(name = "stateId", desc = "状态id", type = ApiParamType.LONG),
            @Param(name = "trigger", desc = "触发点", type = ApiParamType.STRING),
            @Param(name = "actionHandler", desc = "动作插件", type = ApiParamType.STRING),
            @Param(name = "status", desc = "状态", type = ApiParamType.STRING),
            @Param(name = "currentPage", desc = "common.currentpage", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "common.pagesize", type = ApiParamType.INTEGER)
    })
    @Description(desc = "搜索告警熔断动作执行审计")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertBreakerActionAuditVo auditVo = JSON.toJavaObject(jsonObj, AlertBreakerActionAuditVo.class);
        int rowNum = alertBreakerMapper.searchAlertBreakerActionAuditCount(auditVo);
        if (rowNum > 0) {
            auditVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(alertBreakerMapper.searchAlertBreakerActionAudit(auditVo), auditVo);
    }
}
