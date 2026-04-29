package neatlogic.module.alert.api.breaker;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dto.breaker.AlertBreakerPolicyVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAlertBreakerPolicyApi extends PrivateApiComponentBase {
    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    @Override
    public String getToken() {
        return "/alert/breaker/policy/get";
    }

    @Override
    public String getName() {
        return "获取告警熔断策略";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG, isRequired = true)
    })
    @Description(desc = "获取告警熔断策略")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        Long id = jsonObj.getLong("id");
        AlertBreakerPolicyVo policyVo = alertBreakerMapper.getAlertBreakerPolicyById(id);
        if (policyVo != null) {
            policyVo.getHandlerLabel();
            policyVo.getConfig();
        }
        return policyVo;
    }
}
