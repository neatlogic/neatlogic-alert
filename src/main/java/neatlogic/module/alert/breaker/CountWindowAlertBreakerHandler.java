package neatlogic.module.alert.breaker;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.breaker.AlertBreakerHandlerBase;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerPolicyVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerResultVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerStateVo;
import neatlogic.framework.alert.enums.AlertBreakerState;
import neatlogic.framework.alert.enums.AlertBreakerStatus;
import neatlogic.framework.util.Md5Util;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class CountWindowAlertBreakerHandler extends AlertBreakerHandlerBase {
    @Override
    public String getName() {
        return "countwindow";
    }

    @Override
    public String getLabel() {
        return "触发量窗口熔断";
    }

    @Override
    public String getDescription() {
        return "按配置维度统计事件插件在时间窗口内的触发量，超过阈值后进入熔断。";
    }

    @Override
    protected AlertBreakerResultVo myCheck(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId) {
        JSONObject config = policyVo.getConfig();
        String uniqueKey = buildUniqueKey(config, alertVo, eventHandlerVo);
        AlertBreakerStateVo stateVo = new AlertBreakerStateVo();
        stateVo.setPolicyId(policyVo.getId());
        stateVo.setUniqueKey(uniqueKey);
        stateVo.setState(AlertBreakerState.CLOSED.getValue());
        alertBreakerMapper.insertAlertBreakerStateIfNotExists(stateVo);
        stateVo = alertBreakerMapper.getAlertBreakerStateForUpdate(policyVo.getId(), uniqueKey);

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

        long windowMillis = getDurationMillis(config, "windowSize", "windowUnit", 1, "minute");
        int threshold = config == null ? 100 : config.getIntValue("threshold");
        if (threshold <= 0) {
            threshold = 100;
        }
        Date windowStart = stateVo.getWindowStart();
        Date windowEnd = stateVo.getWindowEnd();
        int triggerCount;
        if (windowStart == null || windowEnd == null || windowEnd.getTime() <= nowTime) {
            windowStart = now;
            windowEnd = new Date(nowTime + windowMillis);
            triggerCount = 1;
            stateVo.setOpenTime(null);
            stateVo.setOpenUntil(null);
            stateVo.setSkipCount(0);
        } else {
            triggerCount = (stateVo.getTriggerCount() == null ? 0 : stateVo.getTriggerCount()) + 1;
        }

        stateVo.setWindowStart(windowStart);
        stateVo.setWindowEnd(windowEnd);
        stateVo.setTriggerCount(triggerCount);
        stateVo.setLastTriggerTime(now);
        if (triggerCount > threshold) {
            stateVo.setState(AlertBreakerState.OPEN.getValue());
            stateVo.setOpenTime(now);
            stateVo.setOpenUntil(new Date(nowTime + getDurationMillis(config, "openDuration", "openDurationUnit", 10, "minute")));
            alertBreakerMapper.updateAlertBreakerState(stateVo);
            resultVo.setBreaked(true);
            resultVo.setStatus(AlertBreakerStatus.OPEN.getValue());
            return resultVo;
        }
        stateVo.setState(AlertBreakerState.CLOSED.getValue());
        alertBreakerMapper.updateAlertBreakerState(stateVo);
        resultVo.setBreaked(false);
        resultVo.setStatus(AlertBreakerStatus.PASS.getValue());
        return resultVo;
    }

    private String buildUniqueKey(JSONObject config, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo) {
        JSONArray dimensionArray = config == null ? null : config.getJSONArray("dimensionList");
        List<String> dimensionList = new ArrayList<>();
        if (dimensionArray != null && !dimensionArray.isEmpty()) {
            for (int i = 0; i < dimensionArray.size(); i++) {
                dimensionList.add(dimensionArray.getString(i));
            }
        } else {
            dimensionList.add("handlerInstance");
        }
        List<String> valueList = new ArrayList<>();
        for (String dimension : dimensionList) {
            if (Objects.equals(dimension, "alertType")) {
                valueList.add("alertType=" + alertVo.getType());
            } else if (Objects.equals(dimension, "alertLevel")) {
                valueList.add("alertLevel=" + alertVo.getLevel());
            } else if (Objects.equals(dimension, "event")) {
                valueList.add("event=" + eventHandlerVo.getEvent());
            } else if (Objects.equals(dimension, "handler")) {
                valueList.add("handler=" + eventHandlerVo.getHandler());
            } else if (Objects.equals(dimension, "source")) {
                valueList.add("source=" + alertVo.getSource());
            } else {
                valueList.add("handlerInstance=" + eventHandlerVo.getId());
            }
        }
        return Md5Util.encryptMD5(String.join("|", valueList));
    }

    private long getDurationMillis(JSONObject config, String valueName, String unitName, int defaultValue, String defaultUnit) {
        int value = config == null ? defaultValue : config.getIntValue(valueName);
        if (value <= 0) {
            value = defaultValue;
        }
        String unit = config == null ? defaultUnit : config.getString(unitName);
        if (Objects.equals(unit, "second")) {
            return value * 1000L;
        } else if (Objects.equals(unit, "hour")) {
            return value * 60L * 60L * 1000L;
        }
        return value * 60L * 1000L;
    }
}
