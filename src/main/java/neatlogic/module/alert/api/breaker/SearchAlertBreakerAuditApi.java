package neatlogic.module.alert.api.breaker;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dto.breaker.AlertBreakerAuditVo;
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
public class SearchAlertBreakerAuditApi extends PrivateApiComponentBase {
    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    @Override
    public String getToken() {
        return "/alert/breaker/audit/search";
    }

    @Override
    public String getName() {
        return "搜索告警熔断执行审计";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "policyId", desc = "策略id", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "currentPage", desc = "common.currentpage", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "common.pagesize", type = ApiParamType.INTEGER)
    })
    @Description(desc = "搜索告警熔断执行审计")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertBreakerAuditVo auditVo = JSON.toJavaObject(jsonObj, AlertBreakerAuditVo.class);
        int rowNum = alertBreakerMapper.searchAlertBreakerAuditCount(auditVo);
        if (rowNum > 0) {
            auditVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(alertBreakerMapper.searchAlertBreakerAudit(auditVo), auditVo);
    }
}
