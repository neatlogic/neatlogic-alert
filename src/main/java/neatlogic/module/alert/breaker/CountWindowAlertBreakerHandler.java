package neatlogic.module.alert.breaker;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.breaker.AlertBreakerHandlerBase;
import neatlogic.framework.alert.breaker.AlertBreakerManager;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.breaker.*;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.scheduler.enums.JobLoadTriggerType;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.schedule.handler.AlertBreakerFlushScheduleJob;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class CountWindowAlertBreakerHandler extends AlertBreakerHandlerBase {
    @Resource
    private AlertMapper alertMapper;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getName() {
        return "countwindow";
    }

    @Override
    public String getLabel() {
        return "触发量窗口熔断策略";
    }

    @Override
    public String getDescription() {
        return "按配置维度统计事件插件在时间窗口内的触发量，超过阈值后进入熔断。";
    }

    @Override
    protected String myMakeUniqueKey(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId) {
        return buildUniqueKey(policyVo.getConfig(), alertVo, eventHandlerVo);
    }

    @Override
    protected AlertBreakerCheckResultVo myCheck(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId, AlertBreakerStateVo stateVo, JSONObject data) {
        JSONObject config = policyVo.getConfig();
        long nowTime = System.currentTimeMillis();
        Date now = new Date(nowTime);
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
        } else {
            triggerCount = (stateVo.getTriggerCount() == null ? 0 : stateVo.getTriggerCount()) + 1;
        }

        AlertBreakerCheckResultVo checkResultVo = new AlertBreakerCheckResultVo();
        checkResultVo.setWindowStart(windowStart);
        checkResultVo.setWindowEnd(windowEnd);
        checkResultVo.setTriggerCount(triggerCount);
        if (triggerCount > threshold) {
            checkResultVo.setBreaked(true);
            checkResultVo.setOpenUntil(new Date(nowTime + getDurationMillis(config, "openDuration", "openDurationUnit", 10, "minute")));
            if (isEnableAggregate(config)) {
                checkResultVo.setData(buildInitialData(config));
                checkResultVo.setClearCollectItemOnOpen(true);
            }
        }
        return checkResultVo;
    }

    @Override
    protected AlertBreakerCollectResultVo myCollect(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId, AlertBreakerResultVo resultVo, AlertBreakerStateVo stateVo, JSONObject data, boolean isCollectingStarted) {
        JSONObject config = policyVo.getConfig();
        if (!isEnableAggregate(config) || stateVo == null || stateVo.getId() == null || alertVo == null || alertVo.getId() == null) {
            return null;
        }
        int collectLimit = getIntValue(config, "collectLimit", 1000);
        if (data.isEmpty()) {
            data = buildInitialData(config);
        }
        int collectCount = alertBreakerMapper.getAlertBreakerCollectItemCountByStateId(stateVo.getId());
        int collectDropCount = data.getIntValue("collectDropCount");
        if (collectCount < collectLimit) {
            AlertBreakerCollectItemVo itemVo = new AlertBreakerCollectItemVo();
            itemVo.setStateId(stateVo.getId());
            itemVo.setPolicyId(policyVo.getId());
            itemVo.setAlertId(alertVo.getId());
            if (alertBreakerMapper.insertAlertBreakerCollectItem(itemVo) > 0) {
                collectCount++;
            }
        } else if (alertBreakerMapper.checkAlertBreakerCollectItemIsExists(stateVo.getId(), alertVo.getId()) == 0) {
            collectDropCount++;
        }
        makeupCollectSummary(data, collectCount, collectDropCount);
        if (isCollectingStarted) {
            data.put("baselineAlertId", alertVo.getId());
        }
        AlertBreakerCollectResultVo collectResultVo = new AlertBreakerCollectResultVo();
        collectResultVo.setData(data);
        return collectResultVo;
    }

    @Override
    public void collectingStarted(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId, AlertBreakerResultVo resultVo, AlertBreakerCollectResultVo collectResultVo) {
        if (!isEnableAggregate(policyVo.getConfig()) || collectResultVo == null || collectResultVo.getStateVo() == null) {
            return;
        }
        loadFlushJob(collectResultVo.getStateVo());
    }

    @Override
    protected AlertBreakerFlushResultVo myFlush(AlertBreakerPolicyVo policyVo, AlertBreakerStateVo stateVo, JSONObject data) throws Exception {
        AlertBreakerFlushResultVo resultVo = new AlertBreakerFlushResultVo();
        Long baselineAlertId = data.getLong("baselineAlertId");
        List<Long> alertIdList = baselineAlertId == null
                ? alertBreakerMapper.getAlertBreakerCollectAlertIdListByStateId(stateVo.getId())
                : alertBreakerMapper.getAlertBreakerCollectAlertIdListByStateIdAndBaselineAlertId(stateVo.getId(), baselineAlertId);
        if (CollectionUtils.isNotEmpty(alertIdList)) {
            AlertVo paramVo = new AlertVo();
            paramVo.setIdList(alertIdList);
            List<AlertVo> alertList = alertMapper.getAlertByIdList(paramVo);
            resultVo.setAlertList(alertList);
        }
        resultVo.setData(data);
        return resultVo;
    }

    private void loadFlushJob(AlertBreakerStateVo stateVo) {
        if (stateVo == null || stateVo.getId() == null || stateVo.getOpenUntil() == null) {
            return;
        }
        IJob jobHandler = SchedulerManager.getHandler(AlertBreakerFlushScheduleJob.class.getName());
        if (jobHandler == null) {
            return;
        }
        JobObject jobObject = new JobObject.Builder(AlertBreakerManager.buildExpireJobName(stateVo.getId()), jobHandler.getGroupName(), jobHandler.getClassName())
                .addData("stateId", stateVo.getId())
                .addData("policyId", stateVo.getPolicyId())
                .addData("baselineAlertId", getData(stateVo).getLong("baselineAlertId"))
                .withBeginTime(stateVo.getOpenUntil())
                .build();
        schedulerManager.loadJob(jobObject, JobLoadTriggerType.INITIAL_CREATE);
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
            } else if (Objects.equals(dimension, "worker")) {
                valueList.add("worker=" + joinSorted(alertVo.getUserUuidList()));
            } else if (Objects.equals(dimension, "workerTeam")) {
                valueList.add("workerTeam=" + joinSorted(alertVo.getTeamUuidList()));
            } else {
                valueList.add("handlerInstance=" + eventHandlerVo.getId());
            }
        }
        return Md5Util.encryptMD5(String.join("|", valueList));
    }

    private String joinSorted(List<String> valueList) {
        List<String> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(valueList)) {
            list.addAll(valueList);
        }
        Collections.sort(list);
        return String.join(",", list);
    }

    private boolean isEnableAggregate(JSONObject config) {
        return config != null && Objects.equals(config.getInteger("enableAggregate"), 1);
    }

    private JSONObject buildInitialData(JSONObject config) {
        JSONObject data = new JSONObject();
        data.put("collectCount", 0);
        data.put("collectDropCount", 0);
        data.put("collectLimit", getIntValue(config, "collectLimit", 1000));
        return data;
    }

    private void makeupCollectSummary(JSONObject data, int collectCount, int collectDropCount) {
        data.put("collectCount", collectCount);
        data.put("collectDropCount", collectDropCount);
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

    private int getIntValue(JSONObject config, String name, int defaultValue) {
        int value = config == null ? defaultValue : config.getIntValue(name);
        return value <= 0 ? defaultValue : value;
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
