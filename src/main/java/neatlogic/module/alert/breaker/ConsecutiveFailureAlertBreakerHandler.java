package neatlogic.module.alert.breaker;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.breaker.AlertBreakerHandlerBase;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerAfterResultVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerCheckResultVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerPolicyVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerStateVo;
import neatlogic.framework.alert.enums.AlertBreakerState;
import neatlogic.framework.alert.enums.AlertEventStatus;
import neatlogic.framework.util.Md5Util;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

@Component
public class ConsecutiveFailureAlertBreakerHandler extends AlertBreakerHandlerBase {
    @Override
    public String getName() {
        return "consecutivefailure";
    }

    @Override
    public String getLabel() {
        return "连续失败熔断策略";
    }

    @Override
    public String getDescription() {
        return "按插件实例或插件类型统计连续失败次数，达到阈值后进入熔断。";
    }

    @Override
    protected String myMakeUniqueKey(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId) {
        JSONObject config = policyVo.getConfig();
        String scope = getScope(config);
        return buildUniqueKey(scope, eventHandlerVo);
    }

    @Override
    protected AlertBreakerCheckResultVo myCheck(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId, AlertBreakerStateVo stateVo, JSONObject data) {
        if (Objects.equals(stateVo.getState(), AlertBreakerState.OPEN.getValue())) {
            JSONObject config = policyVo.getConfig();
            String scope = getScope(config);
            AlertBreakerCheckResultVo checkResultVo = new AlertBreakerCheckResultVo();
            checkResultVo.setTriggerCount(0);
            checkResultVo.setData(buildData(scope, getIntValue(config, "failureThreshold", 3), 0, eventHandlerAuditId));
            return checkResultVo;
        }
        return null;
    }

    @Override
    protected AlertBreakerAfterResultVo myAfter(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, AlertEventHandlerAuditVo eventHandlerAuditVo, AlertBreakerStateVo stateVo, JSONObject data) {
        if (eventHandlerAuditVo == null || eventHandlerAuditVo.getStatus() == null) {
            return null;
        }
        if (!Objects.equals(eventHandlerAuditVo.getStatus(), AlertEventStatus.FAILED.getValue()) && !Objects.equals(eventHandlerAuditVo.getStatus(), AlertEventStatus.SUCCEED.getValue())) {
            return null;
        }
        JSONObject config = policyVo.getConfig();
        String scope = getScope(config);
        int threshold = getIntValue(config, "failureThreshold", 3);
        int failureCount = getFailureCount(stateVo);
        if (Objects.equals(eventHandlerAuditVo.getStatus(), AlertEventStatus.FAILED.getValue())) {
            failureCount++;
        } else {
            failureCount = 0;
        }
        long nowTime = System.currentTimeMillis();
        AlertBreakerAfterResultVo afterResultVo = new AlertBreakerAfterResultVo();
        afterResultVo.setTriggerCount(failureCount);
        afterResultVo.setData(buildData(scope, threshold, failureCount, eventHandlerAuditVo.getId()));
        if (failureCount >= threshold) {
            afterResultVo.setBreaked(true);
            afterResultVo.setOpenUntil(new Date(nowTime + getDurationMillis(config, "openDuration", "openDurationUnit", 10, "minute")));
        }
        return afterResultVo;
    }

    private String getScope(JSONObject config) {
        String scope = config == null ? null : config.getString("scope");
        if (Objects.equals(scope, "handler")) {
            return scope;
        }
        return "handlerInstance";
    }

    private String buildUniqueKey(String scope, AlertEventHandlerVo eventHandlerVo) {
        if (Objects.equals(scope, "handler")) {
            return Md5Util.encryptMD5("handler=" + eventHandlerVo.getHandler());
        }
        return Md5Util.encryptMD5("handlerInstance=" + eventHandlerVo.getId());
    }

    private int getFailureCount(AlertBreakerStateVo stateVo) {
        JSONObject data = getData(stateVo);
        int failureCount = data.getIntValue("failureCount");
        if (failureCount <= 0) {
            failureCount = stateVo.getTriggerCount() == null ? 0 : stateVo.getTriggerCount();
        }
        return Math.max(failureCount, 0);
    }

    private JSONObject getData(AlertBreakerStateVo stateVo) {
        if (stateVo == null || stateVo.getData() == null) {
            return new JSONObject();
        }
        try {
            return JSONObject.parseObject(stateVo.getData());
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private JSONObject buildData(String scope, int threshold, int failureCount, Long lastAuditId) {
        JSONObject data = new JSONObject();
        data.put("scope", scope);
        data.put("failureThreshold", threshold);
        data.put("failureCount", failureCount);
        data.put("lastAuditId", lastAuditId);
        return data;
    }

    private int getIntValue(JSONObject config, String name, int defaultValue) {
        int value = config == null ? defaultValue : config.getIntValue(name);
        return value <= 0 ? defaultValue : value;
    }

    private long getDurationMillis(JSONObject config, String valueName, String unitName, int defaultValue, String defaultUnit) {
        int value = getIntValue(config, valueName, defaultValue);
        String unit = config == null ? defaultUnit : config.getString(unitName);
        if (Objects.equals(unit, "second")) {
            return value * 1000L;
        } else if (Objects.equals(unit, "hour")) {
            return value * 60L * 60L * 1000L;
        }
        return value * 60L * 1000L;
    }
}
