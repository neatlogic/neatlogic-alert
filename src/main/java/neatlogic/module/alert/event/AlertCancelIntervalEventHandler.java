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

package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.schedule.handler.AlertEventIntervalScheduleJob;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class AlertCancelIntervalEventHandler extends AlertEventHandlerBase {
    @Resource
    private AlertMapper alertMapper;

    @Resource
    private SchedulerManager schedulerManager;

    @Override
    public int getSort() {
        return 6;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) throws AlertEventHandlerTriggerException {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (config == null) {
            config = new JSONObject();
        }
        List<Long> intervalHandlerIdList = config.getJSONArray("intervalHandlerIdList") == null ? new ArrayList<>() : config.getJSONArray("intervalHandlerIdList").toJavaList(Long.class);
        if (CollectionUtils.isEmpty(intervalHandlerIdList)) {
            throw new AlertEventHandlerTriggerException(new IllegalArgumentException("intervalHandlerIdList is required"));
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("intervalHandlerIdList", intervalHandlerIdList);
        try {
            IJob jobHandler = SchedulerManager.getHandler(AlertEventIntervalScheduleJob.class.getName());
            int cancelled = 0;
            List<Long> cancelledIdList = new ArrayList<>();
            for (Long intervalHandlerId : intervalHandlerIdList) {
                AlertIntervalJobVo intervalJobVo = alertMapper.getAlertIntervalJob(alertVo.getId(), intervalHandlerId);
                if (intervalJobVo == null) {
                    continue;
                }
                JobObject jobObject = new JobObject.Builder(alertVo.getId() + "#" + intervalHandlerId, jobHandler.getGroupName(), jobHandler.getClassName())
                        .addData("alertId", alertVo.getId())
                        .addData("alertEventHandlerId", intervalHandlerId)
                        .build();
                schedulerManager.unloadJob(jobObject);
                alertMapper.deleteAlertIntervalJob(alertVo.getId(), intervalHandlerId);
                cancelled++;
                cancelledIdList.add(intervalHandlerId);
                if (Objects.equals(intervalHandlerId, alertEventHandlerVo.getId())) {
                    resultObj.put("warning", "取消了当前定时插件对应的作业");
                }
            }
            resultObj.put("cancelled", cancelled);
            resultObj.put("cancelledIdList", cancelledIdList);
            alertEventHandlerAuditVo.setResult(resultObj);
            return alertVo;
        } catch (Exception ex) {
            throw new AlertEventHandlerTriggerException(ex);
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public String getName() {
        return "CANCEL_INTERVAL";
    }

    @Override
    public String getLabel() {
        return "取消定时调度";
    }

    @Override
    public String getIcon() {
        return "tsfont-time";
    }

    @Override
    public String getDescription() {
        return "取消当前告警类型中由定时调度插件创建的定时作业。";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
            this.add(AlertEventType.ALERT_SUPPRESS.getName());
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_DELETE.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
            this.add("integration");
        }};
    }
}
