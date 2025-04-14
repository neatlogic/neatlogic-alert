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
