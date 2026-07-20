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

package neatlogic.module.alert.api.alertaudit;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.service.AlertEventAuditService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAlertEventAuditTreeApi extends PrivateApiComponentBase {

    @Resource
    private AlertEventAuditService alertEventAuditService;

    @Override
    public String getToken() {
        return "/alert/event/audit/tree/get";
    }

    @Override
    public String getName() {
        return "获取告警事件执行审计树";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "auditId", desc = "审计记录ID", type = ApiParamType.LONG, isRequired = true)})
    @Output({@Param(explode = AlertEventHandlerAuditVo.class)})
    @Description(desc = "获取告警事件执行审计树")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        return alertEventAuditService.getAuditTree(jsonObj.getLong("auditId"));
    }
}
