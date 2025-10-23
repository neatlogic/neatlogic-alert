/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        return "删除告警事件插件类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG, isRequired = true)
    })
    @Description(desc = "删除告警事件插件类型")
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
