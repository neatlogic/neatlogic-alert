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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.util.EmailUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.schedule.handler.AlertEventSendMailScheduleJob;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AlertSendMailEventHandler extends AlertEventHandlerBase {
    private final Logger logger = LoggerFactory.getLogger(AlertSendMailEventHandler.class);
    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private AlertEventMapper alertEventMapper;

    @Resource
    protected SchedulerManager schedulerManager;

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertVo alertVo) {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (MapUtils.isNotEmpty(config)) {
            List<AlertAttrDefineVo> attrList = AlertAttr.getConstAttrList();
            JSONArray toUserList = config.getJSONArray("toUserList");
            JSONArray ccUserList = config.getJSONArray("ccUserList");
            String title = config.getString("title");
            String content = config.getString("content");
            Integer interval = config.getInteger("interval");
            JSONArray statusList = config.getJSONArray("statusList");
            JSONObject paramObj = new JSONObject();
            JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
            for (AlertAttrDefineVo attr : attrList) {
                paramObj.put(attr.getName(), alertObj.get(attr.getName().replace("const_", "")));
            }
            if (MapUtils.isNotEmpty(alertVo.getAttrObj())) {
                List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
                for (AlertAttrTypeVo alertAttr : attrTypeList) {
                    paramObj.put("attr_" + alertAttr.getName(), alertVo.getAttrObj().get(alertAttr.getName()));
                }
            }
            content = FreemarkerUtil.transform(paramObj, content);
            List<String> to = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(toUserList)) {
                for (int i = 0; i < toUserList.size(); i++) {
                    to.add(toUserList.getString(i));
                }
            }
            List<String> cc = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(ccUserList)) {
                for (int i = 0; i < ccUserList.size(); i++) {
                    cc.add(ccUserList.getString(i));
                }
            }


            if (CollectionUtils.isNotEmpty(to) || CollectionUtils.isNotEmpty(cc)) {
                try {
                    EmailUtil.sendHtmlEmail(title, content, to, cc);
                    //System.out.println("#################发送邮件成功Event：" + alertVo.getId());
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
                if (interval != null && CollectionUtils.isNotEmpty(statusList)) {
                    AlertEventHandlerDataVo dataVo = new AlertEventHandlerDataVo();
                    dataVo.setHandler(alertEventHandlerVo.getHandler());
                    dataVo.setAlertId(alertVo.getId());
                    dataVo.setAlertEventHandlerId(alertEventHandlerVo.getId());
                    dataVo.setData(config);
                    alertEventMapper.insertAlertEventHandlerData(dataVo);

                    //加载定时作业
                    IJob jobHandler = SchedulerManager.getHandler(AlertEventSendMailScheduleJob.class.getName());
                    schedulerManager.loadJob(new JobObject.Builder(alertVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName())
                            .addData("alertId", alertVo.getId())
                            .addData("alertEventHandlerId", alertEventHandlerVo.getId())
                            .withIntervalInSeconds(interval * 60)
                            .build());
                    //System.out.println("##############创建作业Event：" + alertVo.getId());
                }
            }

            if (interval == null || CollectionUtils.isEmpty(statusList)) {
                alertEventMapper.deleteAlertEventHandlerData(alertVo.getId(), alertEventHandlerVo.getId());
                IJob jobHandler = SchedulerManager.getHandler(AlertEventSendMailScheduleJob.class.getName());
                if (jobHandler != null) {
                    String tenantUuid = TenantContext.get().getTenantUuid();
                    JobObject jobObject = new JobObject.Builder(alertVo.getId().toString(), tenantUuid + "-ALERT-EVENT-MAIL", jobHandler.getClassName(), tenantUuid).build();
                    schedulerManager.unloadJob(jobObject);
                    //System.out.println("##############删除作业Event：" + alertVo.getId());
                }
            }

        }
        return alertVo;
    }

    @Override
    public String getName() {
        return "EMAIL";
    }

    @Override
    public String getLabel() {
        return "发送邮件";
    }

    @Override
    public String getIcon() {
        return "tsfont-mail-o";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_DELETE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
        }};
    }

    @Override
    public List<AlertEventHandlerConfigVo> getHandlerConfig(AlertEventHandlerVo alertEventHandlerVo) {
        List<AlertEventHandlerConfigVo> configList = new ArrayList<>();
        AlertEventHandlerConfigVo alertEventHandlerConfigVo = new AlertEventHandlerConfigVo();
        alertEventHandlerConfigVo.setAlertEventHandlerId(alertEventHandlerVo.getId());
        alertEventHandlerConfigVo.setUuid(alertEventHandlerVo.getUuid());
        alertEventHandlerConfigVo.setHandler(alertEventHandlerVo.getHandler());
        alertEventHandlerConfigVo.setConfig(alertEventHandlerVo.getConfig());
        configList.add(alertEventHandlerConfigVo);
        return configList;
    }

}
