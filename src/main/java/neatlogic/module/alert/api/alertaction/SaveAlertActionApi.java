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

package neatlogic.module.alert.api.alertaction;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_ACTION_MODIFY;
import neatlogic.framework.alert.dto.AlertActionVo;
import neatlogic.framework.alert.exception.alertaction.AlertActionNameIsExistsException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertActionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_ACTION_MODIFY.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveAlertActionApi extends PrivateApiComponentBase {

    @Resource
    private AlertActionMapper alertActionMapper;

    @Override
    public String getToken() {
        return "alert/action/save";
    }

    @Override
    public String getName() {
        return "保存告警动作";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", maxLength = 50, desc = "唯一标识", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "label", maxLength = 50, desc = "名称", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "icon", desc = "图标", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "script", desc = "脚本", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "description", desc = "说明", type = ApiParamType.STRING, maxLength = 500)
    })
    @Output({@Param(name = "id", desc = "动作id", type = ApiParamType.LONG)})
    @Description(desc = "保存告警动作")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        AlertActionVo alertActionVo = JSON.parseObject(jsonObj.toJSONString(), AlertActionVo.class);
        if (alertActionMapper.checkAlertActionNameIsExists(alertActionVo) > 0) {
            throw new AlertActionNameIsExistsException(alertActionVo.getName());
        }
        if (id == null) {
            alertActionMapper.insertAlertAction(alertActionVo);
        } else {
            alertActionMapper.updateAlertAction(alertActionVo);
        }
        return alertActionVo.getId();
    }


}
