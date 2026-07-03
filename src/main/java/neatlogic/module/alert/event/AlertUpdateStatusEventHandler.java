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
import neatlogic.framework.alert.event.AlertEventManager;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.store.elasticsearch.ElasticsearchDocumentFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchDocument;
import neatlogic.framework.util.$;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class AlertUpdateStatusEventHandler extends AlertEventHandlerBase {
    @Resource
    private AlertAuditMapper alertAuditMapper;

    @Resource
    private AlertMapper alertMapper;

    @Override
    public int getSort() {
        return 9;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) throws AlertEventHandlerTriggerException {
        //先判断告警是否存在，不存在直接返回
        if (alertMapper.checkAlertIsExists(alertVo.getId()) == 0) {
            return alertVo;
        }
        JSONObject config = alertEventHandlerVo.getConfig();
        if (config == null) {
            config = new JSONObject();
        }
        String status = config.getString("status");
        if (StringUtils.isNotBlank(status)) {
            if (!Objects.equals(alertVo.getStatus(), status)) {
                String oldStatus = alertVo.getStatus();
                alertVo.setStatus(status);
                alertMapper.updateAlertStatus(alertVo);

                IElasticsearchDocument<AlertVo> index = ElasticsearchDocumentFactory.getIndex("ALERT");
                index.updateDocument(alertVo.getId(), new JSONObject() {{
                    this.put("status", alertVo.getStatus());
                }}, false);

                AlertAuditVo alertAuditVo = new AlertAuditVo(true);
                alertAuditVo.setAlertId(alertVo.getId());
                alertAuditVo.setAttrName("const_status");
                alertAuditVo.addOldValue(oldStatus);
                alertAuditVo.addNewValue(alertVo.getStatus());
                alertAuditMapper.insertAlertAudit(alertAuditVo);

                AlertEventManager.doEvent(AlertEventType.ALERT_STATUE_CHANGE, alertVo);

                JSONObject resultObj = new JSONObject();
                resultObj.put("oldStatus", oldStatus);
                resultObj.put("newStatus", alertVo.getStatus());
                alertEventHandlerAuditVo.setResult(resultObj);
            }
        }
        return alertVo;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public String getName() {
        return "UPDATESTATUS";
    }

    @Override
    public String getLabel() {
        return $.t("term.alert.event.updatehandlername");
    }

    @Override
    public String getIcon() {
        return "tsfont-heart-s";
    }

    @Override
    public String getDescription() {
        return $.t("term.alert.event.updatehandlerdesc");
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_OPEN.getName());
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
            this.add(AlertEventType.ALERT_SUPPRESS.getName());
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
