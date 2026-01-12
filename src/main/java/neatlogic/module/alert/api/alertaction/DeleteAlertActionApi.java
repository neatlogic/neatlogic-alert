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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_ACTION_MODIFY;
import neatlogic.framework.alert.dto.AlertActionVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertActionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_ACTION_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAlertActionApi extends PrivateApiComponentBase {

    @Resource
    private AlertActionMapper alertActionMapper;


    @Override
    public String getToken() {
        return "/alert/action/delete";
    }

    @Override
    public String getName() {
        return "删除自定义动作";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", isRequired = true, desc = "id", type = ApiParamType.LONG),
    })
    @Output({@Param(explode = AlertActionVo.class)})
    @Description(desc = "删除自定义动作")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        Long id = jsonObj.getLong("id");
        return alertActionMapper.getAlertActionById(id);
    }
}
