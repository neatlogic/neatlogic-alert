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
import neatlogic.framework.alert.auth.ALERT_EVENT_PLUGIN_MODIFY;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertEventPluginVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_EVENT_PLUGIN_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAlertEventPluginConfigApi extends PrivateApiComponentBase {

    @Resource
    private AlertEventMapper alertEventMapper;

    @Override
    public String getToken() {
        return "alert/event/plugin/config/save";
    }

    @Override
    public String getName() {
        return "保存告警事件插件配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "name", desc = "事件唯一标识", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "isActive", desc = "是否激活", type = ApiParamType.INTEGER),
            @Param(name = "config", desc = "配置", type = ApiParamType.JSONOBJECT)})
    @Description(desc = "保存告警事件插件配置")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AlertEventPluginVo alertEventPluginVo = JSON.toJavaObject(jsonObj, AlertEventPluginVo.class);
        alertEventMapper.saveAlertEventPluginConfig(alertEventPluginVo);
        return null;
    }
}
