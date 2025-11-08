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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertEventPluginVo;
import neatlogic.framework.alert.event.AlertEventHandlerFactory;
import neatlogic.framework.alert.event.IAlertEventHandler;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerNotFoundException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAlertEventPluginApi extends PrivateApiComponentBase {

    @Resource
    private AlertEventMapper alertEventMapper;

    @Override
    public String getToken() {
        return "alert/event/plugin/get";
    }

    @Override
    public String getName() {
        return "获取告警事件插件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "name", desc = "事件唯一标识", isRequired = true, type = ApiParamType.STRING)})
    @Output({@Param(explode = AlertEventPluginVo.class)})
    @Description(desc = "获取告警事件插件")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String name = jsonObj.getString("name");
        IAlertEventHandler handler = AlertEventHandlerFactory.getHandler(name);
        if (handler == null) {
            throw new AlertEventHandlerNotFoundException(name);
        }
        AlertEventPluginVo alertEventPluginVo = new AlertEventPluginVo();
        alertEventPluginVo.setName(handler.getName());
        alertEventPluginVo.setDescription(handler.getDescription());
        alertEventPluginVo.setLabel(handler.getLabel());
        alertEventPluginVo.setIcon(handler.getIcon());
        AlertEventPluginVo configVo = alertEventMapper.getAlertEventPluginConfigByName(name);
        if (configVo != null) {
            alertEventPluginVo.setIsActive(configVo.getIsActive());
            alertEventPluginVo.setConfigStr(configVo.getConfigStr());
        } else {
            alertEventPluginVo.setIsActive(1);
        }
        return alertEventPluginVo;
    }
}
