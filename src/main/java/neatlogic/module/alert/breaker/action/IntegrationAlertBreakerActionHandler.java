package neatlogic.module.alert.breaker.action;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerActionVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerPolicyVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerStateVo;
import neatlogic.framework.alert.enums.AlertBreakerActionTrigger;
import neatlogic.framework.exception.integration.IntegrationHandlerNotFoundException;
import neatlogic.framework.exception.integration.IntegrationNotFoundException;
import neatlogic.framework.exception.integration.IntegrationUnActiveException;
import neatlogic.framework.integration.core.IIntegrationHandler;
import neatlogic.framework.integration.core.IntegrationHandlerFactory;
import neatlogic.framework.integration.dao.mapper.IntegrationMapper;
import neatlogic.framework.integration.dto.IntegrationVo;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.framework.integration.handler.FrameworkRequestFrom;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class IntegrationAlertBreakerActionHandler extends AlertBreakerActionTemplateBase {
    @Resource
    private IntegrationMapper integrationMapper;

    @Override
    public String getName() {
        return "integration";
    }

    @Override
    public String getLabel() {
        return "调用集成";
    }

    @Override
    public String getDescription() {
        return "在熔断生命周期中调用集成，可用于推送熔断、聚合或恢复信息。";
    }

    @Override
    public Set<AlertBreakerActionTrigger> supportTrigger() {
        return EnumSet.allOf(AlertBreakerActionTrigger.class);
    }

    @Override
    protected void myTriggerOpen(AlertBreakerActionVo actionVo, AlertBreakerPolicyVo policyVo, AlertBreakerStateVo stateVo, AlertVo alertVo) {
        invoke(actionVo, buildSingleAlertTemplateParamObj(alertVo));
    }

    @Override
    protected void myTriggerAggregate(AlertBreakerActionVo actionVo, AlertBreakerPolicyVo policyVo, AlertBreakerStateVo stateVo, List<AlertVo> alertList) {
        invoke(actionVo, buildAggregateTemplateParamObj(alertList));
    }

    @Override
    protected void myTriggerRecover(AlertBreakerActionVo actionVo, AlertBreakerPolicyVo policyVo, AlertBreakerStateVo stateVo, List<AlertVo> alertList) {
        invoke(actionVo, buildSingleAlertTemplateParamObj(getLastAlert(alertList)));
    }

    private void invoke(AlertBreakerActionVo actionVo, JSONObject paramObj) {
        JSONObject config = actionVo.getConfig();
        String integrationUuid = config.getString("integrationUuid");
        if (StringUtils.isBlank(integrationUuid)) {
            return;
        }
        IntegrationVo integrationVo = getIntegrationVo(integrationUuid);
        IIntegrationHandler handler = getIntegrationHandler(integrationVo);
        JSONObject integrationParam = buildIntegrationParam(integrationVo, config.getJSONArray("paramMapping"), paramObj);
        integrationVo.setParamObj(integrationParam);
        handler.sendRequest(integrationVo, FrameworkRequestFrom.API);
    }

    private IntegrationVo getIntegrationVo(String integrationUuid) {
        IntegrationVo integrationVo = integrationMapper.getIntegrationByUuid(integrationUuid);
        if (integrationVo == null) {
            throw new IntegrationNotFoundException(integrationUuid);
        }
        if (integrationVo.getIsActive() != 1) {
            throw new IntegrationUnActiveException(integrationUuid);
        }
        return integrationVo;
    }

    private IIntegrationHandler getIntegrationHandler(IntegrationVo integrationVo) {
        IIntegrationHandler handler = IntegrationHandlerFactory.getHandler(integrationVo.getHandler());
        if (handler == null) {
            throw new IntegrationHandlerNotFoundException(integrationVo.getHandler());
        }
        return handler;
    }

    private JSONObject buildIntegrationParam(IntegrationVo integrationVo, JSONArray paramMapping, JSONObject paramObj) {
        JSONObject integrationParam = new JSONObject();
        Map<String, String> paramTypeMap = getParamTypeMap(integrationVo);
        if (CollectionUtils.isNotEmpty(paramMapping)) {
            for (int i = 0; i < paramMapping.size(); i++) {
                JSONObject mapping = paramMapping.getJSONObject(i);
                String transferred = FreemarkerUtil.transform(paramObj, mapping.getString("expression"));
                integrationParam.put(mapping.getString("name"), transferParamValue(paramTypeMap.get(mapping.getString("name")), transferred));
            }
        }
        return integrationParam;
    }

    private Map<String, String> getParamTypeMap(IntegrationVo integrationVo) {
        Map<String, String> paramTypeMap = new HashMap<>();
        if (integrationVo.getConfig().getJSONObject("param") != null && integrationVo.getConfig().getJSONObject("param").getJSONArray("paramList") != null) {
            for (int i = 0; i < integrationVo.getConfig().getJSONObject("param").getJSONArray("paramList").size(); i++) {
                JSONObject paramObj = integrationVo.getConfig().getJSONObject("param").getJSONArray("paramList").getJSONObject(i);
                if (paramObj.getString("mode").equals("input")) {
                    paramTypeMap.put(paramObj.getString("name"), paramObj.getString("type"));
                }
            }
        }
        return paramTypeMap;
    }

    private Object transferParamValue(String type, String transferred) {
        if (type != null && type.equalsIgnoreCase("array")) {
            try {
                return JSON.parseArray(transferred);
            } catch (Exception ignored) {
                return transferred;
            }
        }
        return transferred;
    }
}
