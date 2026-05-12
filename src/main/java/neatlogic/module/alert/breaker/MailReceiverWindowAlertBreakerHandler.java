package neatlogic.module.alert.breaker;

import com.alibaba.fastjson.JSON;
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
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dto.AlertMailReceiverVo;
import neatlogic.module.alert.schedule.handler.AlertBreakerFlushScheduleJob;
import neatlogic.module.alert.service.AlertMailReceiverService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class MailReceiverWindowAlertBreakerHandler extends AlertBreakerHandlerBase {
    @Resource
    private AlertMailReceiverService alertMailReceiverService;

    @Resource
    private AlertMapper alertMapper;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public String getName() {
        return "mailreceiverwindow";
    }

    @Override
    public String getLabel() {
        return "邮件收件人熔断策略";
    }

    @Override
    public String getDescription() {
        return "按邮件实际收件人列表统计发送次数，超过阈值后延迟并聚合发送。";
    }

    @Override
    protected String myMakeUniqueKey(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId) {
        AlertMailReceiverVo receiverVo = getReceiver(alertVo, eventHandlerVo);
        if (receiverVo == null || (CollectionUtils.isEmpty(receiverVo.getToList()) && CollectionUtils.isEmpty(receiverVo.getCcList()))) {
            return null;
        }
        return buildUniqueKey(eventHandlerVo, receiverVo);
    }

    @Override
    protected AlertBreakerCheckResultVo myCheck(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId, AlertBreakerStateVo stateVo, JSONObject data) {
        AlertMailReceiverVo receiverVo = getReceiver(alertVo, eventHandlerVo);
        long nowTime = System.currentTimeMillis();
        Date now = new Date(nowTime);
        JSONObject config = policyVo.getConfig();
        long windowMillis = getWindowMillis(config);
        int threshold = getIntValue(config, "threshold", 10);
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
            checkResultVo.setOpenUntil(new Date(nowTime + windowMillis));
            checkResultVo.setData(buildInitialData(eventHandlerVo, receiverVo));
            checkResultVo.setClearCollectItemOnOpen(true);
        }
        return checkResultVo;
    }

    @Override
    protected AlertBreakerCollectResultVo myCollect(AlertBreakerPolicyVo policyVo, AlertVo alertVo, AlertEventHandlerVo eventHandlerVo, Long eventHandlerAuditId, AlertBreakerResultVo resultVo, AlertBreakerStateVo stateVo, JSONObject data, boolean isCollectingStarted) {
        if (stateVo == null || stateVo.getId() == null || alertVo == null || alertVo.getId() == null) {
            return null;
        }
        int collectLimit = getIntValue(policyVo.getConfig(), "collectLimit", 1000);
        if (data.isEmpty()) {
            AlertMailReceiverVo receiverVo = getReceiver(alertVo, eventHandlerVo);
            data = buildInitialData(eventHandlerVo, receiverVo);
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
        if (collectResultVo == null || collectResultVo.getStateVo() == null || collectResultVo.getStateVo().getId() == null) {
            return;
        }
        loadFlushJob(collectResultVo.getStateVo());
    }

    @Override
    protected AlertBreakerFlushResultVo myFlush(AlertBreakerPolicyVo policyVo, AlertBreakerStateVo stateVo, JSONObject data) throws Exception {
        Long baselineAlertId = data.getLong("baselineAlertId");
        List<Long> alertIdList = baselineAlertId == null
                ? alertBreakerMapper.getAlertBreakerCollectAlertIdListByStateId(stateVo.getId())
                : alertBreakerMapper.getAlertBreakerCollectAlertIdListByStateIdAndBaselineAlertId(stateVo.getId(), baselineAlertId);
        if (CollectionUtils.isEmpty(alertIdList)) {
            return buildFlushResult(data);
        }
        AlertVo paramVo = new AlertVo();
        paramVo.setIdList(alertIdList);
        List<AlertVo> alertList = alertMapper.getAlertByIdList(paramVo);
        AlertBreakerFlushResultVo resultVo = buildFlushResult(data);
        resultVo.setAlertList(alertList);
        return resultVo;
    }

    public void loadFlushJob(AlertBreakerStateVo stateVo) {
        if (stateVo == null || stateVo.getId() == null || stateVo.getOpenUntil() == null) {
            return;
        }
        JSONObject data = getData(stateVo);
        Long baselineAlertId = data.getLong("baselineAlertId");
        if (baselineAlertId == null) {
            return;
        }
        IJob jobHandler = SchedulerManager.getHandler(AlertBreakerFlushScheduleJob.class.getName());
        if (jobHandler == null) {
            return;
        }
        JobObject jobObject = new JobObject.Builder(AlertBreakerManager.buildExpireJobName(stateVo.getId()), jobHandler.getGroupName(), jobHandler.getClassName())
                .addData("stateId", stateVo.getId())
                .addData("policyId", stateVo.getPolicyId())
                .addData("baselineAlertId", baselineAlertId)
                .withBeginTime(stateVo.getOpenUntil())
                .build();
        schedulerManager.loadJob(jobObject);
    }

    public static String buildFlushJobName(Long stateId) {
        return AlertBreakerManager.buildExpireJobName(stateId);
    }

    private AlertMailReceiverVo getReceiver(AlertVo alertVo, AlertEventHandlerVo eventHandlerVo) {
        JSONObject handlerConfig = eventHandlerVo == null ? null : eventHandlerVo.getConfig();
        if (handlerConfig == null) {
            return null;
        }
        return alertMailReceiverService.getReceiver(alertVo, handlerConfig.getJSONArray("toUserList"), handlerConfig.getJSONArray("ccUserList"));
    }

    private JSONObject buildInitialData(AlertEventHandlerVo eventHandlerVo, AlertMailReceiverVo receiverVo) {
        JSONObject data = new JSONObject();
        JSONObject handlerConfig = eventHandlerVo == null ? null : eventHandlerVo.getConfig();
        if (handlerConfig != null) {
            data.put("mailServerId", handlerConfig.getLong("mailServerId"));
        }
        if (receiverVo != null) {
            data.put("to", toJsonArray(receiverVo.getToList()));
            data.put("cc", toJsonArray(receiverVo.getCcList()));
            data.put("groupName", receiverVo.getGroupName());
        }
        data.put("collectCount", 0);
        data.put("collectDropCount", 0);
        return data;
    }

    private void makeupCollectSummary(JSONObject data, int collectCount, int collectDropCount) {
        data.put("collectCount", collectCount);
        data.put("collectDropCount", collectDropCount);
    }

    private AlertBreakerFlushResultVo buildFlushResult(JSONObject data) {
        AlertBreakerFlushResultVo resultVo = new AlertBreakerFlushResultVo();
        resultVo.setData(data);
        return resultVo;
    }

    private String buildUniqueKey(AlertEventHandlerVo eventHandlerVo, AlertMailReceiverVo receiverVo) {
        List<String> toList = sort(receiverVo.getToList());
        List<String> ccList = sort(receiverVo.getCcList());
        return Md5Util.encryptMD5("mailReceiver|handlerInstance=" + eventHandlerVo.getId() + "|to=" + String.join(",", toList) + "|cc=" + String.join(",", ccList));
    }

    private List<String> sort(Set<String> valueSet) {
        List<String> valueList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(valueSet)) {
            valueList.addAll(valueSet);
        }
        Collections.sort(valueList);
        return valueList;
    }

    private JSONArray toJsonArray(Set<String> valueSet) {
        JSONArray array = new JSONArray();
        array.addAll(sort(valueSet));
        return array;
    }

    private JSONObject getData(AlertBreakerStateVo stateVo) {
        if (stateVo == null || StringUtils.isBlank(stateVo.getData())) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(stateVo.getData());
        } catch (Exception ignored) {
            return new JSONObject();
        }
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

    private long getWindowMillis(JSONObject config) {
        if (config != null && !config.containsKey("windowSize") && config.containsKey("openDuration")) {
            return getDurationMillis(config, "openDuration", "openDurationUnit", 1, "minute");
        }
        return getDurationMillis(config, "windowSize", "windowUnit", 1, "minute");
    }

}
