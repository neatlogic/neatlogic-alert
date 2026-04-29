package neatlogic.module.alert.api.breaker;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BREAKER_MODIFY;
import neatlogic.framework.alert.breaker.AlertBreakerHandlerFactory;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dto.breaker.AlertBreakerPolicyVo;
import neatlogic.framework.alert.exception.breaker.AlertBreakerHandlerNotFoundException;
import neatlogic.framework.alert.exception.breaker.AlertBreakerPolicyNameIsExistsException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BREAKER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertBreakerPolicyApi extends PrivateApiComponentBase {
    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    @Override
    public String getToken() {
        return "/alert/breaker/policy/save";
    }

    @Override
    public String getName() {
        return "保存告警熔断策略";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "名称", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "handler", desc = "熔断策略插件", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "config", desc = "配置", type = ApiParamType.JSONOBJECT),
            @Param(name = "isActive", desc = "是否激活", type = ApiParamType.INTEGER, isRequired = true),
            @Param(name = "description", desc = "说明", type = ApiParamType.STRING)
    })
    @Output({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG)
    })
    @Description(desc = "保存告警熔断策略")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        Long id = jsonObj.getLong("id");
        AlertBreakerPolicyVo policyVo = JSON.toJavaObject(jsonObj, AlertBreakerPolicyVo.class);
        if (AlertBreakerHandlerFactory.getHandler(policyVo.getHandler()) == null) {
            throw new AlertBreakerHandlerNotFoundException(policyVo.getHandler());
        }
        if (alertBreakerMapper.checkAlertBreakerPolicyNameIsExists(policyVo) > 0) {
            throw new AlertBreakerPolicyNameIsExistsException(policyVo.getName());
        }
        if (id == null) {
            alertBreakerMapper.insertAlertBreakerPolicy(policyVo);
        } else {
            alertBreakerMapper.updateAlertBreakerPolicy(policyVo);
        }
        return policyVo.getId();
    }
}
