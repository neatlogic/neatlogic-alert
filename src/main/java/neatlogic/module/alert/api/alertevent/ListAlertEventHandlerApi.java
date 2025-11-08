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

package neatlogic.module.alert.api.alertevent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.service.IAlertService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertEventHandlerApi extends PrivateApiComponentBase {


    @Resource
    private IAlertService alertService;

    @Override
    public String getToken() {
        return "alert/event/handler/list";
    }

    @Override
    public String getName() {
        return "列出所有告警事件配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "alertType", desc = "告警类型", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "event", desc = "事件唯一标识", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "parentId", desc = "父处理器id", type = ApiParamType.LONG)
    })
    @Output({@Param(explode = AlertEventHandlerVo[].class)})
    @Description(desc = "列出所有告警事件配置")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AlertEventHandlerVo alertEventHandlerVo = JSON.toJavaObject(jsonObj, AlertEventHandlerVo.class);
        return alertService.listAlertEventHandler(alertEventHandlerVo);
    }


}
