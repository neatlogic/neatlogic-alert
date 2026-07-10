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
import neatlogic.framework.util.$;
import neatlogic.module.alert.service.IAlertService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

@Component
public class AlertDeleteEventHandler extends AlertEventHandlerBase {
    @Resource
    private IAlertService alertService;

    @Override
    public int getSort() {
        return 10;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) {
        JSONObject config = alertEventHandlerVo.getConfig();
        alertService.deleteAlert(alertVo.getId(), config.getIntValue("isDeleteChildAlert") == 1);
        return alertVo;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public String getName() {
        return "DELETE";
    }

    @Override
    public String getLabel() {
        return $.t("term.alert.event.deletehandlername");
    }

    @Override
    public String getIcon() {
        return "tsfont-trash-o";
    }

    @Override
    public String getDescription() {
        return $.t("term.alert.event.deletehandlerdesc");
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
            this.add(AlertEventType.ALERT_SUPPRESS.getName());
        }};
    }

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
