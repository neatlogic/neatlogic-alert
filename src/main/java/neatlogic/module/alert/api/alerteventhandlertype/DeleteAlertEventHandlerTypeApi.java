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

package neatlogic.module.alert.api.alerteventhandlertype;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_EVENT_PLUGIN_MODIFY;
import neatlogic.framework.alert.exception.alerteventhandlertype.AlertEventHandlerTypeNameIsInUsedException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertEventHandlerTypeMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_EVENT_PLUGIN_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAlertEventHandlerTypeApi extends PrivateApiComponentBase {

    @Resource
    private AlertEventHandlerTypeMapper alertEventHandlerTypeMapper;


    @Override
    public String getToken() {
        return "/alert/event/handler/type/delete";
    }

    @Override
    public String getName() {
        return "term.alert.api.deletealerteventhandlertype";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "common.id", type = ApiParamType.LONG, isRequired = true)
    })
    @Description(desc = "term.alert.api.deletealerteventhandlertype")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        Long id = jsonObj.getLong("id");
        if (alertEventHandlerTypeMapper.checkAlertEventHandlerTypeIsInUsed(id) > 0) {
            throw new AlertEventHandlerTypeNameIsInUsedException();
        }
        alertEventHandlerTypeMapper.deleteAlertEventHandlerType(id);
        return null;
    }
}
