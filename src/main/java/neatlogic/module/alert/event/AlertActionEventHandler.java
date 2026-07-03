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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.util.$;
import neatlogic.module.alert.dao.mapper.AlertActionMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

@Component
public class AlertActionEventHandler extends AlertEventHandlerBase {
    @Resource
    private AlertActionMapper alertActionMapper;


    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public int getSort() {
        return 15;
    }

    @Override
    public String getName() {
        return "ACTION";
    }

    @Override
    public String getLabel() {
        return $.t("term.alert.event.actionhandlername");
    }

    @Override
    public String getIcon() {
        return "tsfont-lightning";
    }

    @Override
    public String getDescription() {
        return $.t("term.alert.event.actionhandlerdesc");
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<>() {{
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
            this.add(AlertEventType.ALERT_OPEN.getName());
            this.add(AlertEventType.ALERT_SUPPRESS.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<>() {{
            this.add("condition");
            this.add("interval");
            this.add("integration");
        }};
    }


    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) throws AlertEventHandlerTriggerException {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (config == null) {
            config = new JSONObject();
        }

        JSONArray actionList = config.getJSONArray("actionList");
        if (CollectionUtils.isNotEmpty(actionList)) {
            for (int i = 0; i < actionList.size(); i++) {
                String action = actionList.getString(i);
                AlertActionVo alertActionVo = alertActionMapper.getAlertActionByName(action);
                if (alertActionVo != null) {
                    alertActionMapper.insertAlertActionRel(alertVo.getId(), action);
                }
            }
        }
        return alertVo;
    }
}
