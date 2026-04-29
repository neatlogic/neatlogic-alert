package neatlogic.module.alert.api.breaker;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BREAKER_MODIFY;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.exception.breaker.AlertBreakerPolicyIsInUsedException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BREAKER_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteAlertBreakerPolicyApi extends PrivateApiComponentBase {
    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    @Override
    public String getToken() {
        return "/alert/breaker/policy/delete";
    }

    @Override
    public String getName() {
        return "删除告警熔断策略";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG, isRequired = true)
    })
    @Description(desc = "删除告警熔断策略")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        Long id = jsonObj.getLong("id");
        if (alertBreakerMapper.checkAlertBreakerPolicyIsInUsed(id) > 0) {
            throw new AlertBreakerPolicyIsInUsedException();
        }
        alertBreakerMapper.deleteAlertBreakerPolicy(id);
        return null;
    }
}
