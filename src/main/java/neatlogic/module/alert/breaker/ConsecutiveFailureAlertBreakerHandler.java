package neatlogic.module.alert.breaker;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.breaker.AlertBreakerHandlerBase;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerPolicyVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerResultVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerStateVo;
import neatlogic.framework.alert.enums.AlertBreakerState;
import neatlogic.framework.alert.enums.AlertBreakerStatus;
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
        return "连续失败熔断";
    }

    @Override
    public String getDescription() {
        return "按插件实例或插件类型统计连续失败次数，达到阈值后进入熔断。";
    }

    @Override
    protected AlertBreakerResultVo myCheck(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId) {
        JSONObject config = policyVo.getConfig();
        String scope = getScope(config);
        AlertBreakerStateVo stateVo = getStateForUpdate(policyVo, scope, eventHandlerVo);

        AlertBreakerResultVo resultVo = new AlertBreakerResultVo();
        resultVo.setStateId(stateVo.getId());
        long nowTime = System.currentTimeMillis();
        Date now = new Date(nowTime);
        if (Objects.equals(stateVo.getState(), AlertBreakerState.OPEN.getValue()) && stateVo.getOpenUntil() != null && stateVo.getOpenUntil().getTime() > nowTime) {
            stateVo.setSkipCount((stateVo.getSkipCount() == null ? 0 : stateVo.getSkipCount()) + 1);
            stateVo.setLastTriggerTime(now);
            alertBreakerMapper.updateAlertBreakerState(stateVo);
            resultVo.setBreaked(true);
            resultVo.setStatus(AlertBreakerStatus.OPEN.getValue());
            return resultVo;
        }

        if (Objects.equals(stateVo.getState(), AlertBreakerState.OPEN.getValue())) {
            stateVo.setState(AlertBreakerState.CLOSED.getValue());
            stateVo.setTriggerCount(0);
            stateVo.setOpenTime(null);
            stateVo.setOpenUntil(null);
            stateVo.setLastTriggerTime(now);
            stateVo.setData(buildData(scope, getIntValue(config, "failureThreshold", 3), 0, eventHandlerAuditId));
            alertBreakerMapper.updateAlertBreakerState(stateVo);
        }
        resultVo.setBreaked(false);
        resultVo.setStatus(AlertBreakerStatus.PASS.getValue());
        return resultVo;
    }

    @Override
    protected void myAfter(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, AlertEventHandlerAuditVo eventHandlerAuditVo) {
        if (eventHandlerAuditVo == null || eventHandlerAuditVo.getStatus() == null) {
            return;
        }
        if (!Objects.equals(eventHandlerAuditVo.getStatus(), AlertEventStatus.FAILED.getValue()) && !Objects.equals(eventHandlerAuditVo.getStatus(), AlertEventStatus.SUCCEED.getValue())) {
            return;
        }
        JSONObject config = policyVo.getConfig();
        String scope = getScope(config);
        int threshold = getIntValue(config, "failureThreshold", 3);
        AlertBreakerStateVo stateVo = getStateForUpdate(policyVo, scope, eventHandlerVo);
        int failureCount = getFailureCount(stateVo);
        if (Objects.equals(eventHandlerAuditVo.getStatus(), AlertEventStatus.FAILED.getValue())) {
            failureCount++;
        } else {
            failureCount = 0;
        }
        long nowTime = System.currentTimeMillis();
        Date now = new Date(nowTime);
        stateVo.setTriggerCount(failureCount);
        stateVo.setLastTriggerTime(now);
        stateVo.setWindowStart(null);
        stateVo.setWindowEnd(null);
        stateVo.setData(buildData(scope, threshold, failureCount, eventHandlerAuditVo.getId()));
        if (failureCount >= threshold) {
            stateVo.setState(AlertBreakerState.OPEN.getValue());
            stateVo.setOpenTime(now);
            stateVo.setOpenUntil(new Date(nowTime + getDurationMillis(config, "openDuration", "openDurationUnit", 10, "minute")));
            alertBreakerMapper.updateAlertBreakerState(stateVo);
            return;
        }
        stateVo.setState(AlertBreakerState.CLOSED.getValue());
        stateVo.setOpenTime(null);
        stateVo.setOpenUntil(null);
        stateVo.setSkipCount(0);
        alertBreakerMapper.updateAlertBreakerState(stateVo);
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

    private AlertBreakerStateVo getStateForUpdate(AlertBreakerPolicyVo policyVo, String scope, AlertEventHandlerVo eventHandlerVo) {
        String uniqueKey = buildUniqueKey(scope, eventHandlerVo);
        AlertBreakerStateVo stateVo = new AlertBreakerStateVo();
        stateVo.setPolicyId(policyVo.getId());
        stateVo.setUniqueKey(uniqueKey);
        stateVo.setState(AlertBreakerState.CLOSED.getValue());
        alertBreakerMapper.insertAlertBreakerStateIfNotExists(stateVo);
        return alertBreakerMapper.getAlertBreakerStateForUpdate(policyVo.getId(), uniqueKey);
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

    private String buildData(String scope, int threshold, int failureCount, Long lastAuditId) {
        JSONObject data = new JSONObject();
        data.put("scope", scope);
        data.put("failureThreshold", threshold);
        data.put("failureCount", failureCount);
        data.put("lastAuditId", lastAuditId);
        return data.toJSONString();
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
