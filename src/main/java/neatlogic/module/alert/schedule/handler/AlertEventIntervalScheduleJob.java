/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.alert.schedule.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertIntervalJobVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.event.AlertEventHandlerFactory;
import neatlogic.framework.alert.event.IAlertEventHandler;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 合规检查定时器
 */
@Component
@DisallowConcurrentExecution
public class AlertEventIntervalScheduleJob extends JobBase {
    static Logger logger = LoggerFactory.getLogger(AlertEventIntervalScheduleJob.class);

    @Resource
    private AlertEventMapper alertEventMapper;

    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertAuditMapper alertAuditMapper;


    private JSONObject getHandlerConfig(Long alertId, Long alertEventHandlerId) {
        JSONObject handlerObj = null;
        AlertEventHandlerVo alertEventHandlerVo = alertEventMapper.getAlertEventHandlerById(alertEventHandlerId);
        if (alertEventHandlerVo != null) {
            JSONObject config = alertEventHandlerVo.getConfig();
            JSONArray intervalList = config.getJSONArray("intervalList");
            if (CollectionUtils.isNotEmpty(intervalList)) {
                handlerObj = intervalList.getJSONObject(0).getJSONObject("handler");
            }
        }
        if (handlerObj == null) {
            AlertIntervalJobVo jobVo = alertMapper.getAlertIntervalJob(alertId, alertEventHandlerId);
            if (jobVo != null && MapUtils.isNotEmpty(jobVo.getConfig())) {
                handlerObj = jobVo.getConfig().getJSONObject("handler");
            }
        }
        return handlerObj;
    }

    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        Long alertId = (Long) jobObject.getData("alertId");
        Long alertEventHandlerId = (Long) jobObject.getData("alertEventHandlerId");
        JSONObject handlerConfig = getHandlerConfig(alertId, alertEventHandlerId);

        if (handlerConfig == null) {
            schedulerManager.unloadJob(jobObject);
            //删除event数据
            alertMapper.deleteAlertIntervalJob(alertId, alertEventHandlerId);
            return false;
        }
        return true;
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        Long alertId = (Long) jobObject.getData("alertId");
        Long alertEventHandlerId = (Long) jobObject.getData("alertEventHandlerId");
        AlertIntervalJobVo jobVo = alertMapper.getAlertIntervalJob(alertId, alertEventHandlerId);
        if (jobVo != null) {
            String tenantUuid = TenantContext.get().getTenantUuid();
            if (jobVo.getRepeatCount() > 0) {
                JobObject.Builder builder = new JobObject.Builder(alertId + "#" + alertEventHandlerId, this.getGroupName(), this.getClassName(), tenantUuid);
                builder.withRepeatCount(jobVo.getRepeatCount() - 1);
                Date now = new Date();
                if (now.before(jobVo.getStartTime())) {
                    builder.withBeginTime(jobVo.getStartTime());
                }
                if (jobVo.getRepeatCount() > 0 && jobVo.getIntervalMinute() > 0) {
                    builder.withRepeatCount(jobVo.getRepeatCount());
                    builder.withIntervalInSeconds(jobVo.getIntervalMinute() * 60);
                }
                builder.addData("alertId", alertId);
                builder.addData("alertEventHandlerId", alertEventHandlerId);
                schedulerManager.loadJob(builder.build());
            } else {
                schedulerManager.unloadJob(jobObject);
            }
        } else {
            schedulerManager.unloadJob(jobObject);
        }
    }

    @Override
    public void initJob(String tenantUuid) {
        AlertIntervalJobVo paramJobVo = new AlertIntervalJobVo();
        paramJobVo.setPageSize(100);
        List<AlertIntervalJobVo> jobList = alertMapper.searchAlertIntervalJob(paramJobVo);
        while (CollectionUtils.isNotEmpty(jobList)) {
            for (AlertIntervalJobVo jobVo : jobList) {
                Long alertId = jobVo.getAlertId();
                Long alertEventHandlerId = jobVo.getAlertEventHandlerId();
                JobObject.Builder builder = new JobObject.Builder(alertId + "#" + alertEventHandlerId, this.getGroupName(), this.getClassName(), tenantUuid);
                builder.withRepeatCount(jobVo.getRepeatCount() - 1);
                Date now = new Date();
                if (now.before(jobVo.getStartTime())) {
                    builder.withBeginTime(jobVo.getStartTime());
                }
                if (jobVo.getRepeatCount() > 0 && jobVo.getIntervalMinute() > 0) {
                    builder.withRepeatCount(jobVo.getRepeatCount());
                    builder.withIntervalInSeconds(jobVo.getIntervalMinute() * 60);
                }
                builder.addData("alertId", alertId);
                builder.addData("alertEventHandlerId", alertEventHandlerId);
                schedulerManager.loadJob(builder.build());
            }
            paramJobVo.setCurrentPage(paramJobVo.getCurrentPage() + 1);
            jobList = alertMapper.searchAlertIntervalJob(paramJobVo);
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) {

        Long alertId = (Long) jobObject.getData("alertId");
        Long alertEventHandlerId = (Long) jobObject.getData("alertEventHandlerId");
        //System.out.println("执行作业：alertId=" + alertId + ",alertEventHandlerId=" + alertEventHandlerId);
        JSONObject handlerObj = getHandlerConfig(alertId, alertEventHandlerId);

        AlertVo alertVo = alertMapper.getAlertById(alertId);

        if (MapUtils.isNotEmpty(handlerObj) && alertVo != null) {
            AlertIntervalJobVo jobVo = alertMapper.getAlertIntervalJob(alertId, alertEventHandlerId);
            AlertEventHandlerAuditVo auditVo = alertAuditMapper.getAlertEventAuditById(jobVo.getParentAuditId());
            IAlertEventHandler eventHandler = AlertEventHandlerFactory.getHandler(handlerObj.getString("handler"));
            AlertEventHandlerVo subHandler = alertEventMapper.getAlertEventHandlerByUuid(handlerObj.getString("uuid"));
            if (subHandler != null) {
                eventHandler.trigger(subHandler, alertVo, jobVo.getParentAuditId());
            }
            JSONObject oldResultObj = auditVo.getResult();
            JSONObject resultObj = new JSONObject();
            //System.out.println("next fire time:" + context.getNextFireTime());
            resultObj.put("nextStartTime", context.getNextFireTime());
            resultObj.put("leftExecuteCount", oldResultObj.getIntValue("leftExecuteCount") - 1);
            resultObj.put("intervalMinute", oldResultObj.get("intervalMinute"));
            auditVo.setResult(resultObj);
            alertEventMapper.updateAlertEventAuditResult(auditVo);
            if (context.getNextFireTime() != null) {
                AlertIntervalJobVo alertIntervalJobVo = new AlertIntervalJobVo();
                alertIntervalJobVo.setStartTime(context.getNextFireTime());
                alertIntervalJobVo.setRepeatCount(oldResultObj.getIntValue("leftExecuteCount") - 1);
                alertIntervalJobVo.setAlertId(alertId);
                alertIntervalJobVo.setAlertEventHandlerId(alertEventHandlerId);
                alertMapper.updateAlertIntervalJob(alertIntervalJobVo);
            } else {
                alertMapper.deleteAlertIntervalJob(alertId, alertEventHandlerId);
                schedulerManager.unloadJob(jobObject);
            }
        } else {
            //System.out.println("###########删除作业Job：" + alertId);
            schedulerManager.unloadJob(jobObject);
            alertMapper.deleteAlertIntervalJob(alertId, alertEventHandlerId);
        }
    }


    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-ALERT-EVENT-INTERVAL";
    }

}
