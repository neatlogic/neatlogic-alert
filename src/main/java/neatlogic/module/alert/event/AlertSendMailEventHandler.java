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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.enums.AlertEventStatus;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.alert.exception.alertnotifytemplate.NotifyTemplateIsUnActiveException;
import neatlogic.framework.alert.exception.alertnotifytemplate.NotifyTemplateNameIsNotFoundException;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.util.$;
import neatlogic.framework.util.EmailUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.dao.mapper.AlertNotifyTemplateMapper;
import neatlogic.module.alert.dto.AlertMailReceiverVo;
import neatlogic.module.alert.service.AlertMailReceiverService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class AlertSendMailEventHandler extends AlertEventHandlerBase {
    //private final Logger logger = LoggerFactory.getLogger(AlertSendMailEventHandler.class);
    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private AlertNotifyTemplateMapper alertNotifyTemplateMapper;

    @Resource
    private AlertMailReceiverService alertMailReceiverService;


    @Override
    public int getSort() {
        return 3;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (alertEventPluginVo != null && MapUtils.isNotEmpty(alertEventPluginVo.getConfig())) {
            //检查是否到达最大发送次数
            JSONObject pluginConfig = alertEventPluginVo.getConfig();
            int maxSendCount = pluginConfig.getIntValue("maxSendCount");
            if (maxSendCount > 0 && alertEventHandlerAuditVo.getStartTime() != null) {
                Integer c = alertEventMapper.getAlertEventExecuteCount(alertEventHandlerVo.getHandler(), alertEventHandlerAuditVo.getStartTime().getTime() - 60 * 1000);
                if (c != null && c > maxSendCount) {
                    throw new ApiRuntimeException("已经到达一分钟内最大发送次数：" + maxSendCount);
                }
            }
        }
        if (MapUtils.isNotEmpty(config)) {
            Long mailServerId = config.getLong("mailServerId");
            List<AlertAttrDefineVo> attrList = AlertAttr.getTemplateConstAttrList();
            int interval = config.getIntValue("interval");
            if (interval > 0) {
                AlertEventHandlerAuditVo paramAuditVo = new AlertEventHandlerAuditVo();
                paramAuditVo.setAlertId(alertVo.getId());
                paramAuditVo.setEventHandlerId(alertEventHandlerVo.getId());
                paramAuditVo.setStatus(AlertEventStatus.SUCCEED.getValue());
                AlertEventHandlerAuditVo auditVo = alertEventMapper.getLastAlertEventHandlerAudit(paramAuditVo);
                if (auditVo != null) {
                    Date now = new Date();
                    long diff = now.getTime() - auditVo.getStartTime().getTime();
                    if (diff < (long) interval * 60 * 1000) {
                        //如果时间不够间隔，直接返回告警
                        alertEventStatusVo.setSkipped(true);
                        //config.put("error", "离上次成功发送过去了" + (diff / 1000) + "秒，未到间隔时间，发送跳过");
                        alertEventHandlerAuditVo.setResult(config);
                        return alertVo;
                    }
                }
            }
            JSONArray toUserList = config.getJSONArray("toUserList");
            JSONArray ccUserList = config.getJSONArray("ccUserList");
            String type = config.getString("type");
            String title;
            String content;
            JSONObject paramObj = buildTemplateParamObj(alertVo);
            if (StringUtils.isBlank(type) || !type.equalsIgnoreCase("template")) {
                title = config.getString("title");
                content = config.getString("content");
            } else {
                Long templateId = config.getLong("template");
                if (templateId == null) {
                    throw new ParamNotExistsException("模板");
                }
                AlertNotifyTemplateVo templateVo = alertNotifyTemplateMapper.getNotifyTemplateById(templateId);
                if (templateVo == null) {
                    throw new NotifyTemplateNameIsNotFoundException(templateId);
                }
                if (Objects.equals(templateVo.getIsActive(), 0)) {
                    throw new NotifyTemplateIsUnActiveException(templateVo.getName());
                }
                title = templateVo.getTitle();
                content = templateVo.getContent();
            }
            title = FreemarkerUtil.transform(paramObj, title);
            content = FreemarkerUtil.transform(paramObj, content);


            AlertMailReceiverVo receiverVo = alertMailReceiverService.getReceiver(alertVo, toUserList, ccUserList);
            Set<String> to = receiverVo.getToList();
            Set<String> cc = receiverVo.getCcList();

            if (CollectionUtils.isNotEmpty(to) || CollectionUtils.isNotEmpty(cc)) {
                try {
                    EmailUtil.sendHtmlEmail(mailServerId, title, content, new ArrayList<>(to), new ArrayList<>(cc));
                } catch (Exception ex) {
                    throw new AlertEventHandlerTriggerException(ex);
                }
            }
        }
        return alertVo;
    }

    private JSONObject buildTemplateParamObj(AlertVo alertVo) {
        JSONObject paramObj = new JSONObject();
        JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
        for (AlertAttrDefineVo attr : AlertAttr.getTemplateConstAttrList()) {
            paramObj.put(attr.getName(), alertObj.get(attr.getName().replace("const_", "")));
        }
        if (MapUtils.isNotEmpty(alertVo.getAttrObj())) {
            List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
            for (AlertAttrTypeVo alertAttr : attrTypeList) {
                paramObj.put("attr_" + alertAttr.getName(), alertVo.getAttrObj().get(alertAttr.getName()));
            }
        }
        return paramObj;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public String getName() {
        return "EMAIL";
    }

    @Override
    public String getLabel() {
        return $.t("term.alert.event.emailhandlername");
    }

    @Override
    public String getIcon() {
        return "tsfont-mail-o";
    }

    @Override
    public String getDescription() {
        return $.t("term.alert.event.emailhandlerdesc");
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_DELETE.getName());
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_OPEN.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
            this.add(AlertEventType.ALERT_SUPPRESS.getName());
        }};
    }

    /*@Override
    public List<AlertEventHandlerConfigVo> getHandlerConfig(AlertEventHandlerVo alertEventHandlerVo) {
        List<AlertEventHandlerConfigVo> configList = new ArrayList<>();
        AlertEventHandlerConfigVo alertEventHandlerConfigVo = new AlertEventHandlerConfigVo();
        alertEventHandlerConfigVo.setAlertEventHandlerId(alertEventHandlerVo.getId());
        alertEventHandlerConfigVo.setUuid(alertEventHandlerVo.getUuid());
        alertEventHandlerConfigVo.setHandler(alertEventHandlerVo.getHandler());
        alertEventHandlerConfigVo.setConfig(alertEventHandlerVo.getConfig());
        configList.add(alertEventHandlerConfigVo);
        return configList;
    }*/

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
            this.add("integration");
            this.add("ai_agent");
        }};
    }
}
