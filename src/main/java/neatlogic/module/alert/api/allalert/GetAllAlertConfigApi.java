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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertAllAlertConfigVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAllAlertConfigMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAllAlertConfigApi extends PrivateApiComponentBase {

    @Resource
    private AlertAllAlertConfigMapper alertAllAlertConfigMapper;

    @Override
    public String getToken() {
        return "/alert/allalert/config/get";
    }

    @Override
    public String getName() {
        return "获取所有告警配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", desc = "配置名称", type = ApiParamType.STRING, isRequired = true)
    })
    @Output({@Param(explode = AlertAllAlertConfigVo.class)})
    @Description(desc = "获取所有告警配置")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        return alertAllAlertConfigMapper.getAlertAllAlertConfigByName(jsonObj.getString("name"));
    }
}
