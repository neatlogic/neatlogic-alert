package neatlogic.module.alert.api.breaker;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.breaker.action.AlertBreakerActionHandlerFactory;
import neatlogic.framework.alert.breaker.action.IAlertBreakerActionHandler;
import neatlogic.framework.alert.dto.breaker.AlertBreakerActionHandlerVo;
import neatlogic.framework.alert.enums.AlertBreakerActionTrigger;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertBreakerActionHandlerApi extends PrivateApiComponentBase {
    @Override
    public String getToken() {
        return "/alert/breaker/action/handler/list";
    }

    @Override
    public String getName() {
        return "获取告警熔断动作插件列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "获取告警熔断动作插件列表")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        List<AlertBreakerActionHandlerVo> resultList = new ArrayList<>();
        for (IAlertBreakerActionHandler handler : AlertBreakerActionHandlerFactory.getHandlerList()) {
            List<String> triggerList = handler.supportTrigger().stream().map(AlertBreakerActionTrigger::getValue).collect(Collectors.toList());
            resultList.add(new AlertBreakerActionHandlerVo(handler.getName(), handler.getLabel(), handler.getDescription(), triggerList));
        }
        return resultList;
    }
}
