/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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
import neatlogic.framework.scheduler.enums.JobLoadTriggerType;
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
    @Override
    public String getName() {
        return "告警事件间隔处理";
    }

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
    public void reloadJob(JobObject jobObject, JobLoadTriggerType triggerType) {
        Long alertId = (Long) jobObject.getData("alertId");
        Long alertEventHandlerId = (Long) jobObject.getData("alertEventHandlerId");
        AlertIntervalJobVo jobVo = alertMapper.getAlertIntervalJob(alertId, alertEventHandlerId);
        if (jobVo != null) {
            String tenantUuid = TenantContext.get().getTenantUuid();
            if (getLeftExecuteCount(jobVo) > 0) {
                schedulerManager.loadJob(buildIntervalJobObject(jobVo, tenantUuid), triggerType);
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
                if (getLeftExecuteCount(jobVo) > 0) {
                    schedulerManager.loadJob(buildIntervalJobObject(jobVo, tenantUuid), JobLoadTriggerType.SERVER_RESTART);
                }
            }
            paramJobVo.setCurrentPage(paramJobVo.getCurrentPage() + 1);
            jobList = alertMapper.searchAlertIntervalJob(paramJobVo);
        }
    }

    private JobObject buildIntervalJobObject(AlertIntervalJobVo jobVo, String tenantUuid) {
        JobObject.Builder builder = new JobObject.Builder(jobVo.getAlertId() + "#" + jobVo.getAlertEventHandlerId(), this.getGroupName(), this.getClassName(), tenantUuid);
        Date now = new Date();
        if (jobVo.getStartTime() != null && now.before(jobVo.getStartTime())) {
            builder.withBeginTime(jobVo.getStartTime());
        }
        int leftExecuteCount = getLeftExecuteCount(jobVo);
        if (jobVo.getIntervalMinute() != null && jobVo.getIntervalMinute() > 0) {
            builder.withRepeatCount(Math.max(leftExecuteCount - 1, 0));
            builder.withIntervalInSeconds(jobVo.getIntervalMinute() * 60);
        }
        builder.addData("alertId", jobVo.getAlertId());
        builder.addData("alertEventHandlerId", jobVo.getAlertEventHandlerId());
        return builder.build();
    }

    private int getLeftExecuteCount(AlertIntervalJobVo jobVo) {
        return jobVo == null || jobVo.getRepeatCount() == null ? 0 : Math.max(jobVo.getRepeatCount(), 0);
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
            int oldLeftExecuteCount = oldResultObj == null ? getLeftExecuteCount(jobVo) : oldResultObj.getIntValue("leftExecuteCount");
            int leftExecuteCount = Math.max(oldLeftExecuteCount - 1, 0);
            //System.out.println("next fire time:" + context.getNextFireTime());
            resultObj.put("nextStartTime", context.getNextFireTime());
            resultObj.put("leftExecuteCount", leftExecuteCount);
            resultObj.put("intervalMinute", oldResultObj == null ? jobVo.getIntervalMinute() : oldResultObj.get("intervalMinute"));
            auditVo.setResult(resultObj);
            alertEventMapper.updateAlertEventAuditResult(auditVo);
            if (context.getNextFireTime() != null) {
                AlertIntervalJobVo alertIntervalJobVo = new AlertIntervalJobVo();
                alertIntervalJobVo.setStartTime(context.getNextFireTime());
                alertIntervalJobVo.setRepeatCount(leftExecuteCount);
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
