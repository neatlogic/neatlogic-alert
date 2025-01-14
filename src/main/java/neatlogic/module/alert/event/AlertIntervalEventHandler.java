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

package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertIntervalJobVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventHandlerFactory;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.event.IAlertEventHandler;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.schedule.handler.AlertEventIntervalScheduleJob;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

@Component
public class AlertIntervalEventHandler extends AlertEventHandlerBase {
    @Resource
    private AlertMapper alertMapper;

    @Resource
    protected SchedulerManager schedulerManager;

    @Override
    public void makeupChildHandler(AlertEventHandlerVo alertEventHandlerVo) {
        if (MapUtils.isNotEmpty(alertEventHandlerVo.getConfig())) {
            JSONArray intervalList = alertEventHandlerVo.getConfig().getJSONArray("intervalList");
            for (int e = 0; e < intervalList.size(); e++) {
                JSONObject eventConditionObj = intervalList.getJSONObject(e);
                JSONObject handlerObj = eventConditionObj.getJSONObject("handler");
                IAlertEventHandler eventHandler = AlertEventHandlerFactory.getHandler(handlerObj.getString("handler"));
                AlertEventHandlerVo subAlertEventHandlerVo = new AlertEventHandlerVo();
                subAlertEventHandlerVo.setParentId(alertEventHandlerVo.getId());
                subAlertEventHandlerVo.setEvent(alertEventHandlerVo.getEvent());
                subAlertEventHandlerVo.setAlertType(alertEventHandlerVo.getAlertType());
                subAlertEventHandlerVo.setIsActive(alertEventHandlerVo.getIsActive());
                subAlertEventHandlerVo.setUuid(handlerObj.getString("uuid"));
                subAlertEventHandlerVo.setName(handlerObj.getString("name"));
                subAlertEventHandlerVo.setHandler(handlerObj.getString("handler"));
                subAlertEventHandlerVo.setConfig(handlerObj.getJSONObject("config"));
                alertEventHandlerVo.addHandler(subAlertEventHandlerVo);
                eventHandler.makeupChildHandler(subAlertEventHandlerVo);
            }
        }
    }


    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo) throws AlertEventHandlerTriggerException {
        JSONObject rootConfig = alertEventHandlerVo.getConfig();
        if (MapUtils.isNotEmpty(rootConfig)) {
            JSONArray intervalList = rootConfig.getJSONArray("intervalList");
            if (CollectionUtils.isNotEmpty(intervalList)) {
                for (int i = 0; i < intervalList.size(); i++) {
                    JSONObject config = intervalList.getJSONObject(i);
                    Integer delayMinute = config.getInteger("delayMinute");
                    Integer intervalMinute = config.getInteger("intervalMinute");
                    Integer repeatCount = config.getInteger("repeatCount");
                    Calendar calendar = Calendar.getInstance();
                    if (delayMinute == null) {
                        delayMinute = 0;
                    }
                    if (repeatCount == null) {
                        repeatCount = 0;
                    }
                    if (intervalMinute == null) {
                        intervalMinute = 0;
                    }
                    calendar.add(Calendar.MINUTE, delayMinute);
                    IJob jobHandler = SchedulerManager.getHandler(AlertEventIntervalScheduleJob.class.getName());
                    JobObject.Builder builder = new JobObject.Builder(alertVo.getId().toString() + "#" + alertEventHandlerVo.getId(), jobHandler.getGroupName(), jobHandler.getClassName())
                            .addData("alertId", alertVo.getId())
                            .addData("alertEventHandlerId", alertEventHandlerVo.getId())
                            .withBeginTime(calendar.getTime());
                    if (repeatCount > 0 && intervalMinute > 0) {
                        builder.withRepeatCount(repeatCount);
                        builder.withIntervalInSeconds(intervalMinute * 60);
                    }

                    AlertIntervalJobVo alertIntervalJobVo = new AlertIntervalJobVo();
                    alertIntervalJobVo.setAlertId(alertVo.getId());
                    alertIntervalJobVo.setAlertEventHandlerId(alertEventHandlerVo.getId());
                    alertIntervalJobVo.setParentAuditId(alertEventHandlerAuditVo.getId());
                    //下一次执行的时间，用来判断次作业是否已经执行过
                    alertIntervalJobVo.setStartTime(calendar.getTime());
                    //这里需要记录还需要执行的次数，每次执行完都会-1，变成0后下次load job就不再加载了
                    alertIntervalJobVo.setRepeatCount(repeatCount + 1);
                    alertIntervalJobVo.setIntervalMinute(intervalMinute);
                    alertIntervalJobVo.setConfig(config);
                    alertMapper.insertAlertIntervalJob(alertIntervalJobVo);

                    schedulerManager.loadJob(builder.build());

                    JSONObject resultObj = new JSONObject();
                    resultObj.put("nextStartTime", alertIntervalJobVo.getStartTime());
                    resultObj.put("leftExecuteCount", alertIntervalJobVo.getRepeatCount());
                    resultObj.put("intervalMinute", alertIntervalJobVo.getIntervalMinute());
                    alertEventHandlerAuditVo.setResult(resultObj);
                }

            }

        }
        return alertVo;
    }

    @Override
    public String getName() {
        return "INTERVAL";
    }

    @Override
    public String getLabel() {
        return "定时调度";
    }

    @Override
    public String getIcon() {
        return "tsfont-formtime";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
            this.add(AlertEventType.ALERT_INPUT.getName());
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_DELETE.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
        }};
    }
}
