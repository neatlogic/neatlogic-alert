package neatlogic.module.alert.api.breaker;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.breaker.AlertBreakerHandlerFactory;
import neatlogic.framework.alert.breaker.IAlertBreakerHandler;
import neatlogic.framework.alert.dto.breaker.AlertBreakerHandlerVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertBreakerHandlerApi extends PrivateApiComponentBase {
    @Override
    public String getToken() {
        return "/alert/breaker/handler/list";
    }

    @Override
    public String getName() {
        return "获取告警熔断策略插件列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "获取告警熔断策略插件列表")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        List<AlertBreakerHandlerVo> resultList = new ArrayList<>();
        for (IAlertBreakerHandler handler : AlertBreakerHandlerFactory.getHandlerList()) {
            resultList.add(new AlertBreakerHandlerVo(handler.getName(), handler.getLabel(), handler.getDescription()));
        }
        return resultList;
    }
}
