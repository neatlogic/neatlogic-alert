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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_EVENT_PLUGIN_MODIFY;
import neatlogic.framework.alert.dto.AlertEventHandlerTypeVo;
import neatlogic.framework.alert.exception.alerteventhandlertype.AlertEventHandlerTypeNameIsExistsException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertEventHandlerTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_EVENT_PLUGIN_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertEventHandlerTypeApi extends PrivateApiComponentBase {

    @Resource
    private AlertEventHandlerTypeMapper alertEventHandlerTypeMapper;


    @Override
    public String getToken() {
        return "/alert/event/handler/type/save";
    }

    @Override
    public String getName() {
        return "保存告警事件插件类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "唯一标识", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "label", desc = "名称", type = ApiParamType.STRING, isRequired = true)
    })
    @Output({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
    })
    @Description(desc = "保存告警事件插件类型")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        Long id = jsonObj.getLong("id");
        AlertEventHandlerTypeVo alertEventHandlerTypeVo = JSON.toJavaObject(jsonObj, AlertEventHandlerTypeVo.class);
        if (alertEventHandlerTypeMapper.checkAlertEventHandlerTypeNameIsExists(alertEventHandlerTypeVo) > 0) {
            throw new AlertEventHandlerTypeNameIsExistsException(alertEventHandlerTypeVo.getName());
        }
        if (id == null) {
            alertEventHandlerTypeMapper.insertAlertEventHandlerType(alertEventHandlerTypeVo);
        } else {
            alertEventHandlerTypeMapper.updateAlertEventHandlerType(alertEventHandlerTypeVo);
        }
        return alertEventHandlerTypeVo.getId();
    }
}
