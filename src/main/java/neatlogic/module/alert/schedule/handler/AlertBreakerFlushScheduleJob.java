package neatlogic.module.alert.schedule.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.breaker.AlertBreakerManager;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dto.breaker.AlertBreakerPolicyVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerStateVo;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.module.alert.breaker.MailReceiverWindowAlertBreakerHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
@DisallowConcurrentExecution
public class AlertBreakerFlushScheduleJob extends JobBase {
    private static final int PAGE_SIZE = 100;

    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    @Override
    public String getName() {
        return "告警熔断到期处理";
    }

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-ALERT-BREAKER-FLUSH";
    }

    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        schedulerManager.loadJob(jobObject);
    }

    @Override
    public void initJob(String tenantUuid) {
        Long lastId = 0L;
        List<AlertBreakerStateVo> stateList = alertBreakerMapper.getCollectingAlertBreakerStateList("mailreceiverwindow", lastId, PAGE_SIZE);
        while (CollectionUtils.isNotEmpty(stateList)) {
            for (AlertBreakerStateVo stateVo : stateList) {
                loadFlushJob(stateVo, tenantUuid);
                lastId = stateVo.getId();
            }
            if (stateList.size() < PAGE_SIZE) {
                break;
            }
            stateList = alertBreakerMapper.getCollectingAlertBreakerStateList("mailreceiverwindow", lastId, PAGE_SIZE);
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) {
        try {
            Long stateId = getLong(jobObject.getData("stateId"));
            if (stateId == null) {
                return;
            }
            AlertBreakerStateVo stateVo = new AlertBreakerStateVo();
            stateVo.setId(stateId);
            Long policyId = getLong(jobObject.getData("policyId"));
            AlertBreakerPolicyVo policyVo = policyId == null ? null : alertBreakerMapper.getAlertBreakerPolicyById(policyId);
            if (policyVo == null) {
                AlertBreakerStateVo currentStateVo = alertBreakerMapper.getAlertBreakerStateById(stateId);
                if (currentStateVo != null) {
                    policyVo = alertBreakerMapper.getAlertBreakerPolicyById(currentStateVo.getPolicyId());
                }
            }
            AlertBreakerManager.flush(policyVo, stateVo);
        } finally {
            schedulerManager.unloadJob(jobObject);
        }
    }

    private void loadFlushJob(AlertBreakerStateVo stateVo, String tenantUuid) {
        if (stateVo == null || stateVo.getId() == null || stateVo.getOpenUntil() == null) {
            return;
        }
        JobObject jobObject = new JobObject.Builder(MailReceiverWindowAlertBreakerHandler.buildFlushJobName(stateVo.getId()), this.getGroupName(), this.getClassName(), tenantUuid)
                .addData("stateId", stateVo.getId())
                .addData("policyId", stateVo.getPolicyId())
                .addData("baselineAlertId", getBaselineAlertId(stateVo))
                .withBeginTime(stateVo.getOpenUntil())
                .build();
        schedulerManager.loadJob(jobObject);
    }

    private Long getBaselineAlertId(AlertBreakerStateVo stateVo) {
        try {
            JSONObject data = JSONObject.parseObject(stateVo.getData());
            return data == null ? null : data.getLong("baselineAlertId");
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long getLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.valueOf((String) value);
        }
        return null;
    }
}
