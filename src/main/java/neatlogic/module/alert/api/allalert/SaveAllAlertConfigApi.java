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

package neatlogic.module.alert.api.allalert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_ALLALERTCONFIG_MODIFY;
import neatlogic.framework.alert.dto.AlertAllAlertConfigVo;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAllAlertConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_ALLALERTCONFIG_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAllAlertConfigApi extends PrivateApiComponentBase {

    @Resource
    private AlertAllAlertConfigMapper alertAllAlertConfigMapper;

    @Override
    public String getToken() {
        return "/alert/allalert/config/save";
    }

    @Override
    public String getName() {
        return "保存所有告警配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", desc = "配置名称", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "config", desc = "配置", type = ApiParamType.JSONOBJECT, isRequired = true)
    })
    @Description(desc = "保存所有告警配置")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertAllAlertConfigVo configVo = JSON.toJavaObject(jsonObj, AlertAllAlertConfigVo.class);
        AlertAllAlertConfigVo oldConfigVo = alertAllAlertConfigMapper.getAlertAllAlertConfigByName(configVo.getName());
        if (oldConfigVo == null) {
            configVo.setFcu(UserContext.get().getUserUuid(true));
        } else {
            configVo.setLcu(UserContext.get().getUserUuid(true));
        }
        alertAllAlertConfigMapper.saveAlertAllAlertConfig(configVo);
        return null;
    }
}
