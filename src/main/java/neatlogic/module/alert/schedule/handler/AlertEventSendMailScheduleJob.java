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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertAttrDefineVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.dto.AlertEventHandlerDataVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.util.EmailUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 合规检查定时器
 */
@Component
@DisallowConcurrentExecution
public class AlertEventSendMailScheduleJob extends JobBase {
    static Logger logger = LoggerFactory.getLogger(AlertEventSendMailScheduleJob.class);

    @Resource
    private AlertEventMapper alertEventMapper;

    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;


    @Override
    public Boolean isMyHealthy(JobObject jobObject) {
        Long alertId = (Long) jobObject.getData("alertId");
        Long alertEventHandlerId = (Long) jobObject.getData("alertEventHandlerId");
        AlertEventHandlerDataVo alertEventHandlerDataVo = alertEventMapper.getAlertEventHandlerData(alertId, alertEventHandlerId);
        return alertEventHandlerDataVo != null &&
                MapUtils.isNotEmpty(alertEventHandlerDataVo.getData()) &&
                alertEventHandlerDataVo.getData().getInteger("interval") != null &&
                CollectionUtils.isNotEmpty(alertEventHandlerDataVo.getData().getJSONArray("statusList"));

    }

    @Override
    public void reloadJob(JobObject jobObject) {
        Long alertId = (Long) jobObject.getData("alertId");
        Long alertEventHandlerId = (Long) jobObject.getData("alertEventHandlerId");
        AlertEventHandlerDataVo alertEventHandlerDataVo = alertEventMapper.getAlertEventHandlerData(alertId, alertEventHandlerId);
        if (alertEventHandlerDataVo != null &&
                alertEventHandlerDataVo.getData() != null &&
                alertEventHandlerDataVo.getData().getInteger("interval") != null) {
            String tenantUuid = TenantContext.get().getTenantUuid();
            Integer interval = alertEventHandlerDataVo.getData().getInteger("interval");
            JobObject newJobObject = new JobObject.Builder(alertId.toString(), this.getGroupName(), this.getClassName(), tenantUuid)
                    .withIntervalInSeconds(interval * 60).build();
            schedulerManager.loadJob(newJobObject);
        } else {
            schedulerManager.unloadJob(jobObject);
        }
    }

    @Override
    public void initJob(String tenantUuid) {
        List<AlertEventHandlerDataVo> alertEventHandlerDataList = alertEventMapper.listAlertEventHandlerData();
        if (CollectionUtils.isNotEmpty(alertEventHandlerDataList)) {
            for (AlertEventHandlerDataVo alertEventHandlerDataVo : alertEventHandlerDataList) {
                JSONObject config = alertEventHandlerDataVo.getData();
                Integer interval = config.getInteger("interval");
                JSONArray statusList = config.getJSONArray("statusList");
                if (interval != null && CollectionUtils.isNotEmpty(statusList)) {
                    AlertVo alertVo = alertMapper.getAlertById(alertEventHandlerDataVo.getAlertId());
                    if (alertVo != null && statusList.contains(alertVo.getStatus())) {
                        JobObject newJobObject = new JobObject.Builder(alertEventHandlerDataVo.getAlertId().toString(), this.getGroupName(), this.getClassName(), tenantUuid)
                                .withIntervalInSeconds(interval * 60)
                                .addData("alertId", alertEventHandlerDataVo.getAlertId())
                                .addData("alertEventHandlerId", alertEventHandlerDataVo.getAlertEventHandlerId()).build();
                        schedulerManager.loadJob(newJobObject);
                    }
                }
            }
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) {
        Long alertId = (Long) jobObject.getData("alertId");
        Long alertEventHandlerId = (Long) jobObject.getData("alertEventHandlerId");
        AlertEventHandlerDataVo alertEventHandlerDataVo = alertEventMapper.getAlertEventHandlerData(alertId, alertEventHandlerId);
        AlertVo alertVo = alertMapper.getAlertById(alertId);
        JSONObject config = null;
        boolean needRun = true;

        if (alertVo == null || alertEventHandlerDataVo == null) {
            needRun = false;
        }

        if (needRun) {
            config = alertEventHandlerDataVo.getData();
            if (MapUtils.isEmpty(config) ||
                    config.getInteger("interval") == null ||
                    CollectionUtils.isEmpty(config.getJSONArray("statusList"))) {
                needRun = false;
            }
        }


        if (needRun) {
            JSONArray statusList = alertEventHandlerDataVo.getData().getJSONArray("statusList");
            if (!statusList.contains(alertVo.getStatus())) {
                needRun = false;
            }
        }

        if (needRun) {
            //AlertEventHandlerVo alertEventHandlerVo = alertEventMapper.getAlertEventHandlerById(alertEventHandlerId);
            //JSONObject config = alertEventHandlerVo.getConfig();
            List<AlertAttrDefineVo> attrList = AlertAttr.getConstAttrList();
            JSONArray toUserList = config.getJSONArray("toUserList");
            JSONArray ccUserList = config.getJSONArray("ccUserList");
            String title = config.getString("title");
            String content = config.getString("content");
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

            try {
                if (CollectionUtils.isNotEmpty(to) || CollectionUtils.isNotEmpty(cc)) {
                    EmailUtil.sendHtmlEmail(title, content, to, cc);
                } else {
                    needRun = false;
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        if (!needRun) {
            System.out.println("##############删除作业2");
            schedulerManager.unloadJob(jobObject);
        }
    }


    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-ALERT-EVENT-MAIL";
    }

}
